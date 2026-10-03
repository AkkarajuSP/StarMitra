package com.starmitra.modules.identity.application;

import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/** Captures OTPs for integration tests — test profile only. */
@Component
@Primary
@Profile("test")
public class TestOtpSender implements OtpSender {

    private static final Map<String, String> LAST = new ConcurrentHashMap<>();

    @Override
    public void send(String channel, String destination, String otp) {
        LAST.put(destination, otp);
    }

    public static Optional<String> lastOtpFor(String destination) {
        return Optional.ofNullable(LAST.get(destination));
    }

    public static void clear() { LAST.clear(); }
}
