package com.starmitra.modules.room.application;

import com.starmitra.modules.media.application.MediaReferenceContract;
import com.starmitra.modules.room.persistence.*;
import com.starmitra.modules.skill.application.SkillTaxonomyContract;
import com.starmitra.platform.audit.AuditService;
import com.starmitra.platform.error.ApiException;
import com.starmitra.platform.error.ErrorCode;
import com.starmitra.platform.pagination.Cursor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * M07 — Creative Rooms. Authoritative project-collaboration truth:
 * rooms, members, required skills, invitations, contextual contribution
 * roles, tasks, assets, final outputs, project credits.
 *
 * Contribution roles are freeform contextual names ("Lead Actor") — never
 * TalentSkill, never permission. Project credits are minted at finalization
 * from ACTIVE contribution roles (verified=true) — the only credit source
 * M08 may link via ProjectCreditContract.
 */
@Service
public class RoomService implements ProjectCreditContract, ProjectMembershipContract,
        ProjectContributionContract {

    private final CreativeRoomRepository rooms;
    private final ProjectMemberRepository members;
    private final ProjectContributionRoleRepository roles;
    private final RequiredSkillRepository requiredSkills;
    private final ProjectInvitationRepository invitations;
    private final ProjectTaskRepository tasks;
    private final ProjectAssetRepository assets;
    private final FinalOutputRepository finalOutputs;
    private final ProjectCreditRepository credits;
    private final SkillTaxonomyContract skills;
    private final MediaReferenceContract media;
    private final AuditService audit;

    public RoomService(CreativeRoomRepository rooms, ProjectMemberRepository members,
                       ProjectContributionRoleRepository roles, RequiredSkillRepository requiredSkills,
                       ProjectInvitationRepository invitations, ProjectTaskRepository tasks,
                       ProjectAssetRepository assets, FinalOutputRepository finalOutputs,
                       ProjectCreditRepository credits, SkillTaxonomyContract skills,
                       MediaReferenceContract media, AuditService audit) {
        this.rooms = rooms;
        this.members = members;
        this.roles = roles;
        this.requiredSkills = requiredSkills;
        this.invitations = invitations;
        this.tasks = tasks;
        this.assets = assets;
        this.finalOutputs = finalOutputs;
        this.credits = credits;
        this.skills = skills;
        this.media = media;
        this.audit = audit;
    }

    public record RoomView(UUID id, String name, String status, String visibility,
                           UUID ownerId, int version) {}
    public record MemberView(UUID userId, String status, String joinedAt, List<String> contributionRoles) {}
    public record InvitationView(UUID id, UUID roomId, String status, String expiresAt) {}
    public record ContributionView(UUID id, UUID userId, String roleName, boolean verified) {}
    public record TaskView(UUID id, String title, String status, UUID assigneeMemberId) {}
    public record Page(String nextCursor, boolean hasMore, Integer total) {}
    public record RoomPage(List<RoomView> items, Page page) {}
    public record RoomCommand(String name, String description, String visibility) {}

    // ---------- rooms ----------

    @Transactional(readOnly = true)
    public RoomPage listRooms(UUID caller, UUID skillId, String cursor, Integer limit) {
        int size = Cursor.limit(limit);
        int offset = offset(cursor);
        var rows = rooms.findVisible(caller, skillId, PageRequest.of(0, size + 1 + offset));
        var window = rows.size() > offset ? rows.subList(offset, rows.size()) : List.<CreativeRoomEntity>of();
        boolean hasMore = window.size() > size;
        var items = window.stream().limit(size).map(this::toRoom).toList();
        return new RoomPage(items,
                new Page(hasMore ? Cursor.encode("o", String.valueOf(offset + size)) : null, hasMore, null));
    }

    @Transactional
    public RoomView createRoom(UUID owner, RoomCommand cmd) {
        var room = rooms.save(new CreativeRoomEntity(owner, cmd.name(), cmd.description(),
                cmd.visibility() == null ? CreativeRoomEntity.Visibility.PRIVATE
                        : CreativeRoomEntity.Visibility.valueOf(cmd.visibility())));
        members.save(new ProjectMemberEntity(room.getId(), owner));     // owner is a member
        audit.record("M07", "ROOM_CREATED", owner, "user", "creative_room", room.getId().toString(), null);
        return toRoom(room);
    }

    @Transactional(readOnly = true)
    public RoomView getRoom(UUID caller, UUID roomId) {
        return toRoom(requireVisible(caller, roomId));
    }

    @Transactional
    public RoomView updateRoom(UUID caller, UUID roomId, RoomCommand cmd, String ifMatch) {
        var room = requireOwner(caller, roomId);
        if (ifMatch != null && !String.valueOf(room.getVersion()).equals(stripQuotes(ifMatch))) {
            throw new ApiException(ErrorCode.CONFLICT_VERSION, "Stale version");
        }
        room.update(cmd.name(), cmd.description(),
                cmd.visibility() == null ? null : CreativeRoomEntity.Visibility.valueOf(cmd.visibility()));
        return toRoom(rooms.saveAndFlush(room));
    }

    // ---------- members ----------

    @Transactional(readOnly = true)
    public List<MemberView> listMembers(UUID caller, UUID roomId) {
        var room = requireVisible(caller, roomId);
        var roleNames = roles.findByRoomId(roomId).stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        ProjectContributionRoleEntity::getMemberUserId,
                        java.util.stream.Collectors.mapping(ProjectContributionRoleEntity::getRoleName,
                                java.util.stream.Collectors.toList())));
        return members.findByRoomId(roomId).stream()
                .map(m -> new MemberView(m.getUserId(), m.getStatus().name(),
                        m.getJoinedAt().toString(), roleNames.getOrDefault(m.getUserId(), List.of())))
                .toList();
    }

    // ---------- required skills (owner) ----------

    @Transactional
    public void addRequiredSkill(UUID caller, UUID roomId, UUID skillId) {
        requireOwner(caller, roomId);
        if (!skills.isActiveSkill(skillId)) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED, "Skill not found or inactive");
        }
        var key = new RequiredSkillEntity.Pk(roomId, skillId);
        if (!requiredSkills.existsById(key)) {
            try {
                requiredSkills.saveAndFlush(new RequiredSkillEntity(roomId, skillId));
            } catch (DataIntegrityViolationException dup) { /* concurrent — already required */ }
        }
    }

    @Transactional
    public void removeRequiredSkill(UUID caller, UUID roomId, UUID skillId) {
        requireOwner(caller, roomId);
        var key = new RequiredSkillEntity.Pk(roomId, skillId);
        if (requiredSkills.existsById(key)) requiredSkills.deleteById(key);
    }

    // ---------- invitations ----------

    /** Owner invites; one PENDING per (room,invitee) via uq_pi_pending. */
    @Transactional
    public InvitationView invite(UUID caller, UUID roomId, UUID inviteeId) {
        var room = requireOwner(caller, roomId);
        if (isActiveMember(roomId, inviteeId)) {
            throw new ApiException(ErrorCode.CONFLICT, "User is already a member");
        }
        if (invitations.existsByRoomIdAndInviteeIdAndStatus(roomId, inviteeId,
                ProjectInvitationEntity.Status.PENDING)) {
            throw new ApiException(ErrorCode.CONFLICT, "Invitation already pending");
        }
        var inv = invitations.save(new ProjectInvitationEntity(roomId, caller, inviteeId, null));
        audit.record("M07", "ROOM_INVITE_SENT", caller, "user", "creative_room", roomId.toString(), null);
        return toInv(inv);
    }

    @Transactional(readOnly = true)
    public List<InvitationView> myInvitations(UUID invitee) {
        return invitations.findByInviteeIdAndStatus(invitee, ProjectInvitationEntity.Status.PENDING)
                .stream().map(this::toInv).toList();
    }

    /** Invitee responds — PENDING only; ACCEPT activates membership. */
    @Transactional
    public InvitationView respond(UUID invitee, UUID invitationId, String action) {
        var inv = invitations.findById(invitationId)
                .filter(i -> i.getInviteeId().equals(invitee))
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND));
        if (!inv.isPending()) {
            throw new ApiException(ErrorCode.STATE_TRANSITION_INVALID, "Invitation already resolved");
        }
        if ("ACCEPT".equals(action)) {
            inv.accept();
            var key = new ProjectMemberEntity.Pk(inv.getRoomId(), invitee);
            members.findById(key).ifPresentOrElse(
                    m -> { m.rejoin(); members.save(m); },
                    () -> members.save(new ProjectMemberEntity(inv.getRoomId(), invitee)));
            audit.record("M07", "ROOM_INVITE_ACCEPTED", invitee, "user",
                    "creative_room", inv.getRoomId().toString(), null);
        } else if ("DECLINE".equals(action)) {
            inv.decline();
        } else {
            throw new ApiException(ErrorCode.VALIDATION_FAILED, "action must be ACCEPT|DECLINE");
        }
        return toInv(invitations.save(inv));
    }

    // ---------- contribution roles (owner assigns; contextual, never permission) ----------

    @Transactional
    public ContributionView assignRole(UUID caller, UUID roomId, UUID memberUserId, String roleName) {
        requireOwner(caller, roomId);
        if (roleName == null || roleName.isBlank() || roleName.length() > 80) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED, "roleName invalid");
        }
        if (!isActiveMember(roomId, memberUserId)) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED, "Not an active member");
        }
        try {
            var role = roles.saveAndFlush(new ProjectContributionRoleEntity(roomId, memberUserId, roleName.trim()));
            audit.record("M07", "CONTRIBUTION_ROLE_ASSIGNED", caller, "user",
                    "creative_room", roomId.toString(), null);
            return new ContributionView(role.getId(), memberUserId, role.getRoleName(), false);
        } catch (DataIntegrityViolationException dup) {
            throw new ApiException(ErrorCode.CONFLICT, "Role already assigned");
        }
    }

    // ---------- tasks (members) ----------

    @Transactional(readOnly = true)
    public List<TaskView> listTasks(UUID caller, UUID roomId) {
        requireMember(caller, roomId);
        return tasks.findByRoomId(roomId).stream()
                .map(t -> new TaskView(t.getId(), t.getTitle(), t.getStatus(), t.getAssigneeMemberId())).toList();
    }

    @Transactional
    public TaskView createTask(UUID caller, UUID roomId, String title, String description,
                               UUID assigneeMemberId, LocalDate dueDate) {
        requireMember(caller, roomId);
        if (assigneeMemberId != null && !isActiveMember(roomId, assigneeMemberId)) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED, "Assignee not an active member");
        }
        var t = tasks.save(new ProjectTaskEntity(roomId, title, description, assigneeMemberId, dueDate));
        return new TaskView(t.getId(), t.getTitle(), t.getStatus(), t.getAssigneeMemberId());
    }

    // ---------- assets + final outputs (owner) ----------

    @Transactional
    public void linkAsset(UUID caller, UUID roomId, UUID mediaId, String role) {
        var room = requireOwner(caller, roomId);
        if (!media.isDeliverableTo(mediaId, caller)) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED, "Media not usable");
        }
        try {
            assets.saveAndFlush(new ProjectAssetEntity(roomId, mediaId, role));
        } catch (DataIntegrityViolationException dup) {
            throw new ApiException(ErrorCode.CONFLICT, "Media already linked to this room");
        }
    }

    /**
     * Finalization = authoritative credit minting: every ACTIVE contribution
     * role in the room produces a VERIFIED project_credit (once per role).
     */
    @Transactional
    public void addFinalOutput(UUID caller, UUID roomId, UUID mediaId, String outputType) {
        var room = requireOwner(caller, roomId);
        if (!media.isUsableBy(mediaId, caller)) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED, "Media not usable");
        }
        finalOutputs.save(new FinalOutputEntity(roomId, mediaId, outputType));
        mintCredits(roomId);
        audit.record("M07", "FINAL_OUTPUT_ADDED", caller, "user", "creative_room", roomId.toString(), null);
    }

    private void mintCredits(UUID roomId) {
        for (var role : roles.findByRoomId(roomId)) {
            if (role.isActive() && !credits.existsByContributionRoleId(role.getId())) {
                var credit = new ProjectCreditEntity(roomId, role.getMemberUserId(),
                        role.getId(), role.getRoleName());
                credit.verify();
                credits.save(credit);
            }
        }
    }

    @Transactional(readOnly = true)
    public List<ContributionView> listCredits(UUID caller, UUID roomId) {
        requireVisible(caller, roomId);
        return credits.findByRoomIdAndVerifiedTrue(roomId).stream()
                .map(c -> new ContributionView(c.getId(), c.getMemberUserId(), c.getCreditLabel(), true))
                .toList();
    }

    // ---------- cross-module contracts ----------

    @Override
    @Transactional(readOnly = true)
    public boolean isLinkableCredit(UUID projectCreditId, UUID userId) {
        return credits.findById(projectCreditId)
                .map(c -> c.isVerified() && c.getMemberUserId().equals(userId)).orElse(false);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isActiveMember(UUID roomId, UUID userId) {
        return members.findById(new ProjectMemberEntity.Pk(roomId, userId))
                .map(m -> m.getStatus() == ProjectMemberEntity.Status.ACTIVE).orElse(false);
    }

    @Override
    @Transactional(readOnly = true)
    public java.util.Optional<ResolvedContribution> resolve(UUID roomId, UUID memberUserId, UUID roleRef) {
        if (!isActiveMember(roomId, memberUserId)) {
            return java.util.Optional.empty();
        }
        if (roleRef == null) {
            return java.util.Optional.of(
                    new ResolvedContribution(memberUserId, memberUserId.toString(), null, null));
        }
        return roles.findById(roleRef)
                .filter(r -> r.getRoomId().equals(roomId)
                        && r.getMemberUserId().equals(memberUserId) && r.isActive())
                .map(r -> new ResolvedContribution(memberUserId, memberUserId.toString(),
                        r.getId(), r.getRoleName()));
    }

    // ---------- internals ----------

    private CreativeRoomEntity requireOwner(UUID caller, UUID roomId) {
        var room = rooms.findById(roomId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND));
        if (!room.isOwner(caller)) {
            throw new ApiException(ErrorCode.NOT_FOUND);           // don't leak ownership
        }
        return room;
    }

    private CreativeRoomEntity requireMember(UUID caller, UUID roomId) {
        var room = rooms.findById(roomId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND));
        if (!room.isOwner(caller) && !isActiveMember(roomId, caller)) {
            throw new ApiException(ErrorCode.NOT_FOUND);
        }
        return room;
    }

    private CreativeRoomEntity requireVisible(UUID caller, UUID roomId) {
        var room = rooms.findById(roomId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND));
        if (room.getVisibility() == CreativeRoomEntity.Visibility.PRIVATE
                && !room.isOwner(caller) && !isActiveMember(roomId, caller)) {
            throw new ApiException(ErrorCode.NOT_FOUND);
        }
        return room;
    }

    private RoomView toRoom(CreativeRoomEntity r) {
        return new RoomView(r.getId(), r.getName(), r.getStatus().name(),
                r.getVisibility().name(), r.getOwnerId(), r.getVersion());
    }

    private InvitationView toInv(ProjectInvitationEntity i) {
        return new InvitationView(i.getId(), i.getRoomId(), i.getStatus().name(),
                i.getExpiresAt() == null ? null : i.getExpiresAt().toString());
    }

    private int offset(String cursor) {
        if (cursor == null) return 0;
        String[] p = Cursor.decode(cursor);
        return p.length == 2 ? Integer.parseInt(p[1]) : 0;
    }

    private String stripQuotes(String etag) { return etag.replace("\"", ""); }
}
