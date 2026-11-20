package kr.bokjakbokjak.user.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Base64;
import kr.bokjakbokjak.common.JwtService;
import kr.bokjakbokjak.room.entity.Matching;
import kr.bokjakbokjak.room.repository.MatchingRepository;
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

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MyStatusControllerTest {
    @Autowired MockMvc mvc;
    @Autowired JwtService jwtService;
    @Autowired JdbcClient jdbc;
    @Autowired MatchingRepository matchingRepository;

    @DynamicPropertySource
    static void config(DynamicPropertyRegistry registry) {
        registry.add("app.jwt.secret-base64", () -> Base64.getEncoder().encodeToString(new byte[32]));
    }

    @Test
    void requiresAuthentication() throws Exception {
        mvc.perform(get("/api/me/status")).andExpect(status().isUnauthorized());
    }

    @Test
    void newUserGoesToOnboarding() throws Exception {
        long userId = createUser(false, 0);

        getStatus(userId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("MY_STATUS_SUCCESS"))
                .andExpect(jsonPath("$.data.onboarding_completed").value(false))
                .andExpect(jsonPath("$.data.survey_completed").value(false))
                .andExpect(jsonPath("$.data.survey_update_required").value(false))
                .andExpect(jsonPath("$.data.next_step").value("ONBOARDING"))
                .andExpect(jsonPath("$.data.matched").value(false))
                .andExpect(jsonPath("$.data.matching_id").doesNotExist());
    }

    @Test
    void onboardedUserGoesToSurvey() throws Exception {
        long userId = createUser(true, 0);

        getStatus(userId)
                .andExpect(jsonPath("$.data.onboarding_completed").value(true))
                .andExpect(jsonPath("$.data.survey_completed").value(false))
                .andExpect(jsonPath("$.data.next_step").value("SURVEY"));
    }

    @Test
    void surveyedUserGoesHomeAndSeesFinalMatching() throws Exception {
        long bookmark = persona("책갈피");
        long mint = persona("민트초코");

        getStatus(bookmark)
                .andExpect(jsonPath("$.data.survey_completed").value(true))
                .andExpect(jsonPath("$.data.next_step").value("HOME"))
                .andExpect(jsonPath("$.data.matched").value(false));

        Long matchingId = matchingRepository.save(Matching.start(bookmark, mint, 1, 1)).getId();
        try {
            getStatus(bookmark).andExpect(jsonPath("$.data.matched").value(false));

            jdbc.sql("update matching set status = 'CONFIRMED' where matching_id = ?").param(matchingId).update();
            getStatus(bookmark)
                    .andExpect(jsonPath("$.data.matched").value(true))
                    .andExpect(jsonPath("$.data.matching_id").value(matchingId.intValue()));
            getStatus(mint).andExpect(jsonPath("$.data.matching_id").value(matchingId.intValue()));
        } finally {
            matchingRepository.deleteById(matchingId);
        }
    }

    private ResultActions getStatus(long userId) throws Exception {
        return mvc.perform(get("/api/me/status")
                .header("Authorization", "Bearer " + jwtService.issueAccessToken(userId).accessToken()));
    }

    private long persona(String nickname) {
        return jdbc.sql("select id from app_user where test_user = TRUE and nickname = ?")
                .param(nickname).query(Long.class).single();
    }

    private long createUser(boolean onboarded, int surveyRevision) {
        var keyHolder = new GeneratedKeyHolder();
        jdbc.sql("""
                        insert into app_user (onboarding_completed_at, survey_revision, created_at, updated_at)
                        values (%s, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)"""
                        .formatted(onboarded ? "CURRENT_TIMESTAMP" : "NULL"))
                .param(surveyRevision)
                .update(keyHolder);
        return keyHolder.getKeyAs(Long.class);
    }
}
