package com.starmitra.modules.pricing.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class PricingDtos {

    private PricingDtos() {}

    public record Plan(UUID id, String code, String displayName, String description,
                       String planType, BigDecimal price, String currency,
                       String billingPeriod, String status, int sortOrder,
                       Map<String, String> entitlements) {}

    public record Entitlement(String code, String kind, String value,
                              Long limit, Long remaining) {}

    public record MyPlan(String planCode, String displayName, String subscriptionStatus,
                         String source, OffsetDateTime startsAt, OffsetDateTime endsAt,
                         List<Entitlement> entitlements) {}

    public record PlanSelect(@NotBlank @Size(max = 40) String planCode) {}
}
