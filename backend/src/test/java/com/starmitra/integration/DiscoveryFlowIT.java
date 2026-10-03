package com.starmitra.integration;

import com.starmitra.modules.discovery.application.DiscoveryService;
import com.starmitra.modules.identity.application.OtpService;
import com.starmitra.modules.identity.application.TestOtpSender;
import com.starmitra.modules.media.application.MediaService;
import com.starmitra.modules.media.application.ObjectStorageClient;
import com.starmitra.modules.profile.application.ProfileService;
import com.starmitra.modules.skill.application.SkillService;
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

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/** M05 on real PG17 — FTS ranking, visibility at query time, deterministic feed. */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class DiscoveryFlowIT {

    @Autowired OtpService otpService;
    @Autowired ProfileService profileService;
    @Autowired SkillService skillService;
    @Autowired MediaService mediaService;
    @Autowired ObjectStorageClient storage;
    @Autowired DiscoveryService discovery;
    @Autowired JdbcTemplate jdbc;

    private UUID viewer;
    private UUID publicUser, privateUser, skilledUser;
    private UUID skillId;

    private UUID user(String name, String visibility, String bio) {
        String email = "d" + UUID.randomUUID().toString().substring(0, 6) + "@t.dev";
        otpService.request("EMAIL", email);
        UUID id = otpService.verify(email, TestOtpSender.lastOtpFor(email).orElseThrow());
        profileService.updateMyProfile(id, "x",
                new ProfileService.UpdateCommand(name, bio, "Hyd", null, visibility), null);
        return id;
    }

    @BeforeEach
    void seed() throws Exception {
        TestOtpSender.clear();
        SecurityContextHolder.clearContext();
        viewer = user("viewer", "PUBLIC", null);
        publicUser = user("Ravi Singer", "PUBLIC", "playback singer");
        privateUser = user("Hidden One", "PRIVATE", "secret artist");
        skilledUser = user("Dance Pro", "PUBLIC", "choreographer");

        // admin creates taxonomy + user associates
        SecurityContextHolder.getContext().setAuthentication(
                new TestingAuthenticationToken("a", null,
                        java.util.List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));
        skillId = skillService.create("Playback Singing", null, null).id();
        SecurityContextHolder.clearContext();
        skillService.addMySkill(skilledUser, skillId, "ADVANCED");

        // public processed media for feed
        var a = mediaService.create(publicUser, new MediaService.CreateCommand(
                "DOCUMENT", "application/pdf", null, "portfolio-reel.pdf", "PUBLIC"));
        storage.writeObject(a.objectKey(), "bytes".getBytes(), "application/pdf");
        mediaService.complete(publicUser, a.id(), null, null);
    }

    @Test
    void ftsFindsPublicProfileByNameAndBio() {
        var r = discovery.search(viewer, "Ravi", "PROFILE", null, 10);
        assertTrue(r.items().stream().anyMatch(i -> i.id().equals(publicUser)));
        var bioHit = discovery.search(viewer, "playback", "PROFILE", null, 10);
        assertTrue(bioHit.items().stream().anyMatch(i -> i.id().equals(publicUser)));
    }

    @Test
    void privateProfileNeverLeakThroughSearch() {
        var r = discovery.search(viewer, "Hidden", "PROFILE", null, 10);
        assertFalse(r.items().stream().anyMatch(i -> i.id().equals(privateUser)));
        // indistinguishable from nonexistent
        var ghost = discovery.search(viewer, "NoSuchUserXYZ", "PROFILE", null, 10);
        assertEquals(r.items().size(), ghost.items().stream().filter(i -> i.fields().get("displayName").equals("Hidden One")).count());
    }

    @Test
    void skillSearchFindsActiveTaxonomy() {
        var r = discovery.search(viewer, "singing", "SKILL", null, 10);
        assertTrue(r.items().stream().anyMatch(i -> i.id().equals(skillId)));
    }

    @Test
    void discoveryBrowsesBySkill() {
        var r = discovery.discover(viewer, skillId, null, 10);
        assertEquals(1, r.items().size());
        assertEquals(skilledUser, r.items().get(0).id());
    }

    @Test
    void discoveryWithoutSkillListsPublicOnly() {
        var r = discovery.discover(viewer, null, null, 50);
        assertTrue(r.items().stream().anyMatch(i -> i.id().equals(publicUser)));
        assertFalse(r.items().stream().anyMatch(i -> i.id().equals(privateUser)));
    }

    @Test
    void feedIsDeterministicAndVisibilityFiltered() {
        var f1 = discovery.feed(viewer, null, 10);
        var f2 = discovery.feed(viewer, null, 10);
        assertEquals(f1.items().stream().map(i -> i.id()).toList(),
                f2.items().stream().map(i -> i.id()).toList());
        assertFalse(f1.items().isEmpty());
        assertTrue(f1.items().stream().allMatch(i -> "MEDIA".equals(i.type())));
    }

    @Test
    void invalidMediaTypeSearchIsFiltered() {
        var r = discovery.search(viewer, "pdf", "MEDIA", null, 10);
        assertTrue(r.items().stream().allMatch(i -> "MEDIA".equals(i.type())));
    }
}
