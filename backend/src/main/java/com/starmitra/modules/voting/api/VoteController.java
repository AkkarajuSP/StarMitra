package com.starmitra.modules.voting.api;

import com.starmitra.modules.voting.application.VoteService;
import com.starmitra.platform.security.SecurityUtils;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

/** /api/v1/votes + /api/v1/vote-configs — M11. */
@RestController
public class VoteController {

    private final VoteService votes;

    public VoteController(VoteService votes) {
        this.votes = votes;
    }

    @PostMapping("/api/v1/votes")
    public ResponseEntity<VoteDtos.Vote> castVote(@Valid @RequestBody VoteDtos.VoteCast body) {
        var v = votes.cast(SecurityUtils.currentUserId(), body.submissionId(), body.clientMsgId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new VoteDtos.Vote(v.id(), v.submissionId(), v.targetType(), v.castAt()));
    }

    @GetMapping("/api/v1/votes/counts")
    public ResponseEntity<VoteDtos.VoteCount> getVoteCounts(@RequestParam UUID submissionId) {
        var c = votes.counts(submissionId);
        return ResponseEntity.ok(new VoteDtos.VoteCount(c.submissionId(), c.count(), c.derived()));
    }

    @PostMapping("/api/v1/vote-configs")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> createVoteConfig(
            @Valid @RequestBody VoteDtos.VoteConfigCreate body) {
        var id = votes.createConfig(body.seriesKey(), body.payload());
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("id", id.toString()));
    }
}
