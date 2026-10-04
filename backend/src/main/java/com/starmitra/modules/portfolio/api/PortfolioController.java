package com.starmitra.modules.portfolio.api;

import com.starmitra.modules.portfolio.application.PortfolioService;
import com.starmitra.platform.security.SecurityUtils;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/** /api/v1/portfolios — M08; owner always JWT sub. */
@RestController
@RequestMapping("/api/v1/portfolios")
public class PortfolioController {

    private final PortfolioService portfolios;

    public PortfolioController(PortfolioService portfolios) {
        this.portfolios = portfolios;
    }

    @GetMapping("/me")
    public ResponseEntity<PortfolioDtos.Portfolio> getMyPortfolio() {
        var p = portfolios.myPortfolio(SecurityUtils.currentUserId());
        return ResponseEntity.ok().eTag(p.etag()).body(toDto(p));
    }

    @PutMapping("/me")
    public ResponseEntity<PortfolioDtos.Portfolio> updateMyPortfolio(
            @RequestHeader(name = "If-Match", required = false) String ifMatch,
            @Valid @RequestBody PortfolioDtos.PortfolioUpdate body) {
        var p = portfolios.updateMyPortfolio(SecurityUtils.currentUserId(), body.title(), ifMatch);
        return ResponseEntity.ok().eTag(p.etag()).body(toDto(p));
    }

    @GetMapping("/me/items")
    public ResponseEntity<List<PortfolioDtos.PortfolioItem>> listPortfolioItems() {
        return ResponseEntity.ok(portfolios.myPortfolio(SecurityUtils.currentUserId()).items().stream()
                .map(this::toItemDto).toList());
    }

    @PostMapping("/me/items")
    public ResponseEntity<PortfolioDtos.PortfolioItem> createPortfolioItem(
            @Valid @RequestBody PortfolioDtos.PortfolioItemCreate body) {
        var i = portfolios.createItem(SecurityUtils.currentUserId(),
                new PortfolioService.ItemCommand(body.title(), body.description(), body.skillId(), body.visibility()));
        return ResponseEntity.status(HttpStatus.CREATED).eTag(i.etag()).body(toItemDto(i));
    }

    @PutMapping("/me/items/{itemId}")
    public ResponseEntity<PortfolioDtos.PortfolioItem> updatePortfolioItem(
            @PathVariable UUID itemId,
            @RequestHeader(name = "If-Match", required = false) String ifMatch,
            @Valid @RequestBody PortfolioDtos.PortfolioItemCreate body) {
        var i = portfolios.updateItem(SecurityUtils.currentUserId(), itemId,
                new PortfolioService.ItemCommand(body.title(), body.description(), body.skillId(), body.visibility()),
                ifMatch);
        return ResponseEntity.ok().eTag(i.etag()).body(toItemDto(i));
    }

    @DeleteMapping("/me/items/{itemId}")
    public ResponseEntity<Void> deletePortfolioItem(@PathVariable UUID itemId) {
        portfolios.deleteItem(SecurityUtils.currentUserId(), itemId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/me/items/{itemId}/media")
    public ResponseEntity<Void> linkPortfolioMedia(@PathVariable UUID itemId,
                                                   @Valid @RequestBody PortfolioDtos.AssetLink body) {
        portfolios.linkMedia(SecurityUtils.currentUserId(), itemId, body.mediaId(), body.sortOrder());
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PostMapping("/me/items/{itemId}/contributions")
    public ResponseEntity<Void> linkProjectCredit(@PathVariable UUID itemId,
                                                  @Valid @RequestBody PortfolioDtos.CreditLink body) {
        portfolios.linkCredit(SecurityUtils.currentUserId(), itemId, body.projectCreditId());
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @GetMapping("/{userId}")
    public ResponseEntity<PortfolioDtos.Portfolio> getPublicPortfolio(@PathVariable UUID userId) {
        var p = portfolios.publicPortfolio(SecurityUtils.currentUserId(), userId);
        return ResponseEntity.ok(toDto(p));
    }

    private PortfolioDtos.Portfolio toDto(PortfolioService.PortfolioView p) {
        return new PortfolioDtos.Portfolio(p.id(), p.userId(), p.title(), p.status(),
                p.items().stream().map(this::toItemDto).toList());
    }

    private PortfolioDtos.PortfolioItem toItemDto(PortfolioService.ItemView i) {
        return new PortfolioDtos.PortfolioItem(i.id(), i.title(), i.description(), i.skillId(),
                i.visibility(), i.mediaIds());
    }
}
