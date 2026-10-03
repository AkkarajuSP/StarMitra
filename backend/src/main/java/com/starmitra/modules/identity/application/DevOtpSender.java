package com.starmitra.modules.identity.application;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Local/test OTP sender — logs that an OTP was requested WITHOUT the value
 * (hook a console/mailtrap provider here in dev). Never logs the code itself.
 */
@Component
@Profile({"local", "test", "default"})
public class DevOtpSender implements OtpSender {

    private static final Logger log = LoggerFactory.getLogger(DevOtpSender.class);

    @Override
    public void send(String channel, String destination, String otp) {
        log.info("OTP requested: channel={} destination={} (code withheld from logs)", channel, destination);
    }
}
