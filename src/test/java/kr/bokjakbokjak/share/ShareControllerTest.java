package kr.bokjakbokjak.share;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.RoundingMode;
import java.util.Base64;
import kr.bokjakbokjak.common.JwtService;
import kr.bokjakbokjak.matching.MatchingEngine;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/** 페르소나 테스트 유저(V6) 기준으로 공유 링크 매칭률을 확인한다. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ShareControllerTest {
    @Autowired MockMvc mvc;
    @Autowired JwtService jwtService;
    @Autowired JdbcClient jdbc;
    @Autowired MatchingEngine matchingEngine;

    @DynamicPropertySource
    static void config(DynamicPropertyRegistry registry) {
        registry.add("app.jwt.secret-base64", () -> Base64.getEncoder().encodeToString(new byte[32]));
    }

    @Test
    void requiresAuthentication() throws Exception {
        mvc.perform(get("/api/share/1/match-score")).andExpect(status().isUnauthorized());
    }

    @Test
    void scoresUsersRegardlessOfGenderAndDormitory() throws Exception {
        long bookmark = persona("책갈피");

        // 민트초코는 남성, 옆동주민은 다른 기숙사라 추천 후보는 아니지만 공유 매칭률은 나온다
        for (String other : new String[] {"민트초코", "옆동주민", "깔끔이"}) {
            long target = persona(other);
            int expected = matchingEngine.calculate(bookmark, target).mutualScore()
                    .movePointRight(2).setScale(0, RoundingMode.HALF_UP).intValueExact();

            matchScore(bookmark, target)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("SHARE_MATCH_SCORE_SUCCESS"))
                    .andExpect(jsonPath("$.data.target_user_id").value((int) target))
                    .andExpect(jsonPath("$.data.match_score").value(expected));
        }
    }

    @Test
    void viewerMustFinishSurvey() throws Exception {
        matchScore(createUser(0, "ACTIVE"), persona("책갈피"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("SURVEY_REQUIRED"));
    }

    @Test
    void rejectsSelfAndUnavailableTargets() throws Exception {
        long bookmark = persona("책갈피");

        matchScore(bookmark, bookmark)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.user_id").exists());
        for (long target : new long[] {createUser(0, "ACTIVE"), createUser(1, "WITHDRAWN"), 999999L}) {
            matchScore(bookmark, target)
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("TARGET_WITHDRAWN"));
        }
    }

    private ResultActions matchScore(long viewerId, long targetId) throws Exception {
        return mvc.perform(get("/api/share/" + targetId + "/match-score")
                .header("Authorization", "Bearer " + jwtService.issueAccessToken(viewerId).accessToken()));
    }

    private long persona(String nickname) {
        return jdbc.sql("select id from app_user where test_user = TRUE and nickname = ?")
                .param(nickname).query(Long.class).single();
    }

    private long createUser(int surveyRevision, String status) {
        var keyHolder = new GeneratedKeyHolder();
        jdbc.sql("""
                        insert into app_user (onboarding_completed_at, survey_revision, status, created_at, updated_at)
                        values (CURRENT_TIMESTAMP, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)""")
                .params(surveyRevision, status)
                .update(keyHolder);
        return keyHolder.getKeyAs(Long.class);
    }
}
