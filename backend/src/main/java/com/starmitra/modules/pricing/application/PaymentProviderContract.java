package com.starmitra.modules.pricing.application;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * FUTURE extension point — intentionally UNIMPLEMENTED in MVP.
 *
 * When payment integration is enabled, a provider implementation
 * (e.g. Razorpay) will be bound here; paid-plan activation then flows:
 *
 *   Subscription → PaymentService → PaymentProviderContract → provider
 *
 * No checkout, webhook, refund, invoice, or card/UPI handling exists today.
 * Selecting a paid plan returns PAYMENT_NOT_ENABLED; nothing may mark a
 * subscription PAYMENT-sourced until a real provider is wired.
 */
public interface PaymentProviderContract {

    record CheckoutRequest(UUID userId, String planCode,
                           BigDecimal amount, String currency) {}

    /** Provider-issued handle the client uses to complete payment. */
    record CheckoutSession(String provider, String reference) {}

    String provider();

    /** Initiates a checkout for a paid plan. Not implemented in MVP. */
    CheckoutSession initiateCheckout(CheckoutRequest request);
}
