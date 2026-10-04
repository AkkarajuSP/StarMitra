package com.starmitra.integration;

import com.starmitra.modules.competition.application.CompetitionService;
import com.starmitra.modules.identity.application.OtpService;
import com.starmitra.modules.identity.application.TestOtpSender;
import com.starmitra.modules.judge.application.JudgeService;
import com.starmitra.modules.judge.application.JudgeScopeService;
import com.starmitra.modules.submission.application.SubmissionService;
import com.starmitra.platform.error.ApiException;
import com.starmitra.platform.error.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/** M12 on real PG17 — judge lifecycle, scoped assignments, scope isolation. */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class JudgeFlowIT {

    @Autowired OtpService otpService;
    @Autowired JudgeService judges;
    @Autowired JudgeScopeService scope;
    @Autowired CompetitionService competitions;
    @Autowired SubmissionService submissions;
    @Autowired jakarta.persistence.EntityManager em;

    private UUID admin, judgeA, judgeB, organizer, contestant;

    private UUID register(String tag) {
        String email = tag + UUID.randomUUID().toString().substring(0, 6) + "@t.dev";
        otpService.request("EMAIL", email);
        return otpService.verify(email, TestOtpSender.lastOtpFor(email).orElseThrow());
    }

    private void asAdmin() {
        SecurityContextHolder.getContext().setAuthentication(
                new TestingAuthenticationToken("admin", null,
                        List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));
    }

    private void asJudge(UUID judgeUserId) {
        SecurityContextHolder.getContext().setAuthentication(
                new TestingAuthenticationToken(judgeUserId.toString(), null,
                        List.of(new SimpleGrantedAuthority("ROLE_JUDGE"))));
    }

    @BeforeEach
    void setUp() {
        TestOtpSender.clear();
        SecurityContextHolder.clearContext();
        admin = register("m12ad");
        judgeA = register("m12ja");
        judgeB = register("m12jb");
        organizer = register("m12o");
        contestant = register("m12c");
        asAdmin();
    }

    private record Env(UUID compId, UUID catId, UUID roundId, UUID subId) {}
    private Env env() {
        var compId = competitions.create(organizer,
                new CompetitionService.CompetitionCommand("Idol", null)).id();
        var cat = competitions.createCategory(organizer, compId,
                new CompetitionService.CategoryCommand("Solo", null, null));
        var round = competitions.createRound(organizer, compId,
                new CompetitionService.RoundCommand(1, "R1", null,
                        OffsetDateTime.now().plusDays(7), null, null, null));
        var p = competitions.register(contestant, compId, "USER", cat.id(), null);
        var s = submissions.create(contestant, p.id(), round.id());
        submissions.submit(contestant, s.id());
        submissions.finalize(contestant, s.id());
        return new Env(compId, cat.id(), round.id(), s.id());
    }

    @Test
    void judgeRegistrationAndRole() {
        var j = judges.createJudge(admin, judgeA);
        assertEquals(judgeA, j.userId());
        assertEquals("ACTIVE", j.status());
        // idempotent — same user returns same judge
        assertEquals(j.id(), judges.createJudge(admin, judgeA).id());
    }

    @Test
    void scopedAssignmentIsBoundary() {
        var j = judges.createJudge(admin, judgeA);
        var e = env();

        // before assignment — no scope
        assertFalse(scope.coversScope(j.id(), e.compId(), null, null));

        var a = judges.assign(admin, j.id(),
                new JudgeService.AssignmentCmd(e.compId(), e.catId(), e.roundId()));
        assertTrue(scope.coversScope(j.id(), e.compId(), e.catId(), e.roundId()));

        // cross-category denial
        var cat2 = competitions.createCategory(organizer, e.compId(),
                new CompetitionService.CategoryCommand("Other", null, null));
        assertFalse(scope.coversScope(j.id(), e.compId(), cat2.id(), e.roundId()));
        // cross-round denial
        var r2 = competitions.createRound(organizer, e.compId(),
                new CompetitionService.RoundCommand(2, "R2", null, null, null, null, null));
        assertFalse(scope.coversScope(j.id(), e.compId(), e.catId(), r2.id()));
        // cross-competition denial
        var otherComp = competitions.create(organizer,
                new CompetitionService.CompetitionCommand("Other Comp", null)).id();
        assertFalse(scope.coversScope(j.id(), otherComp, null, null));
        // dup scope → CONFLICT (UQ)
        assertThrows(ApiException.class, () -> judges.assign(admin, j.id(),
                new JudgeService.AssignmentCmd(e.compId(), e.catId(), e.roundId())));

        // revoke → scope gone
        judges.revokeAssignment(admin, a.id());
        assertFalse(scope.coversScope(j.id(), e.compId(), e.catId(), e.roundId()));
    }

    @Test
    void assignmentScopeValidatedByM09() {
        var j = judges.createJudge(admin, judgeA);
        var e = env();
        var otherComp = competitions.create(organizer,
                new CompetitionService.CompetitionCommand("X", null)).id();
        // round from wrong competition → rejected
        assertThrows(ApiException.class, () -> judges.assign(admin, j.id(),
                new JudgeService.AssignmentCmd(otherComp, null, e.roundId())));
        // random category → rejected
        assertThrows(ApiException.class, () -> judges.assign(admin, j.id(),
                new JudgeService.AssignmentCmd(e.compId(), UUID.randomUUID(), null)));
    }

    @Test
    void scopedSubmissionsOnlyAssigned() {
        var ja = judges.createJudge(admin, judgeA);
        judges.createJudge(admin, judgeB);
        var e = env();
        judges.assign(admin, ja.id(),
                new JudgeService.AssignmentCmd(e.compId(), e.catId(), null));   // cat scope, all rounds

        // judge A sees the finalized submission
        asJudge(judgeA);
        var mine = judges.myScopedSubmissions(judgeA, e.compId());
        assertEquals(1, mine.size());
        assertEquals(e.subId(), mine.get(0).id());

        // judge B (no assignment) sees none
        asJudge(judgeB);
        assertTrue(judges.myScopedSubmissions(judgeB, e.compId()).isEmpty());
        asAdmin();
    }

    @Test
    void roleRequiredForJudgePortal() {
        SecurityContextHolder.clearContext();      // no JUDGE role in context
        assertThrows(ApiException.class, () -> scope.requireJudge(contestant));
        assertEquals(ErrorCode.ROLE_REQUIRED,
                assertThrows(ApiException.class, () -> judges.myProfile(contestant)).code());
    }

    @Test
    void expertiseIsQualificationNotAuthorization() {
        var j = judges.createJudge(admin, judgeA);
        judges.setExpertise(admin, j.id(), List.of(
                new JudgeService.ExpertiseCmd(null, "Classical music", true)));
        em.flush();
        // expertise rows exist but do NOT grant scope
        var e = env();
        assertFalse(scope.coversScope(j.id(), e.compId(), null, null));
    }
}
