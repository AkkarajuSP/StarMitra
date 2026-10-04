package com.starmitra.platform.security;

import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * CSRF bootstrap — touches the deferred token so CookieCsrfTokenRepository
 * writes the XSRF-TOKEN cookie. SPAs call this once before their first
 * cookie-carried mutation; Bearer requests are CSRF-exempt by config.
 * permitAll via /api/v1/public/**.
 */
@RestController
public class CsrfController {

    @GetMapping("/api/v1/public/csrf")
    public Map<String, String> csrf(CsrfToken token) {
        return Map.of("token", token.getToken());
    }
}
