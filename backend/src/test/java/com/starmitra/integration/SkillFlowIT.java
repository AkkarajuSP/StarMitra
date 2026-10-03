package com.starmitra.integration;

import com.starmitra.modules.identity.application.OtpService;
import com.starmitra.modules.identity.application.TestOtpSender;
import com.starmitra.modules.profile.application.ProfileService;
import com.starmitra.modules.skill.application.SkillService;
import com.starmitra.modules.skill.application.UserSkillReadContract;
import com.starmitra.platform.error.ApiException;
import com.starmitra.platform.error.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/** M03 slice on real PG17 — taxonomy, associations, constraints, M02 contract. */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class SkillFlowIT {

    @Autowired OtpService otpService;
    @Autowired SkillService skillService;
    @Autowired UserSkillReadContract skillReadContract;
    @Autowired ProfileService profileService;
    @Autowired JdbcTemplate jdbc;

    private UUID me;

    @BeforeEach
    void register() {
        TestOtpSender.clear();
        SecurityContextHolder.clearContext();
        String email = "m03+" + UUID.randomUUID().toString().substring(0, 8) + "@test.dev";
        String otp = TestOtpSender.lastOtpFor(email).orElseGet(() -> {
            otpService.request("EMAIL", email);
            return TestOtpSender.lastOtpFor(email).orElseThrow();
        });
        me = otpService.verify(email, otp);
    }

    private void loginAs(String... roles) {
        var auth = new TestingAuthenticationToken(me.toString(), null,
                java.util.Arrays.stream(roles).map(r -> new SimpleGrantedAuthority("ROLE_" + r)).toList());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    private UUID createSkill(String name) {
        loginAs("ADMIN");
        return skillService.create(name, null, null).id();
    }

    @Test
    void taxonomyCreateListDeactivate() {
        var id = createSkill("Singer");
        var id2 = createSkill("Actor");
        var page = skillService.listActive(null, 10);
        assertTrue(page.items().stream().anyMatch(s -> s.id().equals(id)));

        skillService.deactivate(id2);
        var after = skillService.listActive(null, 10);
        assertFalse(after.items().stream().anyMatch(s -> s.id().equals(id2)));
        // row still exists — lifecycle, not deletion
        assertEquals("INACTIVE", jdbc.queryForObject(
                "select status from talent_skills where id=?", String.class, id2));
    }

    @Test
    void duplicateSkillNameRejected() {
        createSkill("Director");
        var e = assertThrows(ApiException.class, () -> createSkill("Director"));
        assertEquals(ErrorCode.CONFLICT, e.code());
    }

    @Test
    void multiTalentAssociationsAndProfileRead() {
        var singer = createSkill("Singer");
        var actor = createSkill("Actor");
        var director = createSkill("Director");
        loginAs("USER");

        skillService.addMySkill(me, singer, "PROFESSIONAL");
        skillService.addMySkill(me, actor, "ADVANCED");
        skillService.addMySkill(me, director, null);      // proficiency optional

        var mine = skillService.mySkills(me);
        assertEquals(3, mine.size());   // multi-talent, no cap, no forced primary

        // M02 contract consumes real data now
        List<UserSkillReadContract.SkillView> viaContract = skillReadContract.skillsOf(me);
        assertEquals(3, viaContract.size());
        assertTrue(viaContract.stream().anyMatch(s -> "PROFESSIONAL".equals(s.proficiency())));

        // M02 profile exposes skills through the contract
        var profile = profileService.myProfile(me, "m03user");
        assertEquals(3, profile.skills().size());
    }

    @Test
    void addSkillIdempotentRepeat() {
        var skill = createSkill("Writer");
        loginAs("USER");
        skillService.addMySkill(me, skill, "BEGINNER");
        skillService.addMySkill(me, skill, "ADVANCED");   // repeat → update, no dup
        Integer count = jdbc.queryForObject(
                "select count(*) from user_talent_skills where user_id=? and skill_id=?",
                Integer.class, me, skill);
        assertEquals(1, count);
        assertEquals("ADVANCED", skillService.mySkills(me).get(0).proficiency());
    }

    @Test
    void cannotAssociateInactiveOrMissingSkill() {
        var skill = createSkill("Legacy");
        skillService.deactivate(skill);
        loginAs("USER");
        assertThrows(ApiException.class, () -> skillService.addMySkill(me, skill, null));
        assertThrows(ApiException.class, () -> skillService.addMySkill(me, UUID.randomUUID(), null));
    }

    @Test
    void removeSkillIsIdempotent() {
        var skill = createSkill("Editor");
        loginAs("USER");
        skillService.addMySkill(me, skill, null);
        skillService.removeMySkill(me, skill);
        skillService.removeMySkill(me, skill);   // repeat — no error
        assertEquals(0, jdbc.queryForObject(
                "select count(*) from user_talent_skills where user_id=?", Integer.class, me));
    }

    @Test
    void fkIntegrityAndProficiencySeeds() {
        assertEquals(4, jdbc.queryForObject("select count(*) from skill_proficiencies", Integer.class));
        var skill = createSkill("DP");
        loginAs("USER");
        // unknown proficiency → validation error, not FK crash
        assertThrows(ApiException.class, () -> skillService.addMySkill(me, skill, "NONEXISTENT"));
    }
}
