package com.starmitra.integration;

import com.starmitra.modules.identity.application.OtpService;
import com.starmitra.modules.identity.application.TestOtpSender;
import com.starmitra.modules.media.application.MediaService;
import com.starmitra.modules.media.application.ObjectStorageClient;
import com.starmitra.modules.portfolio.application.PortfolioService;
import com.starmitra.modules.skill.application.SkillService;
import com.starmitra.modules.social.application.SocialService;
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

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/** M08 on real PG17 — lazy-init, items, media/credit links, M21 target validation. */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class PortfolioFlowIT {

    @Autowired OtpService otpService;
    @Autowired PortfolioService portfolioService;
    @Autowired SkillService skillService;
    @Autowired MediaService mediaService;
    @Autowired ObjectStorageClient storage;
    @Autowired SocialService socialService;
    @Autowired JdbcTemplate jdbc;
    @Autowired jakarta.persistence.EntityManager em;

    private UUID me, other;

    private UUID register(String tag) {
        String email = tag + UUID.randomUUID().toString().substring(0, 6) + "@t.dev";
        otpService.request("EMAIL", email);
        return otpService.verify(email, TestOtpSender.lastOtpFor(email).orElseThrow());
    }

    @BeforeEach
    void setUp() {
        TestOtpSender.clear();
        SecurityContextHolder.clearContext();
        me = register("m08a");
        other = register("m08b");
    }

    @Test
    void portfolioLazilyCreatedOnePerUser() {
        var p1 = portfolioService.myPortfolio(me);
        var p2 = portfolioService.myPortfolio(me);
        assertEquals(p1.id(), p2.id());                        // same portfolio, not duplicated
        em.flush();
        assertEquals(1, jdbc.queryForObject(
                "select count(*) from portfolios where user_id=?", Integer.class, me));
    }

    @Test
    void titleUpdateWithEtag() {
        var p = portfolioService.myPortfolio(me);
        var updated = portfolioService.updateMyPortfolio(me, "My Reel", p.etag());
        assertEquals("My Reel", updated.title());
        assertThrows(ApiException.class,
                () -> portfolioService.updateMyPortfolio(me, "Stale", p.etag()));   // old etag → conflict
    }

    @Test
    void itemCrudAndOwnership() {
        var p = portfolioService.myPortfolio(me);
        var item = portfolioService.createItem(me,
                new PortfolioService.ItemCommand("Short film role", "lead", null, "PUBLIC"));
        em.flush();
        assertEquals(1, jdbc.queryForObject(
                "select count(*) from portfolio_items where portfolio_id=?", Integer.class, p.id()));

        var upd = portfolioService.updateItem(me, item.id(),
                new PortfolioService.ItemCommand("Renamed", null, null, "PRIVATE"), item.etag());
        assertEquals("Renamed", upd.title());
        assertEquals("PRIVATE", upd.visibility());

        // foreign user cannot touch it — IDOR safe
        assertThrows(ApiException.class, () -> portfolioService.updateItem(other, item.id(),
                new PortfolioService.ItemCommand("hijack", null, null, null), null));
        assertThrows(ApiException.class, () -> portfolioService.deleteItem(other, item.id()));
    }

    @Test
    void skillAndMediaRefsValidatedViaContracts() throws Exception {
        // bad skill → rejected
        assertThrows(ApiException.class, () -> portfolioService.createItem(me,
                new PortfolioService.ItemCommand("x", null, UUID.randomUUID(), null)));

        // real skill via M03
        SecurityContextHolder.getContext().setAuthentication(
                new TestingAuthenticationToken("a", null,
                        java.util.List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));
        var skillId = skillService.create("Cinematography", null, null).id();
        SecurityContextHolder.clearContext();
        var item = portfolioService.createItem(me,
                new PortfolioService.ItemCommand("DP reel", null, skillId, "PUBLIC"));
        assertEquals(skillId, item.skillId());

        // unverified media → link rejected
        var itemId = item.id();
        assertThrows(ApiException.class,
                () -> portfolioService.linkMedia(me, itemId, UUID.randomUUID(), null));

        // usable media via M04 → links
        var img = new BufferedImage(4, 4, BufferedImage.TYPE_INT_RGB);
        var baos = new ByteArrayOutputStream();
        ImageIO.write(img, "png", baos);
        var media = mediaService.create(me, new MediaService.CreateCommand(
                "IMAGE", "image/png", null, "still.png", "PUBLIC"));
        storage.writeObject(media.objectKey(), baos.toByteArray(), "image/png");
        mediaService.complete(me, media.id(), null, null);
        portfolioService.linkMedia(me, itemId, media.id(), 0);
        assertTrue(portfolioService.myPortfolio(me).items().get(0).mediaIds().contains(media.id()));
    }

    @Test
    void publicPortfolioShowsOnlyPublicItems() {
        portfolioService.createItem(me,
                new PortfolioService.ItemCommand("pub", null, null, "PUBLIC"));
        portfolioService.createItem(me,
                new PortfolioService.ItemCommand("priv", null, null, "PRIVATE"));
        var view = portfolioService.publicPortfolio(other, me);
        assertEquals(1, view.items().size());
        assertEquals("pub", view.items().get(0).title());
        assertThrows(ApiException.class, () -> portfolioService.publicPortfolio(other, UUID.randomUUID()));
    }

    @Test
    void creditLinkStoredNotManufactured() {
        var item = portfolioService.createItem(me,
                new PortfolioService.ItemCommand("film", null, null, "PUBLIC"));
        var creditId = UUID.randomUUID();                       // M07 ref — link stored as reference
        portfolioService.linkCredit(me, item.id(), creditId);
        em.flush();
        assertEquals(1, jdbc.queryForObject(
                "select count(*) from portfolio_item_contributions where item_id=?",
                Integer.class, item.id()));
        // duplicate link → CONFLICT (uq_pic_item_credit)
        assertThrows(ApiException.class, () -> portfolioService.linkCredit(me, item.id(), creditId));
    }

    @Test
    void m21PortfolioTargetValidationWorks() {
        var item = portfolioService.createItem(me,
                new PortfolioService.ItemCommand("reel", null, null, "PUBLIC"));
        var likeId = socialService.like(other, "PORTFOLIO", item.id());   // public → allowed
        assertNotNull(likeId);

        var priv = portfolioService.createItem(me,
                new PortfolioService.ItemCommand("hidden", null, null, "PRIVATE"));
        var e = assertThrows(ApiException.class,
                () -> socialService.like(other, "PORTFOLIO", priv.id()));
        assertEquals(ErrorCode.NOT_FOUND, e.code());           // private not engageable
    }
}
