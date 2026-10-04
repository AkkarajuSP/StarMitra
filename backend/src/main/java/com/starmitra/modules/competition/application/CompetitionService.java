package com.starmitra.modules.competition.application;

import com.starmitra.modules.competition.persistence.*;
import com.starmitra.modules.room.application.ProjectMembershipContract;
import com.starmitra.modules.skill.application.SkillTaxonomyContract;
import com.starmitra.platform.audit.AuditService;
import com.starmitra.platform.error.ApiException;
import com.starmitra.platform.error.ErrorCode;
import com.starmitra.platform.pagination.Cursor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * M09 — Competitions. Authoritative competition truth: competition, categories
 * (multi-skill via M03-validated tags), rounds, eligibility rules, submission
 * config, participants (DB-03 XOR USER/PROJECT). Voting/judging/scoring/
 * progression/submission/leaderboard stay in M10–M16 via
 * CompetitionStructureContract. Eligibility rule storage is canonical;
 * per-rule-type evaluation is pluggable (open decision).
 */
@Service
public class CompetitionService implements CompetitionStructureContract {

    private final CompetitionRepository competitions;
    private final CompetitionCategoryRepository categories;
    private final CompetitionCategorySkillRepository categorySkills;
    private final CompetitionRoundRepository rounds;
    private final EligibilityRuleRepository rules;
    private final SubmissionConfigRepository submissionConfigs;
    private final CompetitionParticipantRepository participants;
    private final SkillTaxonomyContract skills;
    private final ProjectMembershipContract projectMembers;
    private final AuditService audit;

    public CompetitionService(CompetitionRepository competitions,
                              CompetitionCategoryRepository categories,
                              CompetitionCategorySkillRepository categorySkills,
                              CompetitionRoundRepository rounds, EligibilityRuleRepository rules,
                              SubmissionConfigRepository submissionConfigs,
                              CompetitionParticipantRepository participants,
                              SkillTaxonomyContract skills,
                              ProjectMembershipContract projectMembers, AuditService audit) {
        this.competitions = competitions;
        this.categories = categories;
        this.categorySkills = categorySkills;
        this.rounds = rounds;
        this.rules = rules;
        this.submissionConfigs = submissionConfigs;
        this.participants = participants;
        this.skills = skills;
        this.projectMembers = projectMembers;
        this.audit = audit;
    }

    public record CompetitionView(UUID id, String title, String configStatus,
                                  String participationStatus, String roundState, int version) {}
    public record CategoryView(UUID id, String name) {}
    public record RoundView(UUID id, int sequence, String name, String startAt, String endAt,
                            UUID voteConfigId, UUID rubricVersionId, UUID progressionConfigId) {}
    public record ParticipantView(UUID id, String participantType, UUID userId,
                                  UUID projectId, String status) {}
    public record Page(String nextCursor, boolean hasMore, Integer total) {}
    public record PageResult<T>(List<T> items, Page page) {}
    public record CompetitionCommand(String title, String description) {}
    public record CategoryCommand(String name, String description, List<UUID> skillIds) {}
    public record RoundCommand(int sequence, String name, OffsetDateTime startAt,
                               OffsetDateTime endAt, UUID voteConfigId, UUID rubricVersionId,
                               UUID progressionConfigId) {}

    // ---------- competitions ----------

    @Transactional(readOnly = true)
    public PageResult<CompetitionView> list(UUID caller, String status, String cursor, Integer limit) {
        int size = Cursor.limit(limit);
        int offset = offset(cursor);
        var rows = competitions.findListing(status, PageRequest.of(0, size + 1 + offset));
        var window = rows.size() > offset ? rows.subList(offset, rows.size()) : List.<CompetitionEntity>of();
        boolean hasMore = window.size() > size;
        return new PageResult<>(window.stream().limit(size).map(this::toComp).toList(),
                new Page(hasMore ? Cursor.encode("o", String.valueOf(offset + size)) : null, hasMore, null));
    }

    @Transactional
    public CompetitionView create(UUID caller, CompetitionCommand cmd) {
        var c = competitions.save(new CompetitionEntity(cmd.title(), cmd.description(), caller));
        audit.record("M09", "COMPETITION_CREATED", caller, "user", "competition", c.getId().toString(), null);
        return toComp(c);
    }

    @Transactional(readOnly = true)
    public CompetitionView get(UUID caller, UUID competitionId) {
        return toComp(requireComp(competitionId));
    }

    @Transactional
    public CompetitionView update(UUID caller, UUID competitionId, CompetitionCommand cmd, String ifMatch) {
        var c = requireOwner(caller, competitionId);
        assertVersion(ifMatch, c.getVersion());
        c.update(cmd.title(), cmd.description());
        return toComp(competitions.saveAndFlush(c));
    }

    // ---------- categories (multi-skill; skill is capability tag, not permission) ----------

    @Transactional
    public CategoryView createCategory(UUID caller, UUID competitionId, CategoryCommand cmd) {
        var c = requireOwner(caller, competitionId);
        var cat = categories.save(new CompetitionCategoryEntity(c.getId(), cmd.name(), cmd.description()));
        for (UUID skillId : cmd.skillIds() == null ? List.<UUID>of() : cmd.skillIds()) {
            if (!skills.isActiveSkill(skillId)) {
                throw new ApiException(ErrorCode.VALIDATION_FAILED, "Skill not found or inactive");
            }
            categorySkills.save(new CompetitionCategorySkillEntity(cat.getId(), skillId));
        }
        return new CategoryView(cat.getId(), cat.getName());
    }

    // ---------- rounds ----------

    @Transactional
    public RoundView createRound(UUID caller, UUID competitionId, RoundCommand cmd) {
        var c = requireOwner(caller, competitionId);
        var r = rounds.save(new CompetitionRoundEntity(c.getId(), cmd.sequence(), cmd.name(),
                cmd.startAt(), cmd.endAt(), cmd.voteConfigId(), cmd.rubricVersionId(),
                cmd.progressionConfigId()));
        return new RoundView(r.getId(), r.getSequence(), r.getName(),
                r.getStartAt() == null ? null : r.getStartAt().toString(),
                r.getEndAt() == null ? null : r.getEndAt().toString(),
                r.getVoteConfigId(), r.getRubricVersionId(), r.getProgressionConfigId());
    }

    // ---------- eligibility + submission config ----------

    @Transactional
    public void addEligibilityRule(UUID caller, UUID competitionId, String ruleType, String ruleParams) {
        var c = requireOwner(caller, competitionId);
        rules.save(new EligibilityRuleEntity(c.getId(), ruleType, ruleParams));
    }

    @Transactional
    public void putSubmissionConfig(UUID caller, UUID competitionId, String configJson, String ifMatch) {
        var c = requireOwner(caller, competitionId);
        var existing = submissionConfigs.findByCompetitionId(competitionId);
        if (existing.isPresent()) {
            var cfg = existing.get();
            assertVersion(ifMatch, cfg.getVersion());
            cfg.updateConfig(configJson);
            submissionConfigs.save(cfg);
        } else {
            submissionConfigs.save(new SubmissionConfigEntity(competitionId, null, configJson));
        }
        c.markConfigured();
        competitions.save(c);
    }

    // ---------- participants (USER xor PROJECT; eligibility ≠ permission) ----------

    /**
     * Register: participation OPEN + competition exists + optional category
     * belongs to comp. USER → caller self. PROJECT → caller must be an
     * ACTIVE member of that M07 room. UQ dedups.
     */
    @Transactional
    public ParticipantView register(UUID caller, UUID competitionId, String participantType,
                                    UUID categoryId, UUID projectId) {
        var c = requireComp(competitionId);
        if (c.getParticipationStatus() != CompetitionEntity.ParticipationStatus.OPEN) {
            throw new ApiException(ErrorCode.STATE_TRANSITION_INVALID, "Registration closed");
        }
        if (categoryId != null && !categoryBelongsTo(competitionId, categoryId)) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED, "Category does not belong to competition");
        }
        CompetitionParticipantEntity p;
        if ("USER".equals(participantType)) {
            p = CompetitionParticipantEntity.forUser(competitionId, categoryId, caller);
        } else if ("PROJECT".equals(participantType)) {
            if (projectId == null || !projectMembers.isActiveMember(projectId, caller)) {
                throw new ApiException(ErrorCode.NOT_A_MEMBER, "Not an active project member");
            }
            p = CompetitionParticipantEntity.forProject(competitionId, categoryId, projectId);
        } else {
            throw new ApiException(ErrorCode.VALIDATION_FAILED, "participantType must be USER|PROJECT");
        }
        try {
            return toParticipant(participants.saveAndFlush(p));
        } catch (DataIntegrityViolationException dup) {
            throw new ApiException(ErrorCode.CONFLICT, "Already registered");
        }
    }

    @Transactional(readOnly = true)
    public PageResult<ParticipantView> listParticipants(UUID caller, UUID competitionId,
                                                       String cursor, Integer limit) {
        requireComp(competitionId);
        int size = Cursor.limit(limit);
        int offset = offset(cursor);
        var rows = participants.findByCompetitionIdOrderByRegisteredAtAsc(
                competitionId, PageRequest.of(0, size + 1 + offset));
        var window = rows.size() > offset ? rows.subList(offset, rows.size()) : List.<CompetitionParticipantEntity>of();
        boolean hasMore = window.size() > size;
        return new PageResult<>(window.stream().limit(size).map(this::toParticipant).toList(),
                new Page(hasMore ? Cursor.encode("o", String.valueOf(offset + size)) : null, hasMore, null));
    }

    // ---------- CompetitionStructureContract (M10–M16) ----------

    @Override @Transactional(readOnly = true)
    public boolean roundBelongsTo(UUID competitionId, UUID roundId) {
        return rounds.findById(roundId)
                .map(r -> r.getCompetitionId().equals(competitionId)).orElse(false);
    }

    @Override @Transactional(readOnly = true)
    public boolean categoryBelongsTo(UUID competitionId, UUID categoryId) {
        return categories.findById(categoryId)
                .map(c -> c.getCompetitionId().equals(competitionId)).orElse(false);
    }

    @Override @Transactional(readOnly = true)
    public boolean isActiveParticipant(UUID competitionId, UUID participantId) {
        return participants.findById(participantId)
                .map(p -> p.getCompetitionId().equals(competitionId)
                        && p.getStatus() == CompetitionParticipantEntity.Status.ACTIVE).orElse(false);
    }

    @Override @Transactional(readOnly = true)
    public boolean isOpenForParticipation(UUID competitionId) {
        return competitions.findById(competitionId)
                .map(c -> c.getParticipationStatus() == CompetitionEntity.ParticipationStatus.OPEN)
                .orElse(false);
    }

    @Override @Transactional(readOnly = true)
    public java.util.Optional<ParticipantDetails> participantView(UUID participantId) {
        return participants.findById(participantId)
                .map(p -> new ParticipantDetails(p.getId(), p.getCompetitionId(), p.getCategoryId(),
                        p.getParticipantType().name(), p.getUserId(), p.getProjectId(),
                        p.getStatus().name()));
    }

    @Override @Transactional(readOnly = true)
    public java.util.Optional<RoundWindow> roundWindow(UUID roundId) {
        return rounds.findById(roundId)
                .map(r -> new RoundWindow(r.getId(), r.getCompetitionId(),
                        r.getStartAt(), r.getEndAt()));
    }

    // ---------- internals ----------

    private CompetitionEntity requireComp(UUID competitionId) {
        return competitions.findById(competitionId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND));
    }

    private CompetitionEntity requireOwner(UUID caller, UUID competitionId) {
        var c = requireComp(competitionId);
        if (!c.isOwner(caller)) {
            throw new ApiException(ErrorCode.NOT_FOUND);
        }
        return c;
    }

    private void assertVersion(String ifMatch, int version) {
        if (ifMatch != null && !String.valueOf(version).equals(ifMatch.replace("\"", ""))) {
            throw new ApiException(ErrorCode.CONFLICT_VERSION, "Stale version");
        }
    }

    private CompetitionView toComp(CompetitionEntity c) {
        return new CompetitionView(c.getId(), c.getTitle(), c.getConfigStatus().name(),
                c.getParticipationStatus().name(), c.getRoundState().name(), c.getVersion());
    }

    private ParticipantView toParticipant(CompetitionParticipantEntity p) {
        return new ParticipantView(p.getId(), p.getParticipantType().name(),
                p.getUserId(), p.getProjectId(), p.getStatus().name());
    }

    private int offset(String cursor) {
        if (cursor == null) return 0;
        String[] p = Cursor.decode(cursor);
        return p.length == 2 ? Integer.parseInt(p[1]) : 0;
    }
}
