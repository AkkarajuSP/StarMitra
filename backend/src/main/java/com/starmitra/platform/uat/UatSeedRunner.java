package com.starmitra.platform.uat;

import com.starmitra.modules.competition.application.CompetitionService;
import com.starmitra.modules.identity.application.RegistrationService;
import com.starmitra.modules.identity.application.SystemRoleContract;
import com.starmitra.modules.identity.persistence.UserRepository;
import com.starmitra.modules.judge.application.JudgeService;
import com.starmitra.modules.leaderboard.application.LeaderboardService;
import com.starmitra.modules.moderation.application.ModerationService;
import com.starmitra.modules.profile.application.ProfileService;
import com.starmitra.modules.progression.application.ProgressionService;
import com.starmitra.modules.room.application.RoomService;
import com.starmitra.modules.rubric.application.RubricService;
import com.starmitra.modules.rubric.persistence.EvaluationTemplateVersionRepository;
import com.starmitra.modules.scoring.application.ScoringService;
import com.starmitra.modules.skill.application.SkillService;
import com.starmitra.modules.submission.application.SubmissionService;
import com.starmitra.modules.voting.application.VoteService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * UAT seed — representative demo data across the whole product, created once
 * (idempotent on the uat-admin marker). Runs ONLY under the `uat` profile and
 * drives the real module services — never raw SQL — so every seeded row went
 * through the same rules as production traffic.
 *
 * UAT accounts (all email OTP; the UatOtpSender prints codes to the console):
 *   uat-user@starmitra.dev        USER (audience)
 *   uat-creator@starmitra.dev     USER (talent, PUBLIC profile + portfolio)
 *   uat-judge@starmitra.dev       USER + JUDGE (assigned to UAT R1)
 *   uat-admin@starmitra.dev       USER + ADMIN
 *   uat-superadmin@starmitra.dev  USER + SUPER_ADMIN
 */
@Component
@Profile("uat")
public class UatSeedRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(UatSeedRunner.class);
    private static final String MARKER = "uat-admin@starmitra.dev";

    private final RegistrationService registration;
    private final SystemRoleContract systemRoles;
    private final UserRepository users;
    private final ProfileService profiles;
    private final SkillService skills;
    private final RoomService rooms;
    private final CompetitionService competitions;
    private final SubmissionService submissions;
    private final VoteService votes;
    private final JudgeService judges;
    private final RubricService rubrics;
    private final EvaluationTemplateVersionRepository rubricVersions;
    private final ScoringService scoring;
    private final ProgressionService progression;
    private final LeaderboardService leaderboards;
    private final ModerationService moderation;

    public UatSeedRunner(RegistrationService registration, SystemRoleContract systemRoles,
                         UserRepository users, ProfileService profiles, SkillService skills,
                         RoomService rooms, CompetitionService competitions,
                         SubmissionService submissions, VoteService votes, JudgeService judges,
                         RubricService rubrics, EvaluationTemplateVersionRepository rubricVersions,
                         ScoringService scoring, ProgressionService progression,
                         LeaderboardService leaderboards, ModerationService moderation) {
        this.registration = registration;
        this.systemRoles = systemRoles;
        this.users = users;
        this.profiles = profiles;
        this.skills = skills;
        this.rooms = rooms;
        this.competitions = competitions;
        this.submissions = submissions;
        this.votes = votes;
        this.judges = judges;
        this.rubrics = rubrics;
        this.rubricVersions = rubricVersions;
        this.scoring = scoring;
        this.progression = progression;
        this.leaderboards = leaderboards;
        this.moderation = moderation;
    }

    private void as(UUID userId, String... roles) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userId.toString(), null,
                        java.util.Arrays.stream(roles)
                                .map(r -> new SimpleGrantedAuthority("ROLE_" + r)).toList()));
    }

    private UUID user(String email) {
        return registration.findOrRegister("EMAIL", email).getId();
    }

    @Override
    public void run(ApplicationArguments args) {
        if (users.findByEmail(MARKER).isPresent()) {
            log.info("UAT seed: marker {} exists — already seeded, skipping", MARKER);
            return;
        }
        log.warn("==================== UAT SEED — START ====================");

        // ---------- users + roles ----------
        var uatUser = user("uat-user@starmitra.dev");
        var uatCreator = user("uat-creator@starmitra.dev");
        var uatJudge = user("uat-judge@starmitra.dev");
        var uatAdmin = user(MARKER);
        var uatSuper = user("uat-superadmin@starmitra.dev");
        var uatMember = user("uat-member@starmitra.dev");
        var uatVoter = user("uat-voter@starmitra.dev");
        var uatContestant2 = user("uat-contestant2@starmitra.dev");

        as(uatSuper, "SUPER_ADMIN");
        systemRoles.grantSystemRole(uatAdmin, "ADMIN");
        systemRoles.grantSystemRole(uatSuper, "SUPER_ADMIN");

        // ---------- profiles + skills (M02/M03) ----------
        var singing = skills.create("UAT Singing", "Vocal performance", null);
        var acting = skills.create("UAT Acting", "Stage and screen", null);
        var dancing = skills.create("UAT Dancing", "Choreography", null);
        profiles.updateMyProfile(uatCreator, "creator", new ProfileService.UpdateCommand(
                "UAT Creator", "Talent showcase profile", "IN", null, "PUBLIC"), null);
        profiles.updateMyProfile(uatContestant2, "c2", new ProfileService.UpdateCommand(
                "UAT Contestant Two", null, null, null, "PUBLIC"), null);
        skills.addMySkill(uatCreator, singing.id(), "ADVANCED");
        skills.addMySkill(uatContestant2, acting.id(), "INTERMEDIATE");

        // ---------- creative room (M07) — contribution role ≠ skill ----------
        var room = rooms.createRoom(uatCreator,
                new RoomService.RoomCommand("UAT Room - The Trio", "UAT demo project", "PUBLIC"));
        var inv = rooms.invite(uatCreator, room.id(), uatMember);
        rooms.respond(uatMember, inv.id(), "ACCEPT");
        rooms.assignRole(uatCreator, room.id(), uatMember, "Vocalist");

        // ---------- competition (M09 via ADMIN context) ----------
        as(uatAdmin, "ADMIN");
        var template = rubrics.createTemplate(uatAdmin, "UAT Rubric - Performance",
                List.of(new RubricService.CriterionCmd("Technique", "craft", new BigDecimal("60"), null),
                        new RubricService.CriterionCmd("Stage Presence", "delivery",
                                new BigDecimal("40"), null)),
                0, 10, null, null, null);
        var rvId = rubrics.publish(uatAdmin,
                rubricVersions.draftsOf(template.id()).get(0).getId()).id();

        var comp = competitions.create(uatCreator,
                new CompetitionService.CompetitionCommand(
                        "UAT Competition - Talent Showcase", "UAT demo competition"));
        var catSinging = competitions.createCategory(uatCreator, comp.id(),
                new CompetitionService.CategoryCommand("UAT Category - Singing", null,
                        List.of(singing.id())));
        var catActing = competitions.createCategory(uatCreator, comp.id(),
                new CompetitionService.CategoryCommand("UAT Category - Acting", null,
                        List.of(acting.id())));
        var r1 = competitions.createRound(uatCreator, comp.id(),
                new CompetitionService.RoundCommand(1, "UAT Round 1", null,
                        OffsetDateTime.now().plusDays(7), null, rvId, null));
        competitions.createRound(uatCreator, comp.id(),
                new CompetitionService.RoundCommand(2, "UAT Round 2", null,
                        OffsetDateTime.now().plusDays(14), null, rvId, null));

        // ---------- participants + submissions (M09→M10) ----------
        var pSolo = competitions.register(uatContestant2, comp.id(), "USER", catSinging.id(), null);
        var pTeam = competitions.register(uatCreator, comp.id(), "PROJECT", catActing.id(), room.id());
        var sSolo = submissions.create(uatContestant2, pSolo.id(), r1.id());
        var sTeam = submissions.create(uatCreator, pTeam.id(), r1.id());
        submissions.submit(uatContestant2, sSolo.id());
        submissions.finalize(uatContestant2, sSolo.id());
        submissions.submit(uatCreator, sTeam.id());
        submissions.finalize(uatCreator, sTeam.id());

        // ---------- votes (M11 — individual vs unsplit PROJECT) ----------
        votes.cast(uatVoter, sSolo.id(), "uat-v1");
        votes.cast(uatVoter, sTeam.id(), "uat-v2");
        votes.cast(uatUser, sSolo.id(), "uat-v3");

        // ---------- judge (M12) + evaluation (M13) ----------
        var judge = judges.createJudge(uatAdmin, uatJudge);
        judges.assign(uatAdmin, judge.id(),
                new JudgeService.AssignmentCmd(comp.id(), null, r1.id()));
        as(uatJudge, "JUDGE");
        var rubric = rubrics.publishedVersion(rvId).orElseThrow();
        rubrics.submitEvaluation(uatJudge, sSolo.id(), rvId, List.of(
                new RubricService.ScoreCmd(rubric.criteria().get(0).id(), new BigDecimal("9"), "solid craft"),
                new RubricService.ScoreCmd(rubric.criteria().get(1).id(), new BigDecimal("8"), "good presence")));
        rubrics.submitEvaluation(uatJudge, sTeam.id(), rvId, List.of(
                new RubricService.ScoreCmd(rubric.criteria().get(0).id(), new BigDecimal("7"), null),
                new RubricService.ScoreCmd(rubric.criteria().get(1).id(), new BigDecimal("7"), null)));

        // ---------- scoring + progression + leaderboard (M14/M15/M16, ADMIN) ----------
        as(uatAdmin, "ADMIN");
        scoring.createQualificationConfig(comp.id(), Map.of("type", "TOP_N", "n", 1));
        var cfg = scoring.createScoringConfig(uatAdmin, comp.id(),
                new BigDecimal("40"), new BigDecimal("60"));
        scoring.calculate(uatAdmin, comp.id(), r1.id(), cfg);
        scoring.finalize(uatAdmin, comp.id(), r1.id());
        var pcfg = progression.createConfig(comp.id(), Map.of("maxAdvance", 1));
        progression.activate(uatAdmin, comp.id(), r1.id());
        progression.calculate(uatAdmin, comp.id(), r1.id(), pcfg);
        leaderboards.publish(uatAdmin, comp.id(), catSinging.id(), r1.id(), "PUBLISH");

        // ---------- moderation case (M18) ----------
        var report = moderation.createReport(uatUser, "MEDIA",
                UUID.randomUUID(), "INAPPROPRIATE_CONTENT", "UAT demo report");

        SecurityContextHolder.clearContext();
        log.warn("==================== UAT SEED — DONE ====================");
        log.warn("competition={} round1={} report={}", comp.id(), r1.id(), report.id());
        log.warn("accounts: uat-user@ / uat-creator@ / uat-judge@ / uat-admin@ / uat-superadmin@starmitra.dev");
    }
}
