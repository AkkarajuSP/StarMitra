package com.starmitra.security;

import com.starmitra.modules.judge.application.JudgeScopeService;
import com.starmitra.modules.judgeportal.api.JudgePortalController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.starmitra.platform.security.SecurityConfig;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import com.starmitra.platform.correlation.CorrelationIdFilter;

/**
 * Security baseline — unauthenticated denied; role-less users denied;
 * JUDGE role reaches scope resolution (service decides scope).
 */
@WebMvcTest(controllers = JudgePortalController.class)
@Import({SecurityConfig.class, CorrelationIdFilter.class})
class SecurityBaselineTest {

    @Autowired MockMvc mvc;
    @MockBean JudgeScopeService judgeScope;
    @MockBean com.starmitra.modules.judge.application.JudgeService judgeService;
    @MockBean JwtDecoder jwtDecoder;

    @Test
    void unauthenticatedIs401() throws Exception {
        mvc.perform(get("/api/v1/judges/me")).andExpect(status().isUnauthorized());
    }

    @Test
    void authenticatedUserReachesScopeResolver() throws Exception {
        var userId = java.util.UUID.randomUUID();
        when(judgeScope.requireJudge(any())).thenReturn(new JudgeScopeService.JudgeView(userId, "ACTIVE"));
        mvc.perform(get("/api/v1/judges/me").with(jwt().jwt(j -> j.subject(userId.toString())
                        .claim("roles", java.util.List.of("USER")))))
            .andExpect(status().isOk());
        verify(judgeScope).requireJudge(userId);    // service enforces ROLE_JUDGE internally
    }

    @Test
    void correlationIdEchoedOnResponse() throws Exception {
        mvc.perform(get("/api/v1/judges/me")
                .header("X-Correlation-Id", "test-corr-1"))
            .andExpect(header().string("X-Correlation-Id", "test-corr-1"));
    }
}
