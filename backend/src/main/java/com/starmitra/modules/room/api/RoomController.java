package com.starmitra.modules.room.api;

import com.starmitra.modules.room.application.RoomService;
import com.starmitra.platform.security.SecurityUtils;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** /api/v1/rooms + /api/v1/invitations — M07. */
@RestController
public class RoomController {

    private final RoomService rooms;

    public RoomController(RoomService rooms) {
        this.rooms = rooms;
    }

    @GetMapping("/api/v1/rooms")
    public ResponseEntity<RoomDtos.RoomPage> listRooms(
            @RequestParam(required = false) UUID skillId,
            @RequestParam(required = false) String cursor,
            @RequestParam(required = false) Integer limit) {
        var p = rooms.listRooms(SecurityUtils.currentUserId(), skillId, cursor, limit);
        return ResponseEntity.ok(new RoomDtos.RoomPage(
                p.items().stream().map(this::toDto).toList(),
                new RoomDtos.PageMeta(p.page().nextCursor(), p.page().hasMore(), p.page().total())));
    }

    @PostMapping("/api/v1/rooms")
    public ResponseEntity<RoomDtos.Room> createRoom(@Valid @RequestBody RoomDtos.RoomCreate body) {
        var r = rooms.createRoom(SecurityUtils.currentUserId(),
                new RoomService.RoomCommand(body.name(), body.description(), body.visibility()));
        return ResponseEntity.status(HttpStatus.CREATED).body(toDto(r));
    }

    @GetMapping("/api/v1/rooms/{roomId}")
    public ResponseEntity<RoomDtos.Room> getRoom(@PathVariable UUID roomId) {
        return ResponseEntity.ok(toDto(rooms.getRoom(SecurityUtils.currentUserId(), roomId)));
    }

    @PutMapping("/api/v1/rooms/{roomId}")
    public ResponseEntity<RoomDtos.Room> updateRoom(
            @PathVariable UUID roomId,
            @RequestHeader(name = "If-Match", required = false) String ifMatch,
            @Valid @RequestBody RoomDtos.RoomCreate body) {
        var r = rooms.updateRoom(SecurityUtils.currentUserId(), roomId,
                new RoomService.RoomCommand(body.name(), body.description(), body.visibility()), ifMatch);
        return ResponseEntity.ok().eTag(String.valueOf(r.version())).body(toDto(r));
    }

    @GetMapping("/api/v1/rooms/{roomId}/members")
    public ResponseEntity<List<RoomDtos.ProjectMember>> listRoomMembers(@PathVariable UUID roomId) {
        return ResponseEntity.ok(rooms.listMembers(SecurityUtils.currentUserId(), roomId).stream()
                .map(m -> new RoomDtos.ProjectMember(m.userId(), m.status(), m.joinedAt(), m.contributionRoles()))
                .toList());
    }

    @PutMapping("/api/v1/rooms/{roomId}/required-skills/{skillId}")
    public ResponseEntity<Void> addRequiredSkill(@PathVariable UUID roomId, @PathVariable UUID skillId) {
        rooms.addRequiredSkill(SecurityUtils.currentUserId(), roomId, skillId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/api/v1/rooms/{roomId}/required-skills/{skillId}")
    public ResponseEntity<Void> removeRequiredSkill(@PathVariable UUID roomId, @PathVariable UUID skillId) {
        rooms.removeRequiredSkill(SecurityUtils.currentUserId(), roomId, skillId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/api/v1/rooms/{roomId}/invitations")
    public ResponseEntity<RoomDtos.Invitation> inviteToRoom(@PathVariable UUID roomId,
                                                          @Valid @RequestBody RoomDtos.InviteCreate body) {
        var i = rooms.invite(SecurityUtils.currentUserId(), roomId, body.inviteeId());
        return ResponseEntity.status(HttpStatus.CREATED).body(toInv(i));
    }

    @GetMapping("/api/v1/invitations/me")
    public ResponseEntity<List<RoomDtos.Invitation>> myInvitations() {
        return ResponseEntity.ok(rooms.myInvitations(SecurityUtils.currentUserId()).stream()
                .map(this::toInv).toList());
    }

    @PostMapping("/api/v1/invitations/{invitationId}/respond")
    public ResponseEntity<RoomDtos.Invitation> respondToInvitation(
            @PathVariable UUID invitationId, @Valid @RequestBody RoomDtos.InviteResponse body) {
        return ResponseEntity.ok(toInv(
                rooms.respond(SecurityUtils.currentUserId(), invitationId, body.action())));
    }

    @PostMapping("/api/v1/rooms/{roomId}/contributions")
    public ResponseEntity<RoomDtos.Contribution> assignContributionRole(
            @PathVariable UUID roomId, @Valid @RequestBody RoomDtos.ContributionAssign body) {
        var c = rooms.assignRole(SecurityUtils.currentUserId(), roomId,
                body.memberUserId(), body.roleName());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new RoomDtos.Contribution(c.id(), c.userId(), c.roleName(), c.verified()));
    }

    @GetMapping("/api/v1/rooms/{roomId}/tasks")
    public ResponseEntity<List<RoomDtos.RoomTask>> listRoomTasks(@PathVariable UUID roomId) {
        return ResponseEntity.ok(rooms.listTasks(SecurityUtils.currentUserId(), roomId).stream()
                .map(t -> new RoomDtos.RoomTask(t.id(), t.title(), t.status(), t.assigneeMemberId())).toList());
    }

    @PostMapping("/api/v1/rooms/{roomId}/tasks")
    public ResponseEntity<RoomDtos.RoomTask> createRoomTask(@PathVariable UUID roomId,
                                                            @Valid @RequestBody RoomDtos.TaskCreate body) {
        var t = rooms.createTask(SecurityUtils.currentUserId(), roomId, body.title(), body.description(),
                body.assigneeMemberId(), body.dueDate() == null ? null : LocalDate.parse(body.dueDate()));
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new RoomDtos.RoomTask(t.id(), t.title(), t.status(), t.assigneeMemberId()));
    }

    @PostMapping("/api/v1/rooms/{roomId}/assets")
    public ResponseEntity<Void> linkRoomAsset(@PathVariable UUID roomId,
                                              @Valid @RequestBody RoomDtos.AssetLink body) {
        rooms.linkAsset(SecurityUtils.currentUserId(), roomId, body.mediaId(), body.role());
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PostMapping("/api/v1/rooms/{roomId}/final-outputs")
    public ResponseEntity<Void> addFinalOutput(@PathVariable UUID roomId,
                                               @Valid @RequestBody RoomDtos.AssetLink body) {
        rooms.addFinalOutput(SecurityUtils.currentUserId(), roomId, body.mediaId(), body.role());
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @GetMapping("/api/v1/rooms/{roomId}/credits")
    public ResponseEntity<List<RoomDtos.Contribution>> listProjectCredits(@PathVariable UUID roomId) {
        return ResponseEntity.ok(rooms.listCredits(SecurityUtils.currentUserId(), roomId).stream()
                .map(c -> new RoomDtos.Contribution(c.id(), c.userId(), c.roleName(), c.verified())).toList());
    }

    private RoomDtos.Room toDto(RoomService.RoomView r) {
        return new RoomDtos.Room(r.id(), r.name(), r.status(), r.visibility(), r.ownerId(), r.version());
    }

    private RoomDtos.Invitation toInv(RoomService.InvitationView i) {
        return new RoomDtos.Invitation(i.id(), i.roomId(), i.status(), i.expiresAt());
    }
}
