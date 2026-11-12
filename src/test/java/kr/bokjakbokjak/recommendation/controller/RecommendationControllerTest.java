package kr.bokjakbokjak.recommendation.controller;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.Base64;
import java.util.List;
import kr.bokjakbokjak.common.JwtService;
import kr.bokjakbokjak.matching.MatchResultDto;
import kr.bokjakbokjak.matching.MatchingEngine;
import kr.bokjakbokjak.recommendation.service.InMemoryRecommendationSnapshotStore;
import kr.bokjakbokjak.support.FakeUserQueryService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RecommendationControllerTest {
    private static final long VIEWER = 7L;
    private static final long MINT = 12L;

    @MockitoBean MatchingEngine matchingEngine;

    @Autowired MockMvc mvc;
    @Autowired JwtService jwtService;
    @Autowired FakeUserQueryService users;
    @Autowired InMemoryRecommendationSnapshotStore store;

    @DynamicPropertySource
    static void config(DynamicPropertyRegistry registry) {
        registry.add("app.jwt.secret-base64", () -> Base64.getEncoder().encodeToString(new byte[32]));
    }

    @BeforeEach
    void setUp() {
        users.clear();
        store.clear();
        users.addUser(VIEWER, "책갈피", 1);
        users.addUser(MINT, "민트초코", 1);
        users.connect(VIEWER, MINT);
        given(matchingEngine.calculate(anyLong(), anyLong())).willAnswer(invocation -> new MatchResultDto(
                invocation.getArgument(0), invocation.getArgument(1), new BigDecimal("0.82"),
                BigDecimal.ONE, BigDecimal.ONE, List.of()));
    }

    @AfterEach
    void cleanUp() {
        users.clear();
        store.clear();
    }

    @Test
    void requiresAuthentication() throws Exception {
        mvc.perform(get("/api/recommendations")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/recommendations/refresh")).andExpect(status().isUnauthorized());
    }

    @Test
    void listsRecommendations() throws Exception {
        mvc.perform(authorized(get("/api/recommendations"), VIEWER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("RECOMMENDATION_LIST_SUCCESS"))
                .andExpect(jsonPath("$.data.total_count").value(1))
                .andExpect(jsonPath("$.data.recommendations[0].user_id").value(MINT))
                .andExpect(jsonPath("$.data.recommendations[0].nickname").value("민트초코"))
                .andExpect(jsonPath("$.data.recommendations[0].department").value("컴퓨터공학과"))
                .andExpect(jsonPath("$.data.recommendations[0].top_categories").isArray())
                .andExpect(jsonPath("$.data.recommendations[0].match_score").value(82))
                .andExpect(jsonPath("$.data.recommendations[0].relation").value("NONE"));
    }

    @Test
    void refreshesRecommendations() throws Exception {
        mvc.perform(authorized(get("/api/recommendations"), VIEWER)).andExpect(status().isOk());

        mvc.perform(authorized(post("/api/recommendations/refresh"), VIEWER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("RECOMMENDATION_REFRESH_SUCCESS"))
                .andExpect(jsonPath("$.data.changed").value(false))
                .andExpect(jsonPath("$.data.total_count").value(1))
                .andExpect(jsonPath("$.data.recommendations[0].user_id").value(MINT));
    }

    @Test
    void viewerWithoutSurveyIsConflict() throws Exception {
        users.addUser(VIEWER, "책갈피", 0);

        mvc.perform(authorized(get("/api/recommendations"), VIEWER))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ONBOARDING_REQUIRED"));
    }

    private MockHttpServletRequestBuilder authorized(MockHttpServletRequestBuilder request, long userId) {
        return request.header("Authorization", "Bearer " + jwtService.issueAccessToken(userId).accessToken());
    }
}
