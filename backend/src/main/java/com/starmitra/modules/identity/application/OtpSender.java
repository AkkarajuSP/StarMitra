package com.starmitra.modules.identity.application;

/**
 * Provider-neutral OTP delivery (ADR: provider-neutral, configurable).
 * Implementations: email/SMS provider adapters. Raw OTP crosses this boundary
 * only for delivery — it is never persisted or logged.
 */
public interface OtpSender {

    void send(String channel, String destination, String otp);
}
