package com.starmitra.modules.submission.application;

import com.starmitra.modules.competition.application.CompetitionStructureContract;
import com.starmitra.modules.media.application.MediaReferenceContract;
import com.starmitra.modules.room.application.ProjectContributionContract;
import com.starmitra.modules.room.application.ProjectMembershipContract;
import com.starmitra.modules.submission.persistence.*;
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
 * M10 — Submissions. Authoritative submission truth.
 *
 * One submission = one entry (a PROJECT participant = ONE team submission,
 * never split per contributor). Contributors hold live M07 refs
 * (member_ref = user, role_ref = contribution role) with DB-02 snapshots
 * captured at finalize. Roles are contextual M07 roles — never inferred
 * from TalentSkill, never manufactured.
 *
 * Authorization: USER participant → caller is that user; PROJECT
 * participant → caller is active M07 member. Deadline = round.end_at.
 */
@Service
public class SubmissionService implements SubmissionTruthContract {

    private final SubmissionRepository submissions;
    private final SubmissionMediaRepository media;
    private final SubmissionContributorRepository contributors;
    private final SubmissionHistoryRepository history;
    private final CompetitionStructureContract competition;
    private final ProjectMembershipContract projectMembers;
    private final ProjectContributionContract contributions;
    private final MediaReferenceContract mediaRef;
    private final AuditService audit;

    public SubmissionService(SubmissionRepository submissions, SubmissionMediaRepository media,
                             SubmissionContributorRepository contributors,
                             SubmissionHistoryRepository history,
                             CompetitionStructureContract competition,
                             ProjectMembershipContract projectMembers,
                             ProjectContributionContract contributions,
                             MediaReferenceContract mediaRef, AuditService audit) {
        this.submissions = submissions;
        this.media = media;
        this.contributors = contributors;
        this.history = history;
        this.competition = competition;
        this.projectMembers = projectMembers;
        this.contributions = contributions;
        this.mediaRef = mediaRef;
        this.audit = audit;
    }

    public record SubmissionView(UUID id, UUID participantId, UUID competitionId, UUID roundId,
                                 String state, String submittedAt, String finalizedAt,
                                 List<UUID> mediaIds, int version) {}
    public record HistoryView(String fromState, String toState, String at) {}
    public record ContributorCmd(UUID memberRef, UUID roleRef) {}
    public record Page(String nextCursor, boolean hasMore, Integer total) {}
    public record PageResult<T>(List<T> items, Page page) {}

    // ---------- create ----------

    /**
     * participantId + roundId in; comp/category derived from M09 participant
     * truth (never client-supplied). One submission per participant per round
     * (non-WITHDRAWN) → CONFLICT.
     */
    @Transactional
    public SubmissionView create(UUID caller, UUID participantId, UUID roundId) {
        var p = competition.participantView(participantId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND));
        requireParticipantAuth(caller, p);
        if (!p.status().equals("ACTIVE")) {
            throw new ApiException(ErrorCode.STATE_TRANSITION_INVALID, "Participant not active");
        }
        if (!competition.roundBelongsTo(p.competitionId(), roundId)) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED, "Round not in this competition");
        }
        if (p.categoryId() == null) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED, "Participant has no category");
        }
        if (submissions.existsByParticipantIdAndRoundIdAndStateNot(
                participantId, roundId, SubmissionEntity.State.WITHDRAWN)) {
            throw new ApiException(ErrorCode.CONFLICT, "Submission already exists for this round");
        }
        var s = submissions.save(new SubmissionEntity(participantId, p.competitionId(),
                p.categoryId(), roundId));
        record(s, null, "DRAFT", caller, "created");
        audit.record("M10", "SUBMISSION_CREATED", caller, "user", "submission", s.getId().toString(), null);
        return toView(s);
    }

    @Transactional(readOnly = true)
    public SubmissionView get(UUID caller, UUID submissionId) {
        var s = requireSubmission(submissionId);
        requireParticipantAuth(caller, participantOf(s));
        return toView(s);
    }

    // ---------- evidence ----------

    /** Attach media — DRAFT/SUBMITTED only; media validated via M04. */
    @Transactional
    public void attachMedia(UUID caller, UUID submissionId, UUID mediaId, Integer sortOrder) {
        var s = requireSubmission(submissionId);
        requireParticipantAuth(caller, participantOf(s));
        requireMutable(s);
        if (!mediaRef.isUsableBy(mediaId, caller)) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED, "Media not usable");
        }
        if (!media.existsById(new SubmissionMediaEntity.Pk(submissionId, mediaId))) {
            try {
                media.saveAndFlush(new SubmissionMediaEntity(submissionId, mediaId, sortOrder));
            } catch (DataIntegrityViolationException dup) { /* already attached */ }
        }
    }

    // ---------- contributors (DB-02: live refs; snapshot at finalize) ----------

    /**
     * Declare contributors — PROJECT submissions only. Every member_ref must
     * be an ACTIVE member of the project room; role_ref must be that member's
     * ACTIVE M07 contribution role (never inferred from TalentSkill).
     */
    @Transactional
    public void declareContributors(UUID caller, UUID submissionId, List<ContributorCmd> cmds) {
        var s = requireSubmission(submissionId);
        var p = participantOf(s);
        requireParticipantAuth(caller, p);
        requireMutable(s);
        if (!"PROJECT".equals(p.type())) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED,
                    "Contributors apply to project submissions");
        }
        for (var c : cmds) {
            var resolved = contributions.resolve(p.projectId(), c.memberRef(), c.roleRef())
                    .orElseThrow(() -> new ApiException(ErrorCode.VALIDATION_FAILED,
                            "Contributor not resolvable against project"));
            contributors.save(new SubmissionContributorEntity(
                    submissionId, resolved.memberUserId(), resolved.roleId()));
        }
    }

    // ---------- state transitions ----------

    /** DRAFT→SUBMITTED — deadline = round.end_at (server clock). */
    @Transactional
    public SubmissionView submit(UUID caller, UUID submissionId) {
        var s = requireSubmission(submissionId);
        requireParticipantAuth(caller, participantOf(s));
        if (s.getState() != SubmissionEntity.State.DRAFT) {
            throw new ApiException(ErrorCode.STATE_TRANSITION_INVALID, "Only DRAFT can submit");
        }
        var window = competition.roundWindow(s.getRoundId()).orElseThrow();
        if (window.endAt() != null && OffsetDateTime.now().isAfter(window.endAt())) {
            throw new ApiException(ErrorCode.STATE_TRANSITION_INVALID, "Submission deadline passed");
        }
        s.submit();
        record(s, "DRAFT", "SUBMITTED", caller, null);
        return toView(submissions.saveAndFlush(s));
    }

    /** SUBMITTED→FINALIZED — freezes evidence; captures DB-02 contributor snapshots. */
    @Transactional
    public SubmissionView finalize(UUID caller, UUID submissionId) {
        var s = requireSubmission(submissionId);
        var p = participantOf(s);
        requireParticipantAuth(caller, p);
        if (s.getState() != SubmissionEntity.State.SUBMITTED) {
            throw new ApiException(ErrorCode.STATE_TRANSITION_INVALID, "Only SUBMITTED can finalize");
        }
        if ("PROJECT".equals(p.type())) {
            for (var c : contributors.findBySubmissionId(submissionId)) {
                var resolved = contributions.resolve(p.projectId(), c.getMemberRef(), c.getRoleRef());
                c.captureSnapshot(
                        resolved.map(ProjectContributionContract.ResolvedContribution::memberDisplay)
                                .orElse(c.getMemberRef().toString()),
                        resolved.map(ProjectContributionContract.ResolvedContribution::roleName)
                                .orElse(c.getSnapshotRoleName()));
                contributors.save(c);
            }
        }
        s.finalize();
        record(s, "SUBMITTED", "FINALIZED", caller, null);
        audit.record("M10", "SUBMISSION_FINALIZED", caller, "user", "submission", submissionId.toString(), null);
        return toView(submissions.saveAndFlush(s));
    }

    /** DRAFT/SUBMITTED→WITHDRAWN — terminal; FINALIZED cannot withdraw. */
    @Transactional
    public SubmissionView withdraw(UUID caller, UUID submissionId) {
        var s = requireSubmission(submissionId);
        requireParticipantAuth(caller, participantOf(s));
        var from = s.getState();
        if (from == SubmissionEntity.State.FINALIZED || from == SubmissionEntity.State.WITHDRAWN) {
            throw new ApiException(ErrorCode.STATE_TRANSITION_INVALID, "Cannot withdraw " + from);
        }
        s.withdraw();
        record(s, from.name(), "WITHDRAWN", caller, null);
        return toView(submissions.saveAndFlush(s));
    }

    @Transactional(readOnly = true)
    public List<HistoryView> history(UUID caller, UUID submissionId) {
        var s = requireSubmission(submissionId);
        requireParticipantAuth(caller, participantOf(s));
        return history.findBySubmissionIdOrderByCreatedAtAsc(submissionId).stream()
                .map(h -> new HistoryView(h.getFromState(), h.getToState(), h.getCreatedAt().toString()))
                .toList();
    }

    @Transactional(readOnly = true)
    public PageResult<SubmissionView> list(UUID caller, UUID competitionId, UUID roundId,
                                           UUID categoryId, String cursor, Integer limit) {
        int size = Cursor.limit(limit);
        int offset = offset(cursor);
        var rows = submissions.findListing(competitionId, roundId, categoryId,
                PageRequest.of(0, size + 1 + offset));
        var window = rows.size() > offset ? rows.subList(offset, rows.size()) : List.<SubmissionEntity>of();
        boolean hasMore = window.size() > size;
        return new PageResult<>(window.stream().limit(size).map(this::toView).toList(),
                new Page(hasMore ? Cursor.encode("o", String.valueOf(offset + size)) : null, hasMore, null));
    }

    // ---------- SubmissionTruthContract (M11–M16) ----------

    @Override @Transactional(readOnly = true)
    public boolean isFinalizedSubmission(UUID submissionId, UUID roundId) {
        return submissions.findById(submissionId)
                .map(s -> s.getState() == SubmissionEntity.State.FINALIZED
                        && s.getRoundId().equals(roundId)).orElse(false);
    }

    @Override @Transactional(readOnly = true)
    public boolean belongsToCompetition(UUID submissionId, UUID competitionId) {
        return submissions.findById(submissionId)
                .map(s -> s.getCompetitionId().equals(competitionId)).orElse(false);
    }

    @Override @Transactional(readOnly = true)
    public java.util.Optional<SubmissionDetails> submissionView(UUID submissionId) {
        return submissions.findById(submissionId).map(s -> {
            var p = competition.participantView(s.getParticipantId())
                    .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND));
            return new SubmissionDetails(s.getId(), s.getParticipantId(), p.type(),
                    s.getCompetitionId(), s.getCategoryId(), s.getRoundId(), s.getState().name());
        });
    }

    @Override @Transactional(readOnly = true)
    public java.util.List<UUID> finalizedInScope(UUID competitionId, UUID categoryId, UUID roundId) {
        return submissions.findFinalizedIds(competitionId, categoryId, roundId);
    }

    @Override @Transactional(readOnly = true)
    public java.util.List<UUID> mediaIdsOf(UUID submissionId) {
        return media.findMediaIds(submissionId);
    }

    // ---------- internals ----------

    private SubmissionEntity requireSubmission(UUID submissionId) {
        return submissions.findById(submissionId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND));
    }

    private CompetitionStructureContract.ParticipantDetails participantOf(SubmissionEntity s) {
        return competition.participantView(s.getParticipantId()).orElseThrow();
    }

    /** USER → caller is that user; PROJECT → caller is ACTIVE M07 member. */
    private void requireParticipantAuth(UUID caller,
                                        CompetitionStructureContract.ParticipantDetails p) {
        boolean ok = "PROJECT".equals(p.type())
                ? projectMembers.isActiveMember(p.projectId(), caller)
                : p.userId().equals(caller);
        if (!ok) throw new ApiException(ErrorCode.NOT_FOUND);    // IDOR-safe
    }

    private void requireMutable(SubmissionEntity s) {
        if (s.getState() == SubmissionEntity.State.FINALIZED
                || s.getState() == SubmissionEntity.State.WITHDRAWN) {
            throw new ApiException(ErrorCode.STATE_TRANSITION_INVALID,
                    "Submission " + s.getState() + " is frozen");
        }
    }

    private void record(SubmissionEntity s, String from, String to, UUID actor, String reason) {
        history.save(new SubmissionHistoryEntity(s.getId(), from, to, actor, reason));
    }

    private SubmissionView toView(SubmissionEntity s) {
        return new SubmissionView(s.getId(), s.getParticipantId(), s.getCompetitionId(),
                s.getRoundId(), s.getState().name(),
                s.getSubmittedAt() == null ? null : s.getSubmittedAt().toString(),
                s.getFinalizedAt() == null ? null : s.getFinalizedAt().toString(),
                media.findMediaIds(s.getId()), s.getVersion());
    }

    private int offset(String cursor) {
        if (cursor == null) return 0;
        String[] p = Cursor.decode(cursor);
        return p.length == 2 ? Integer.parseInt(p[1]) : 0;
    }
}
