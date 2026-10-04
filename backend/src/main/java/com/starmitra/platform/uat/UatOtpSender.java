package com.starmitra.platform.uat;

import com.starmitra.modules.identity.application.OtpSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * UAT-only OTP delivery — no real email/SMS provider exists locally, so the
 * code is printed to the backend console inside an unmistakable UAT banner.
 * Active ONLY under the `uat` profile; never registered in prod/staging.
 */
@Component
@Profile("uat")
public class UatOtpSender implements OtpSender {

    private static final Logger log = LoggerFactory.getLogger(UatOtpSender.class);

    @Override
    public void send(String channel, String destination, String otp) {
        log.warn("======================== UAT OTP ========================");
        log.warn("channel={} destination={} otp={}", channel, destination, otp);
        log.warn("===========================================================");
    }
}
