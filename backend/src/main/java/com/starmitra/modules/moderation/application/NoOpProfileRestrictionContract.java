package com.starmitra.modules.moderation.application;

import org.springframework.stereotype.Component;

import java.util.UUID;

/** Default until M18 implements moderation restrictions — nothing restricted. */
@Component
public class NoOpProfileRestrictionContract implements ProfileRestrictionContract {

    @Override
    public boolean isRestricted(UUID userId) {
        return false;
    }
}
