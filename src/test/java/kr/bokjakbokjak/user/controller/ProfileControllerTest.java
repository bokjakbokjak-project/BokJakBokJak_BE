package kr.bokjakbokjak.user.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Year;
import java.time.ZoneId;
import java.util.Base64;
import kr.bokjakbokjak.common.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/** 페르소나 테스트 유저(V6) 기준으로 프로필을 확인한다. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ProfileControllerTest {
    @Autowired MockMvc mvc;
    @Autowired JwtService jwtService;
    @Autowired JdbcClient jdbc;

    @DynamicPropertySource
    static void config(DynamicPropertyRegistry registry) {
        registry.add("app.jwt.secret-base64", () -> Base64.getEncoder().encodeToString(new byte[32]));
    }

    @Test
    void myProfileListsAllCategoriesAndTopicsInDisplayOrder() throws Exception {
        long bookmark = persona("책갈피");
        int age = Year.now(ZoneId.of("Asia/Seoul")).getValue() - 2004 + 1;

        getAs(bookmark, "/api/me/profile")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("MY_PROFILE_SUCCESS"))
                .andExpect(jsonPath("$.data.user_id").value((int) bookmark))
                .andExpect(jsonPath("$.data.nickname").value("책갈피"))
                .andExpect(jsonPath("$.data.age").value(age))
                .andExpect(jsonPath("$.data.department").value("산업공학과"))
                .andExpect(jsonPath("$.data.top_categories[0]").value("CLEANING"))
                .andExpect(jsonPath("$.data.top_categories.length()").value(3))
                .andExpect(jsonPath("$.data.survey_revision").value(1))
                .andExpect(jsonPath("$.data.categories.length()").value(5))
                .andExpect(jsonPath("$.data.categories[0].category_code").value("SLEEP_HABITS"))
                .andExpect(jsonPath("$.data.categories[0].name").value("잠버릇"))
                .andExpect(jsonPath("$.data.categories[1].category_code").value("SLEEP_SCHEDULE"))
                .andExpect(jsonPath("$.data.categories[1].topics.length()").value(2))
                .andExpect(jsonPath("$.data.categories[1].topics[0].topic_code").value("BEDTIME"))
                .andExpect(jsonPath("$.data.categories[1].topics[0].question")
                        .value("평일에 나는 보통 몇 시쯤 잠자리에 드나요?"))
                .andExpect(jsonPath("$.data.categories[1].topics[0].habit_answer").value("밤 11시~12시"))
                .andExpect(jsonPath("$.data.categories[1].topics[0].tolerance_answer").value("밤 12시까지"))
                .andExpect(jsonPath("$.data.categories[1].topics[1].topic_code").value("ALARM"))
                .andExpect(jsonPath("$.data.categories[4].category_code").value("SMOKING"))
                .andExpect(jsonPath("$.data.categories[4].topics[0].habit_answer").value("비흡연자다"))
                .andExpect(jsonPath("$.data.categories[4].topics[0].tolerance_answer")
                        .value("흡연자와는 같은 방이 어렵다"));
    }

    @Test
    void myProfileRequiresSurvey() throws Exception {
        getAs(createUser("DONGGUK", 0, "ACTIVE"), "/api/me/profile")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("SURVEY_REQUIRED"));
    }

    @Test
    void roommateProfileShowsCandidateAnswersOnly() throws Exception {
        long bookmark = persona("책갈피");
        long tidy = persona("깔끔이");

        getAs(bookmark, "/api/roommates/" + tidy + "/profile")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("ROOMMATE_PROFILE_SUCCESS"))
                .andExpect(jsonPath("$.data.candidate_id").value((int) tidy))
                .andExpect(jsonPath("$.data.nickname").value("깔끔이"))
                .andExpect(jsonPath("$.data.department").value("화학과"))
                .andExpect(jsonPath("$.data.top_categories[1]").value("PERSONAL_HYGIENE"))
                .andExpect(jsonPath("$.data.categories.length()").value(5))
                .andExpect(jsonPath("$.data.categories[1].topics[0].habit_answer").value("밤 10시~11시"))
                .andExpect(jsonPath("$.data.user_id").doesNotExist());
    }

    @Test
    void roommateProfileRejectsSelfOtherSchoolAndUnavailableUsers() throws Exception {
        long bookmark = persona("책갈피");

        getAs(bookmark, "/api/roommates/" + bookmark + "/profile")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.candidate_id").exists());
        for (long candidate : new long[] {
                createUser("KOOKMIN", 1, "ACTIVE"),
                createUser("DONGGUK", 0, "ACTIVE"),
                createUser("DONGGUK", 1, "WITHDRAWN"),
                999999L}) {
            getAs(bookmark, "/api/roommates/" + candidate + "/profile")
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("ROOMMATE_NOT_FOUND"));
        }
    }

    @Test
    void nicknameChangeKeepsSurveyRevision() throws Exception {
        long userId = createUser("DONGGUK", 3, "ACTIVE");

        changeNickname(userId, "{\"nickname\":\"  복작이  \"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("NICKNAME_UPDATE_SUCCESS"))
                .andExpect(jsonPath("$.data.nickname").value("복작이"));

        var row = jdbc.sql("select nickname, survey_revision from app_user where id = ?")
                .param(userId).query().singleRow();
        assertThat(row.get("nickname")).isEqualTo("복작이");
        assertThat(((Number) row.get("survey_revision")).intValue()).isEqualTo(3);
    }

    @Test
    void nicknameMustBeOneToTwentyCharacters() throws Exception {
        long userId = createUser("DONGGUK", 0, "ACTIVE");

        for (String body : new String[] {
                "{\"nickname\":\"   \"}", "{\"nickname\":\"" + "가".repeat(21) + "\"}", "{}"}) {
            changeNickname(userId, body)
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("INVALID_NICKNAME"))
                    .andExpect(jsonPath("$.errors.nickname").value("INVALID_FORMAT"));
        }
    }

    private ResultActions getAs(long userId, String path) throws Exception {
        return mvc.perform(get(path).header("Authorization", bearer(userId)));
    }

    private ResultActions changeNickname(long userId, String body) throws Exception {
        return mvc.perform(patch("/api/me/profile")
                .header("Authorization", bearer(userId))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    private String bearer(long userId) {
        return "Bearer " + jwtService.issueAccessToken(userId).accessToken();
    }

    private long persona(String nickname) {
        return jdbc.sql("select id from app_user where test_user = TRUE and nickname = ?")
                .param(nickname).query(Long.class).single();
    }

    private long createUser(String universityCode, int surveyRevision, String status) {
        var keyHolder = new GeneratedKeyHolder();
        jdbc.sql("""
                        insert into app_user (university_id, survey_revision, status, created_at, updated_at)
                        values ((select id from university where code = ?), ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)""")
                .params(universityCode, surveyRevision, status)
                .update(keyHolder);
        return keyHolder.getKeyAs(Long.class);
    }
}
