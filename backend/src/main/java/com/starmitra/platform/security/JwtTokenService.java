package com.starmitra.platform.security;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Service;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Issues short-lived JWT access tokens (ADR-006). Carries SystemRole claims
 * ONLY — TalentSkill / ProjectContributionRole / JudgeExpertise are never
 * permission inputs.
 */
@Service
public class JwtTokenService {

    private final JwtEncoder encoder;
    private final String issuer;
    private final Duration ttl;

    public JwtTokenService(@Value("${app.jwt.secret}") String secret,
                           @Value("${app.jwt.issuer}") String issuer,
                           @Value("${app.jwt.access-token-ttl}") Duration ttl) {
        byte[] key = secret.getBytes(StandardCharsets.UTF_8);
        SecretKeySpec spec = new SecretKeySpec(key, "HmacSHA256");
        this.encoder = new NimbusJwtEncoder(new ImmutableSecret<>(spec));
        this.issuer = issuer;
        this.ttl = ttl;
    }

    public String issueAccessToken(UUID userId, List<String> systemRoles) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(issuer)
                .issuedAt(now)
                .expiresAt(now.plus(ttl))
                .subject(userId.toString())
                .claim("roles", systemRoles)
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }

    public Duration ttl() { return ttl; }
}
