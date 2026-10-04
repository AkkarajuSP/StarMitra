package com.starmitra.modules.portfolio.application;

import com.starmitra.modules.media.application.MediaReferenceContract;
import com.starmitra.modules.portfolio.persistence.*;
import com.starmitra.modules.skill.application.SkillTaxonomyContract;
import com.starmitra.platform.audit.AuditService;
import com.starmitra.platform.error.ApiException;
import com.starmitra.platform.error.ErrorCode;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * M08 — Portfolio. One portfolio per user (DB-04, lazy-init like M02).
 * Cross-module refs by UUID only (no FK by design):
 *   skill_id           → M03 via SkillTaxonomyContract (must be ACTIVE)
 *   media_id           → M04 via MediaReferenceContract (must be usable + owned)
 *   project_credit_id  → M07 — verification seam (M07 not implemented; the
 *                        LINK is stored, contribution is never manufactured)
 * Credits ≠ talent-skill copies: contributions link verified M07
 * ProjectCredit records; the optional skill_id is a display tag only.
 */
@Service
public class PortfolioService implements PortfolioTargetContract {

    private final PortfolioRepository portfolios;
    private final PortfolioItemRepository items;
    private final PortfolioItemMediaRepository itemMedia;
    private final PortfolioItemContributionRepository contributions;
    private final SkillTaxonomyContract skills;
    private final MediaReferenceContract media;
    private final AuditService audit;

    public PortfolioService(PortfolioRepository portfolios, PortfolioItemRepository items,
                            PortfolioItemMediaRepository itemMedia,
                            PortfolioItemContributionRepository contributions,
                            SkillTaxonomyContract skills, MediaReferenceContract media,
                            AuditService audit) {
        this.portfolios = portfolios;
        this.items = items;
        this.itemMedia = itemMedia;
        this.contributions = contributions;
        this.skills = skills;
        this.media = media;
        this.audit = audit;
    }

    public record ItemView(UUID id, String title, String description, UUID skillId,
                           String visibility, List<UUID> mediaIds, String etag) {}
    public record PortfolioView(UUID id, UUID userId, String title, String status,
                                List<ItemView> items, String etag) {}
    public record ItemCommand(String title, String description, UUID skillId, String visibility) {}

    // ---------- portfolio ----------

    /** Lazy-init (no POST in contract); uq_portfolios_user converges races. */
    private PortfolioEntity ownPortfolio(UUID userId) {
        return portfolios.findByUserId(userId).orElseGet(() -> {
            try {
                return portfolios.saveAndFlush(new PortfolioEntity(userId));
            } catch (DataIntegrityViolationException e) {
                return portfolios.findByUserId(userId).orElseThrow();
            }
        });
    }

    @Transactional
    public PortfolioView myPortfolio(UUID userId) {
        return toView(ownPortfolio(userId), items(userId), true);
    }

    @Transactional
    public PortfolioView updateMyPortfolio(UUID userId, String title, String ifMatch) {
        var p = ownPortfolio(userId);
        assertCurrent(ifMatch, p.etag());
        p.updateTitle(title);
        return toView(portfolios.saveAndFlush(p), items(userId), true);
    }

    @Transactional(readOnly = true)
    public PortfolioView publicPortfolio(UUID viewer, UUID ownerId) {
        var p = portfolios.findByUserId(ownerId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND));
        var pubItems = items(ownerId).stream()
                .filter(i -> i.getVisibility() == PortfolioItemEntity.Visibility.PUBLIC)
                .toList();
        return toView(p, pubItems, false);
    }

    // ---------- items ----------

    @Transactional
    public ItemView createItem(UUID userId, ItemCommand cmd) {
        var p = ownPortfolio(userId);
        var visibility = parseVisibility(cmd.visibility());
        if (cmd.skillId() != null && !skills.isActiveSkill(cmd.skillId())) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED, "Skill not found or inactive");
        }
        var item = items.saveAndFlush(new PortfolioItemEntity(p.getId(), cmd.title(),
                cmd.description(), cmd.skillId(), visibility));
        audit.record("M08", "PORTFOLIO_ITEM_CREATED", userId, "user",
                "portfolio_item", item.getId().toString(), null);
        return toItem(item);
    }

    @Transactional
    public ItemView updateItem(UUID userId, UUID itemId, ItemCommand cmd, String ifMatch) {
        var item = ownItem(userId, itemId);
        assertCurrent(ifMatch, item.etag());
        if (cmd.skillId() != null && !skills.isActiveSkill(cmd.skillId())) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED, "Skill not found or inactive");
        }
        item.update(cmd.title(), cmd.description(), cmd.skillId(), parseVisibility(cmd.visibility()));
        return toItem(items.saveAndFlush(item));
    }

    @Transactional
    public void deleteItem(UUID userId, UUID itemId) {
        var item = ownItem(userId, itemId);
        items.delete(item);
        audit.record("M08", "PORTFOLIO_ITEM_DELETED", userId, "user",
                "portfolio_item", itemId.toString(), null);
    }

    // ---------- media links ----------

    /** linkPortfolioMedia — media must be usable by the portfolio owner (M04 contract). */
    @Transactional
    public void linkMedia(UUID userId, UUID itemId, UUID mediaId, Integer sortOrder) {
        var item = ownItem(userId, itemId);
        if (!media.isUsableBy(mediaId, userId)) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED, "Media not usable");
        }
        var key = new PortfolioItemMediaEntity.Pk(itemId, mediaId);
        if (!itemMedia.existsById(key)) {
            try {
                itemMedia.saveAndFlush(new PortfolioItemMediaEntity(item.getId(), mediaId, sortOrder));
            } catch (DataIntegrityViolationException dup) { /* concurrent — already linked */ }
        }
    }

    // ---------- credit links ----------

    /**
     * linkProjectCredit — stores the M07 credit reference. M07 does not exist
     * yet, so verification is impossible; the seam is the contract — M08
     * never fabricates contribution data.
     */
    @Transactional
    public void linkCredit(UUID userId, UUID itemId, UUID projectCreditId) {
        ownItem(userId, itemId);
        try {
            contributions.saveAndFlush(new PortfolioItemContributionEntity(itemId, projectCreditId));
        } catch (DataIntegrityViolationException dup) {
            throw new ApiException(ErrorCode.CONFLICT, "Credit already linked");
        }
        audit.record("M08", "PROJECT_CREDIT_LINKED", userId, "user",
                "portfolio_item", itemId.toString(), null);
    }

    // ---------- M21 target contract ----------

    @Override
    @Transactional(readOnly = true)
    public boolean isEngageableItem(UUID itemId, UUID callerUserId) {
        return items.findById(itemId)
                .filter(i -> "ACTIVE".equals(i.getStatus()))
                .map(i -> i.getVisibility() == PortfolioItemEntity.Visibility.PUBLIC
                        || portfolios.findById(i.getPortfolioId())
                            .map(p -> p.getUserId().equals(callerUserId)).orElse(false))
                .orElse(false);
    }

    // ---------- internals ----------

    private List<PortfolioItemEntity> items(UUID userId) {
        return portfolios.findByUserId(userId)
                .map(p -> items.findByPortfolioIdOrderBySortOrderAscCreatedAtAsc(p.getId()))
                .orElse(List.of());
    }

    private PortfolioItemEntity ownItem(UUID userId, UUID itemId) {
        var item = items.findById(itemId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND));
        var owner = portfolios.findById(item.getPortfolioId())
                .map(PortfolioEntity::getUserId).orElse(null);
        if (!userId.equals(owner)) {
            throw new ApiException(ErrorCode.NOT_FOUND);           // IDOR-safe
        }
        return item;
    }

    private void assertCurrent(String ifMatch, String etag) {
        if (ifMatch != null && !ifMatch.equals(etag)) {
            throw new ApiException(ErrorCode.CONFLICT_VERSION, "Stale version");
        }
    }

    private PortfolioItemEntity.Visibility parseVisibility(String v) {
        if (v == null) return PortfolioItemEntity.Visibility.PUBLIC;
        try {
            return PortfolioItemEntity.Visibility.valueOf(v);
        } catch (IllegalArgumentException e) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED, "Invalid visibility");
        }
    }

    private ItemView toItem(PortfolioItemEntity i) {
        return new ItemView(i.getId(), i.getTitle(), i.getDescription(), i.getSkillId(),
                i.getVisibility().name(), items.findMediaIds(i.getId()), i.etag());
    }

    private PortfolioView toView(PortfolioEntity p, List<PortfolioItemEntity> its, boolean includePrivate) {
        var views = its.stream()
                .filter(i -> includePrivate || i.getVisibility() == PortfolioItemEntity.Visibility.PUBLIC)
                .map(this::toItem).toList();
        return new PortfolioView(p.getId(), p.getUserId(), p.getTitle(), p.getStatus(), views, p.etag());
    }
}
