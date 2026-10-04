package com.starmitra.modules.voting.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.starmitra.modules.competition.application.CompetitionStructureContract;
import com.starmitra.modules.submission.application.SubmissionTruthContract;
import com.starmitra.modules.voting.persistence.*;
import com.starmitra.platform.audit.AuditService;
import com.starmitra.platform.error.ApiException;
import com.starmitra.platform.error.ErrorCode;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * M11 — Audience Voting. Authoritative vote truth.
 *
 * Vote derives target_type/comp/category/round from M10 submission truth —
 * never client-supplied, never split per contributor (PROJECT vote = one
 * target). DB enforces dedup (uq_voter+submission+round) and idempotency
 * (uq_client_msg_id); race-safe: UQ violation returns the winning vote.
 *
 * Behavior is data-driven via M11-owned `vote_configs` (versioned series;
 * round references a config_id). Configurable: maxVotesPerVoter, window.
 * FRS example weights are NOT product truth — absent config falls back to
 * round start/end window only.
 */
@Service
public class VoteService implements VoteTruthContract {

    private final VoteRepository votes;
    private final VoteConfigRepository configs;
    private final SubmissionTruthContract submissions;
    private final CompetitionStructureContract competition;
    private final AuditService audit;
    private final ObjectMapper json;

    public VoteService(VoteRepository votes, VoteConfigRepository configs,
                       SubmissionTruthContract submissions,
                       CompetitionStructureContract competition,
                       AuditService audit, ObjectMapper json) {
        this.votes = votes;
        this.configs = configs;
        this.submissions = submissions;
        this.competition = competition;
        this.audit = audit;
        this.json = json;
    }

    public record VoteView(UUID id, UUID submissionId, String targetType, String castAt) {}
    public record VoteCountView(UUID submissionId, long count, boolean derived) {}

    /**
     * Cast — validates submission is FINALIZED + in window; scope from M10
     * truth. Idempotent on clientMsgId AND on (voter,submission,round) —
     * replay returns the existing vote (201 still).
     */
    @Transactional
    public VoteView cast(UUID voter, UUID submissionId, String clientMsgId) {
        if (clientMsgId == null || clientMsgId.isBlank()) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED, "clientMsgId required");
        }
        var prior = votes.findByClientMsgId(clientMsgId);
        if (prior.isPresent()) return toView(prior.get());

        var s = submissions.submissionView(submissionId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND));
        if (!"FINALIZED".equals(s.state())) {
            throw new ApiException(ErrorCode.STATE_TRANSITION_INVALID,
                    "Submission is not votable");
        }
        var window = competition.roundWindow(s.roundId()).orElseThrow();
        enforceWindow(window);
        enforceLimit(voter, s, window);

        var existing = votes.findByVoterIdAndSubmissionId(voter, submissionId);
        if (existing.isPresent()) return toView(existing.get());   // dedup — replay

        var vote = new VoteEntity(voter, submissionId, s.competitionId(), s.categoryId(),
                s.roundId(), "PROJECT".equals(s.participantType())
                        ? VoteEntity.TargetType.PROJECT : VoteEntity.TargetType.INDIVIDUAL,
                clientMsgId);
        try {
            votes.saveAndFlush(vote);
        } catch (DataIntegrityViolationException race) {
            // concurrent duplicate or clientMsgId collision → return the winner
            return votes.findByClientMsgId(clientMsgId).map(this::toView)
                    .orElseGet(() -> toView(votes
                            .findByVoterIdAndSubmissionId(voter, submissionId).orElseThrow()));
        }
        audit.record("M11", "VOTE_CAST", voter, "user", "submission",
                submissionId.toString(), s.participantType());
        return toView(vote);
    }

    @Transactional(readOnly = true)
    public VoteCountView counts(UUID submissionId) {
        return new VoteCountView(submissionId, votes.countBySubmissionId(submissionId), true);
    }

    /** Admin-only (controller enforces ROLE_ADMIN). Versioned series. */
    @Transactional
    public UUID createConfig(String seriesKey, Map<String, Object> payload) {
        try {
            var cfg = new VoteConfigEntity(seriesKey, configs.maxVersion(seriesKey) + 1,
                    json.writeValueAsString(payload));
            return configs.save(cfg).getId();
        } catch (Exception e) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED, "Invalid config payload: " + e.getMessage());
        }
    }

    // ---------- VoteTruthContract (M14) ----------

    @Override @Transactional(readOnly = true)
    public long countForSubmission(UUID submissionId) {
        return votes.countBySubmissionId(submissionId);
    }

    @Override @Transactional(readOnly = true)
    public Map<UUID, Long> countsBySubmission(UUID roundId) {
        return votes.countsBySubmission(roundId).stream().collect(
                Collectors.toMap(VoteRepository.SubmissionCount::getSubmissionId,
                        VoteRepository.SubmissionCount::getCount));
    }

    // ---------- internals ----------

    /**
     * Config window (if configured) overrides; round window always applies.
     * vote_configs payload keys: windowStart, windowEnd — ISO timestamps.
     */
    private void enforceWindow(CompetitionStructureContract.RoundWindow window) {
        var now = OffsetDateTime.now();
        if (window.endAt() != null && now.isAfter(window.endAt())) {
            throw new ApiException(ErrorCode.STATE_TRANSITION_INVALID, "Voting window closed");
        }
        if (window.startAt() != null && now.isBefore(window.startAt())) {
            throw new ApiException(ErrorCode.STATE_TRANSITION_INVALID, "Voting not yet open");
        }
        if (window.voteConfigId() != null) {
            configs.findById(window.voteConfigId()).ifPresent(c -> {
                var p = readPayload(c);
                if (p.has("windowEnd") && now.isAfter(OffsetDateTime.parse(p.get("windowEnd").asText()))) {
                    throw new ApiException(ErrorCode.STATE_TRANSITION_INVALID, "Voting window closed");
                }
                if (p.has("windowStart") && now.isBefore(OffsetDateTime.parse(p.get("windowStart").asText()))) {
                    throw new ApiException(ErrorCode.STATE_TRANSITION_INVALID, "Voting not yet open");
                }
            });
        }
    }

    /** maxVotesPerVoter from the round's vote config (data-driven limit). */
    private void enforceLimit(UUID voter, SubmissionTruthContract.SubmissionDetails s,
                              CompetitionStructureContract.RoundWindow window) {
        if (window.voteConfigId() == null) return;
        configs.findById(window.voteConfigId()).ifPresent(c -> {
            var p = readPayload(c);
            if (p.has("maxVotesPerVoter")
                    && votes.countByVoterIdAndRoundId(voter, s.roundId())
                        >= p.get("maxVotesPerVoter").asInt()) {
                throw new ApiException(ErrorCode.STATE_TRANSITION_INVALID, "Vote limit reached");
            }
        });
    }

    private JsonNode readPayload(VoteConfigEntity c) {
        try {
            return json.readTree(c.getPayload());
        } catch (Exception e) {
            return json.createObjectNode();
        }
    }

    private VoteView toView(VoteEntity v) {
        return new VoteView(v.getId(), v.getSubmissionId(), v.getTargetType().name(),
                v.getCastAt().toString());
    }
}
