package com.starmitra.modules.identity.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import java.util.List;
import java.util.UUID;

/** API DTOs — never expose persistence entities. */
public final class AuthDtos {

    private AuthDtos() {}

    public record OtpRequest(
            @NotBlank String identifier,
            @NotBlank @Pattern(regexp = "EMAIL|SMS") String channel) {}

    public record OtpVerify(
            @NotBlank String identifier,
            @NotBlank String otp,
            String clientMsgId) {}

    public record AuthSession(
            String accessToken,
            long expiresIn,
            CurrentUserResponse user) {}

    public record CurrentUserResponse(
            UUID id,
            String email,
            String status,
            List<String> systemRoles) {}

    public record SessionInfo(
            UUID id,
            String createdAt,
            boolean current) {}
}
