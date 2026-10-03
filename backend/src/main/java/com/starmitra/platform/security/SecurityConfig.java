package com.starmitra.platform.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.security.oauth2.server.resource.web.DefaultBearerTokenResolver;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * ADR-006 security: stateless JWT (Bearer for mobile, httpOnly cookie for web).
 * CSRF applies to cookie-carried requests; Bearer flows are CSRF-exempt.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final String cookieName;
    private final List<String> corsOrigins;

    public SecurityConfig(@Value("${app.auth.cookie.name}") String cookieName,
                          @Value("${app.cors.allowed-origins}") String corsOrigins) {
        this.cookieName = cookieName;
        this.corsOrigins = Arrays.asList(corsOrigins.split(","));
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, JwtDecoder jwtDecoder) throws Exception {
        CsrfTokenRequestAttributeHandler csrfHandler = new CsrfTokenRequestAttributeHandler();
        http
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .csrf(csrf -> csrf
                .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                .csrfTokenRequestHandler(csrfHandler)
                // Bearer-token requests carry no ambient credential → CSRF-exempt
                .ignoringRequestMatchers(req -> req.getHeader("Authorization") != null)
            )
            .cors(cors -> cors.configurationSource(corsSource()))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.POST,
                    "/api/v1/auth/otp/request",
                    "/api/v1/auth/otp/verify",
                    "/api/v1/auth/refresh").permitAll()
                .requestMatchers("/api/v1/public/**", "/actuator/health", "/v3/api-docs/**", "/openapi.yaml").permitAll()
                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(oauth -> oauth
                .jwt(jwt -> jwt.decoder(jwtDecoder).jwtAuthenticationConverter(jwtConverter()))
                .bearerTokenResolver(bearerTokenResolver())
            );
        return http.build();
    }

    /** Bearer header first; fall back to the httpOnly access cookie for web. */
    @Bean
    BearerTokenResolver bearerTokenResolver() {
        DefaultBearerTokenResolver delegate = new DefaultBearerTokenResolver();
        return req -> {
            String token = delegate.resolve(req);
            if (token != null) return token;
            if (req.getCookies() != null) {
                for (var c : req.getCookies()) {
                    if (cookieName.equals(c.getName())) return c.getValue();
                }
            }
            return null;
        };
    }

    private JwtAuthenticationConverter jwtConverter() {
        JwtGrantedAuthoritiesConverter authorities = new JwtGrantedAuthoritiesConverter();
        authorities.setAuthoritiesClaimName("roles");
        authorities.setAuthorityPrefix("ROLE_");
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(authorities);
        return converter;
    }

    private CorsConfigurationSource corsSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(corsOrigins);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type", "Idempotency-Key", "If-Match", "X-Correlation-Id"));
        config.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }
}
