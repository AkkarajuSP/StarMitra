package com.starmitra.modules.pricing.api;

import com.starmitra.modules.pricing.application.PricingService;
import com.starmitra.platform.security.SecurityUtils;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * M22 — /api/v1/public/plans (readable by anyone) and /api/v1/me/plan
 * (the caller's own assignment — always JWT-derived, never from the body).
 * Paid-plan selection fails closed: 402 PAYMENT_NOT_ENABLED.
 */
@RestController
public class PricingController {

    private final PricingService pricing;

    public PricingController(PricingService pricing) {
        this.pricing = pricing;
    }

    @GetMapping("/api/v1/public/plans")
    public ResponseEntity<List<PricingDtos.Plan>> publicCatalog() {
        return ResponseEntity.ok(pricing.catalog().stream().map(this::toDto).toList());
    }

    @GetMapping("/api/v1/me/plan")
    public ResponseEntity<PricingDtos.MyPlan> myPlan() {
        return ResponseEntity.ok(toDto(pricing.myPlan(SecurityUtils.currentUserId())));
    }

    /** Select a plan. FREE activates; priced plans → 402 (no payment rail yet). */
    @PostMapping("/api/v1/me/plan")
    public ResponseEntity<PricingDtos.MyPlan> selectPlan(
            @Valid @RequestBody PricingDtos.PlanSelect body) {
        return ResponseEntity.ok(toDto(
                pricing.selectPlan(SecurityUtils.currentUserId(), body.planCode())));
    }

    private PricingDtos.Plan toDto(PricingService.PlanView p) {
        return new PricingDtos.Plan(p.id(), p.code(), p.displayName(), p.description(),
                p.planType(), p.price(), p.currency(), p.billingPeriod(), p.status(),
                p.sortOrder(), p.entitlements());
    }

    private PricingDtos.MyPlan toDto(PricingService.UserPlanView v) {
        return new PricingDtos.MyPlan(v.planCode(), v.displayName(), v.subscriptionStatus(),
                v.source(), v.startsAt(), v.endsAt(),
                v.entitlements().stream()
                        .map(e -> new PricingDtos.Entitlement(e.code(), e.kind(), e.value(),
                                e.limit(), e.remaining()))
                        .toList());
    }
}
