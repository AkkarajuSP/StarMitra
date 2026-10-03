package com.starmitra.modules.skill.application;

import com.starmitra.modules.skill.persistence.*;
import com.starmitra.platform.audit.AuditService;
import com.starmitra.platform.error.ApiException;
import com.starmitra.platform.error.ErrorCode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SkillServiceTest {

    private TalentSkillRepository skills;
    private SkillProficiencyRepository proficiencies;
    private UserTalentSkillRepository userSkills;
    private SkillService service;
    private final UUID me = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        skills = mock(TalentSkillRepository.class);
        proficiencies = mock(SkillProficiencyRepository.class);
        userSkills = mock(UserTalentSkillRepository.class);
        service = new SkillService(skills, proficiencies, userSkills, mock(AuditService.class));
    }

    @AfterEach
    void clearAuth() { SecurityContextHolder.clearContext(); }

    private void loginAs(String... roles) {
        var auth = new TestingAuthenticationToken("u", null,
                java.util.Arrays.stream(roles).map(r -> new SimpleGrantedAuthority("ROLE_" + r)).toList());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    void nonAdminCannotCreateSkill() {
        loginAs("USER");
        var e = assertThrows(ApiException.class, () -> service.create("Singer", null, null));
        assertEquals(ErrorCode.ROLE_REQUIRED, e.code());
        verify(skills, never()).save(any());
    }

    @Test
    void nonAdminCannotDeactivate() {
        loginAs("USER", "JUDGE");   // even JUDGE — talent skills are not permissions
        assertThrows(ApiException.class, () -> service.deactivate(UUID.randomUUID()));
    }

    @Test
    void adminCreatesSkill() {
        loginAs("ADMIN");
        when(skills.saveAndFlush(any())).thenAnswer(i -> i.getArgument(0));
        var v = service.create("Singer", "vocal", null);
        assertEquals("Singer", v.name());
        assertEquals("ACTIVE", v.status());
    }

    @Test
    void deactivateMarksInactiveNeverDeletes() {
        loginAs("ADMIN");
        var s = new TalentSkillEntity("Actor", null, null);
        when(skills.findById(s.getId())).thenReturn(Optional.of(s));
        service.deactivate(s.getId());
        assertEquals(TalentSkillEntity.Status.INACTIVE, s.getStatus());
        verify(skills, never()).delete(any());
        verify(skills).save(s);
    }

    @Test
    void addSkillRequiresActiveTaxonomyEntry() {
        loginAs("USER");
        UUID missing = UUID.randomUUID();
        when(skills.findById(missing)).thenReturn(Optional.empty());
        var e = assertThrows(ApiException.class, () -> service.addMySkill(me, missing, null));
        assertEquals(ErrorCode.NOT_FOUND, e.code());
    }

    @Test
    void cannotAddInactiveSkill() {
        loginAs("USER");
        var s = new TalentSkillEntity("Old", null, null);
        s.deactivate();
        when(skills.findById(s.getId())).thenReturn(Optional.of(s));
        assertThrows(ApiException.class, () -> service.addMySkill(me, s.getId(), null));
    }

    @Test
    void addSkillIsIdempotentUpsert() {
        loginAs("USER");
        var s = new TalentSkillEntity("Dance", null, null);
        when(skills.findById(s.getId())).thenReturn(Optional.of(s));
        var existing = new UserTalentSkillEntity(me, s.getId(), null);
        when(userSkills.findById(new UserTalentSkillEntity.Pk(me, s.getId())))
                .thenReturn(Optional.of(existing));
        var prof = mock(SkillProficiencyEntity.class);
        when(prof.getId()).thenReturn(UUID.randomUUID());
        when(proficiencies.findByCode("ADVANCED")).thenReturn(Optional.of(prof));

        var v = service.addMySkill(me, s.getId(), "ADVANCED");
        assertEquals("ADVANCED", v.proficiency());
        assertEquals(prof.getId(), existing.getProficiencyId());
        verify(userSkills).save(existing);          // update path, no insert attempt
    }

    @Test
    void unknownProficiencyRejected() {
        loginAs("USER");
        var s = new TalentSkillEntity("Dance", null, null);
        when(skills.findById(s.getId())).thenReturn(Optional.of(s));
        when(proficiencies.findByCode("GURU")).thenReturn(Optional.empty());
        var e = assertThrows(ApiException.class, () -> service.addMySkill(me, s.getId(), "GURU"));
        assertEquals(ErrorCode.VALIDATION_FAILED, e.code());
    }

    @Test
    void removeMissingSkillIsNoOp() {
        loginAs("USER");
        var skillId = UUID.randomUUID();
        when(userSkills.existsById(new UserTalentSkillEntity.Pk(me, skillId))).thenReturn(false);
        assertDoesNotThrow(() -> service.removeMySkill(me, skillId));
        verify(userSkills, never()).deleteById(any(UserTalentSkillEntity.Pk.class));
    }

    @Test
    void talentSkillNeverConsultsRolesForRead() {
        // no login at all — listActive is a service-level read (controller enforces auth)
        assertDoesNotThrow(() -> service.listActive(null, 10));
    }
}
