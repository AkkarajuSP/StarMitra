package com.starmitra.modules.identity.application;

import com.starmitra.modules.identity.persistence.UserRepository;
import com.starmitra.platform.error.ApiException;
import com.starmitra.platform.error.ErrorCode;
import com.starmitra.platform.security.JwtTokenService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Session orchestration: OTP verify → issue access JWT + opaque refresh
 * (new family), rotation, logout/revocation.
 */
@Service
public class AuthService {

    private final OtpService otpService;
    private final RefreshTokenService refreshTokens;
    private final JwtTokenService jwt;
    private final UserRepository users;

    public AuthService(OtpService otpService, RefreshTokenService refreshTokens,
                       JwtTokenService jwt, UserRepository users) {
        this.otpService = otpService;
        this.refreshTokens = refreshTokens;
        this.jwt = jwt;
        this.users = users;
    }

    public record Session(UUID userId, String accessToken, long expiresInSeconds, String refreshToken) {}
    /** Application-layer identity view — api layer never sees UserEntity. */
    public record IdentityView(UUID id, String email, String status, List<String> systemRoles) {}

    /** Verified OTP → new session family. */
    @Transactional
    public Session completeOtpLogin(String identifier, String otp) {
        UUID userId = otpService.verify(identifier, otp);
        return newSession(userId);
    }

    /** Rotate: presented refresh → new pair in the same family. */
    @Transactional
    public Session refresh(String presentedRefreshToken) {
        RefreshTokenService.IssuedToken rotated = refreshTokens.rotate(presentedRefreshToken);
        List<String> roles = users.findRoleNamesByUserId(rotated.userId());
        return new Session(rotated.userId(), jwt.issueAccessToken(rotated.userId(), roles),
                jwt.ttl().toSeconds(), rotated.raw());
    }

    @Transactional
    public void logout(String presentedRefreshToken, UUID actorId) {
        refreshTokens.revoke(presentedRefreshToken, actorId);
    }

    @Transactional(readOnly = true)
    public IdentityView identityOf(UUID userId) {
        var user = users.findById(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "User not found"));
        return new IdentityView(user.getId(), user.getEmail(), user.getStatus().name(),
                users.findRoleNamesByUserId(userId));
    }

    private Session newSession(UUID userId) {
        List<String> roles = users.findRoleNamesByUserId(userId);
        RefreshTokenService.IssuedToken refresh = refreshTokens.issue(userId, UUID.randomUUID());
        return new Session(userId, jwt.issueAccessToken(userId, roles), jwt.ttl().toSeconds(), refresh.raw());
    }
}
