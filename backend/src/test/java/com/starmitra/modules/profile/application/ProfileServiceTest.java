package com.starmitra.modules.profile.application;

import com.starmitra.modules.moderation.application.ProfileRestrictionContract;
import com.starmitra.modules.profile.persistence.UserProfileEntity;
import com.starmitra.modules.profile.persistence.UserProfileRepository;
import com.starmitra.modules.skill.application.UserSkillReadContract;
import com.starmitra.platform.audit.AuditService;
import com.starmitra.platform.error.ApiException;
import com.starmitra.platform.error.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ProfileServiceTest {

    private UserProfileRepository profiles;
    private ProfileRestrictionContract restriction;
    private ProfileService service;
    private final UUID me = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        profiles = mock(UserProfileRepository.class);
        restriction = mock(ProfileRestrictionContract.class);
        service = new ProfileService(profiles,
                (userId) -> List.of(new UserSkillReadContract.SkillView(UUID.randomUUID(), "Dance", "ADVANCED")),
                restriction, new com.starmitra.modules.media.application.MediaReferenceContract() {
                    public boolean isUsableBy(UUID m, UUID o) { return true; }
                    public boolean isDeliverableTo(UUID m, UUID c) { return true; }
                    public java.util.Optional<UUID> ownerOf(UUID m) { return java.util.Optional.empty(); }
                }, mock(AuditService.class));
    }

    private UserProfileEntity publicProfile(UUID userId) {
        var p = new UserProfileEntity(userId, "star-user");
        p.applyUpdate(null, null, null, null, UserProfileEntity.Visibility.PUBLIC);
        return p;
    }

    @Test
    void lazilyCreatesOwnProfileOnFirstRead() {
        when(profiles.findByUserId(me)).thenReturn(Optional.empty());
        when(profiles.saveAndFlush(any())).thenAnswer(i -> i.getArgument(0));
        var v = service.myProfile(me, "me@test.dev");
        assertEquals(me, v.userId());
        verify(profiles).saveAndFlush(argThat(p -> p.getVisibilityState() == UserProfileEntity.Visibility.PRIVATE));
    }

    @Test
    void publicProfileVisibleWhenPublic() {
        var target = publicProfile(UUID.randomUUID());
        when(profiles.findByUserId(target.getUserId())).thenReturn(Optional.of(target));
        var v = service.publicProfile(me, target.getUserId());
        assertEquals("star-user", v.displayName());
        assertEquals(1, v.skills().size());
    }

    @Test
    void privateProfileIsEnumerationSafeNotFound() {
        var target = new UserProfileEntity(UUID.randomUUID(), "hidden");   // default PRIVATE
        when(profiles.findByUserId(target.getUserId())).thenReturn(Optional.of(target));
        var e = assertThrows(ApiException.class, () -> service.publicProfile(me, target.getUserId()));
        assertEquals(ErrorCode.NOT_FOUND, e.code());
    }

    @Test
    void restrictedProfileIsAlsoNotFound() {
        var target = publicProfile(UUID.randomUUID());
        when(restriction.isRestricted(target.getUserId())).thenReturn(true);
        when(profiles.findByUserId(target.getUserId())).thenReturn(Optional.of(target));
        var e = assertThrows(ApiException.class, () -> service.publicProfile(me, target.getUserId()));
        assertEquals(ErrorCode.NOT_FOUND, e.code());   // restricted indistinguishable from missing
    }

    @Test
    void ownerSeesOwnPrivateProfile() {
        var p = new UserProfileEntity(me, "mine");
        when(profiles.findByUserId(me)).thenReturn(Optional.of(p));
        var v = service.publicProfile(me, me);
        assertEquals("mine", v.displayName());
    }

    @Test
    void staleEtagRejectsUpdate() {
        var p = new UserProfileEntity(me, "mine");
        when(profiles.findByUserId(me)).thenReturn(Optional.of(p));
        var cmd = new ProfileService.UpdateCommand("new-name", null, null, null, null);
        var e = assertThrows(ApiException.class,
                () -> service.updateMyProfile(me, "x", cmd, "\"0000-0\""));
        assertEquals(ErrorCode.CONFLICT_VERSION, e.code());
        verify(profiles, never()).saveAndFlush(any());
    }

    @Test
    void matchingEtagAllowsUpdateAndAudits() {
        var p = new UserProfileEntity(me, "mine");
        when(profiles.findByUserId(me)).thenReturn(Optional.of(p));
        when(profiles.saveAndFlush(any())).thenAnswer(i -> i.getArgument(0));
        var cmd = new ProfileService.UpdateCommand("new-name", null, null, null, "PUBLIC");
        var v = service.updateMyProfile(me, "x", cmd, p.etag());
        assertEquals("new-name", v.displayName());
        assertEquals("PUBLIC", v.visibilityState());
    }

    @Test
    void updateWithoutEtagSucceeds() {
        var p = new UserProfileEntity(me, "mine");
        when(profiles.findByUserId(me)).thenReturn(Optional.of(p));
        when(profiles.saveAndFlush(any())).thenAnswer(i -> i.getArgument(0));
        var v = service.updateMyProfile(me, "x",
                new ProfileService.UpdateCommand("n", null, null, null, null), null);
        assertEquals("n", v.displayName());
    }

    @Test
    void invalidVisibilityRejected() {
        var p = new UserProfileEntity(me, "mine");
        when(profiles.findByUserId(me)).thenReturn(Optional.of(p));
        assertThrows(IllegalArgumentException.class, () -> service.updateMyProfile(me, "x",
                new ProfileService.UpdateCommand(null, null, null, null, "EVERYONE"), null));
    }
}
