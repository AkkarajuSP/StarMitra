package com.starmitra.modules.identity.api;

import com.starmitra.modules.identity.application.AuthService;
import com.starmitra.modules.identity.application.OtpService;
import com.starmitra.modules.identity.application.SessionService;
import com.starmitra.platform.security.SecurityUtils;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * /api/v1/auth — OpenAPI operations: requestOtp, verifyOtp, refreshSession,
 * logout, listSessions, revokeSession, getMe.
 * Web gets the access token via httpOnly cookie; mobile via response body Bearer.
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final OtpService otpService;
    private final AuthService authService;
    private final SessionService sessionService;
    private final String cookieName;
    private final boolean cookieSecure;
    private final String sameSite;

    public AuthController(OtpService otpService, AuthService authService, SessionService sessionService,
                          @Value("${app.auth.cookie.name}") String cookieName,
                          @Value("${app.auth.cookie.secure}") boolean cookieSecure,
                          @Value("${app.auth.cookie.same-site}") String sameSite) {
        this.otpService = otpService;
        this.authService = authService;
        this.sessionService = sessionService;
        this.cookieName = cookieName;
        this.cookieSecure = cookieSecure;
        this.sameSite = sameSite;
    }

    @PostMapping("/otp/request")
    public ResponseEntity<Void> requestOtp(@Valid @RequestBody AuthDtos.OtpRequest body) {
        otpService.request(body.channel(), body.identifier());   // enumeration-safe: always 202
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/otp/verify")
    public ResponseEntity<AuthDtos.AuthSession> verifyOtp(@Valid @RequestBody AuthDtos.OtpVerify body,
                                                        HttpServletResponse response) {
        AuthService.Session s = authService.completeOtpLogin(body.identifier(), body.otp());
        issueCookies(response, s, true);
        return ResponseEntity.ok(toDto(s));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthDtos.AuthSession> refresh(@CookieValue(name = "sm_refresh", required = false) String cookieToken,
                                                        @RequestHeader(name = "X-Refresh-Token", required = false) String headerToken,
                                                        HttpServletResponse response) {
        String token = headerToken != null ? headerToken : cookieToken;
        AuthService.Session s = authService.refresh(token);
        issueCookies(response, s, true);
        return ResponseEntity.ok(toDto(s));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@CookieValue(name = "sm_refresh", required = false) String token,
                                       HttpServletResponse response) {
        UUID actor = SecurityUtils.currentUserId();
        if (token != null) authService.logout(token, actor);
        expireCookie(response, cookieName);
        expireCookie(response, "sm_refresh");
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/sessions")
    public ResponseEntity<List<AuthDtos.SessionInfo>> listSessions(
            @CookieValue(name = "sm_refresh", required = false) String cookieToken,
            @RequestHeader(name = "X-Refresh-Token", required = false) String headerToken) {
        String presented = headerToken != null ? headerToken : cookieToken;
        var sessions = sessionService.listForUser(SecurityUtils.currentUserId(), presented).stream()
                .map(s -> new AuthDtos.SessionInfo(s.id(), s.createdAt(), s.current()))
                .toList();
        return ResponseEntity.ok(sessions);
    }

    @DeleteMapping("/sessions/{sessionId}")
    public ResponseEntity<Void> revokeSession(@PathVariable UUID sessionId) {
        sessionService.revokeSession(SecurityUtils.currentUserId(), sessionId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    public ResponseEntity<AuthDtos.CurrentUserResponse> getMe() {
        var v = authService.identityOf(SecurityUtils.currentUserId());
        return ResponseEntity.ok(new AuthDtos.CurrentUserResponse(v.id(), v.email(), v.status(), v.systemRoles()));
    }

    private void issueCookies(HttpServletResponse response, AuthService.Session s, boolean includeRefresh) {
        response.addHeader(HttpHeaders.SET_COOKIE, cookie(cookieName, s.accessToken(),
                Duration.ofSeconds(s.expiresInSeconds())).toString());
        if (includeRefresh) {
            response.addHeader(HttpHeaders.SET_COOKIE, cookie("sm_refresh", s.refreshToken(),
                    Duration.ofDays(30)).toString());
        }
    }

    private ResponseCookie cookie(String name, String value, Duration maxAge) {
        return ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite(sameSite)
                .path("/")
                .maxAge(maxAge)
                .build();
    }

    private void expireCookie(HttpServletResponse response, String name) {
        response.addHeader(HttpHeaders.SET_COOKIE, cookie(name, "", Duration.ZERO).toString());
    }

    private AuthDtos.AuthSession toDto(AuthService.Session s) {
        var v = authService.identityOf(s.userId());
        return new AuthDtos.AuthSession(s.accessToken(), s.expiresInSeconds(),
                new AuthDtos.CurrentUserResponse(v.id(), v.email(), v.status(), v.systemRoles()));
    }
}
