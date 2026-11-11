package kr.bokjakbokjak.recommendation.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Base64;
import java.util.List;
import kr.bokjakbokjak.common.ApiException;
import kr.bokjakbokjak.common.ErrorCode;
import kr.bokjakbokjak.common.JwtService;
import kr.bokjakbokjak.recommendation.dto.LifestyleSummaryResponse;
import kr.bokjakbokjak.recommendation.service.LifestyleSummaryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class LifestyleSummaryControllerTest {
    @Autowired MockMvc mvc;
    @Autowired JwtService jwtService;
    @MockitoBean LifestyleSummaryService lifestyleSummaryService;

    @DynamicPropertySource
    static void config(DynamicPropertyRegistry registry) {
        registry.add("app.jwt.secret-base64", () -> Base64.getEncoder().encodeToString(new byte[32]));
    }

    @Test
    void requiresAuthentication() throws Exception {
        mvc.perform(get("/api/users/12/lifestyle-summary")).andExpect(status().isUnauthorized());
    }

    @Test
    void returnsSummaryInSnakeCase() throws Exception {
        when(lifestyleSummaryService.getSummary(12L)).thenReturn(new LifestyleSummaryResponse(
                12L, "거의 매일 바닥을 청소해요.", List.of("매일 청소", "비흡연"), Instant.parse("2026-11-12T03:00:00Z")));

        mvc.perform(get("/api/users/12/lifestyle-summary").header("Authorization", bearer(7L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("LIFESTYLE_SUMMARY_SUCCESS"))
                .andExpect(jsonPath("$.data.user_id").value(12))
                .andExpect(jsonPath("$.data.summary").value("거의 매일 바닥을 청소해요."))
                .andExpect(jsonPath("$.data.keywords[1]").value("비흡연"))
                .andExpect(jsonPath("$.data.generated_at").value("2026-11-12T03:00:00Z"));
    }

    @Test
    void withdrawnUserIsNotFound() throws Exception {
        when(lifestyleSummaryService.getSummary(99L)).thenThrow(new ApiException(ErrorCode.TARGET_WITHDRAWN));

        mvc.perform(get("/api/users/99/lifestyle-summary").header("Authorization", bearer(7L)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("TARGET_WITHDRAWN"));
    }

    private String bearer(long userId) {
        return "Bearer " + jwtService.issueAccessToken(userId).accessToken();
    }
}
