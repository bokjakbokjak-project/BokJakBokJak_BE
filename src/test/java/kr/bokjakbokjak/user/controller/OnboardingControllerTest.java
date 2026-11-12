package kr.bokjakbokjak.user.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Base64;
import java.util.List;
import java.util.Map;
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

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OnboardingControllerTest {
    private static final String THREE_PRIORITIES = """
            [{"category_code":"CLEANING","rank":1},{"category_code":"SLEEP_SCHEDULE","rank":2},
             {"category_code":"SMOKING","rank":3}]""";

    @Autowired MockMvc mvc;
    @Autowired JwtService jwtService;
    @Autowired JdbcClient jdbc;

    @DynamicPropertySource
    static void config(DynamicPropertyRegistry registry) {
        registry.add("app.jwt.secret-base64", () -> Base64.getEncoder().encodeToString(new byte[32]));
    }

    @Test
    void savesProfileAndPrioritiesThenNextStepIsSurvey() throws Exception {
        long userId = createUser("DONGGUK");

        save(userId, body("  새내기  ", "2006", roomTypeId("남산학사", "2인실"), THREE_PRIORITIES))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("ONBOARDING_SAVE_SUCCESS"))
                .andExpect(jsonPath("$.data.onboarding_completed").value(true))
                .andExpect(jsonPath("$.data.survey_completed").value(false))
                .andExpect(jsonPath("$.data.next_step").value("SURVEY"));

        Map<String, Object> user = jdbc.sql("""
                        select nickname, gender, birth_year, department, student_number, room_type_id,
                               onboarding_completed_at
                        from app_user where id = ?""")
                .param(userId).query().singleRow();
        assertThat(user.get("nickname")).isEqualTo("새내기");
        assertThat(user.get("gender")).isEqualTo("FEMALE");
        assertThat(((Number) user.get("birth_year")).intValue()).isEqualTo(2006);
        assertThat(user.get("department")).isEqualTo("컴퓨터공학과");
        assertThat(user.get("student_number")).isEqualTo("2025123456");
        assertThat(((Number) user.get("room_type_id")).longValue()).isEqualTo(roomTypeId("남산학사", "2인실"));
        assertThat(user.get("onboarding_completed_at")).isNotNull();
        assertThat(categories(userId)).containsExactly("CLEANING", "SLEEP_SCHEDULE", "SMOKING");
    }

    @Test
    void resaveBeforeSurveyReplacesPriorities() throws Exception {
        long userId = createUser("DONGGUK");
        long roomTypeId = roomTypeId("남산학사", "2인실");
        save(userId, body("다시저장", "2005", roomTypeId, """
                [{"category_code":"SMOKING","rank":1},{"category_code":"CLEANING","rank":2},
                 {"category_code":"SLEEP_HABITS","rank":3},{"category_code":"PERSONAL_HYGIENE","rank":4}]"""))
                .andExpect(status().isOk());

        save(userId, body("다시저장", "2005", roomTypeId, THREE_PRIORITIES))
                .andExpect(status().isOk());

        assertThat(categories(userId)).containsExactly("CLEANING", "SLEEP_SCHEDULE", "SMOKING");
    }

    @Test
    void rejectsOtherSchoolRoomTypeAndUnsupportedCapacity() throws Exception {
        long userId = createUser("DONGGUK");
        jdbc.sql("""
                        insert into dormitory_room_type (university_id, dormitory_key, dormitory_name, name, capacity)
                        select id, '국민온보딩학사', '국민온보딩학사', '2인실', 2 from university where code = 'KOOKMIN'""")
                .update();

        save(userId, body("남의학교", "2005", roomTypeId("국민온보딩학사", "2인실"), THREE_PRIORITIES))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ROOM_TYPE_NOT_FOUND"));
        save(userId, body("일인실", "2005", roomTypeId("남산학사", "1인실(장애인실)"), THREE_PRIORITIES))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("UNSUPPORTED_ROOM_CAPACITY"));
        save(userId, body("없는방", "2005", 999999, THREE_PRIORITIES))
                .andExpect(status().isNotFound());
    }

    @Test
    void validatesInput() throws Exception {
        long userId = createUser("DONGGUK");
        long roomTypeId = roomTypeId("남산학사", "2인실");

        save(userId, body("순위", "2005", roomTypeId, """
                [{"category_code":"CLEANING","rank":1},{"category_code":"SMOKING","rank":3},
                 {"category_code":"SLEEP_HABITS","rank":4}]"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.priorities").exists());
        save(userId, body("   ", "2005", roomTypeId, THREE_PRIORITIES))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.nickname").exists());
        save(userId, body("가".repeat(21), "2005", roomTypeId, THREE_PRIORITIES))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.nickname").exists());
        save(userId, body("출생", "1800", roomTypeId, THREE_PRIORITIES))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.birth_year").exists());
        save(userId, """
                {"department":"컴퓨터공학과","student_number":"abc","nickname":"학번","gender":"FEMALE",
                 "birth_year":2005,"room_type_id":%d,"priorities":%s}""".formatted(roomTypeId, THREE_PRIORITIES))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.student_number").exists());
        save(userId, """
                {"department":"컴퓨터공학과","student_number":"2025123456","nickname":"성별","gender":"OTHER",
                 "birth_year":2005,"room_type_id":%d,"priorities":%s}""".formatted(roomTypeId, THREE_PRIORITIES))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"));
    }

    @Test
    void cannotSaveAgainAfterFirstSurvey() throws Exception {
        long userId = createUser("DONGGUK");
        jdbc.sql("update app_user set survey_revision = 1 where id = ?").param(userId).update();

        save(userId, body("설문후", "2005", roomTypeId("남산학사", "2인실"), THREE_PRIORITIES))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ONBOARDING_ALREADY_SUBMITTED"));
    }

    private ResultActions save(long userId, String body) throws Exception {
        return mvc.perform(put("/api/me/onboarding")
                .header("Authorization", "Bearer " + jwtService.issueAccessToken(userId).accessToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    private static String body(String nickname, String birthYear, long roomTypeId, String priorities) {
        return """
                {"department":"컴퓨터공학과","student_number":"2025123456","nickname":"%s","gender":"FEMALE",
                 "birth_year":%s,"room_type_id":%d,"priorities":%s}"""
                .formatted(nickname, birthYear, roomTypeId, priorities);
    }

    private List<String> categories(long userId) {
        return jdbc.sql("select category_code from user_weight where user_id = ? order by priority_rank")
                .param(userId).query(String.class).list();
    }

    private long roomTypeId(String dormitoryKey, String name) {
        return jdbc.sql("select id from dormitory_room_type where dormitory_key = ? and name = ?")
                .params(dormitoryKey, name).query(Long.class).single();
    }

    private long createUser(String universityCode) {
        var keyHolder = new GeneratedKeyHolder();
        jdbc.sql("""
                        insert into app_user (university_id, survey_revision, created_at, updated_at)
                        values ((select id from university where code = ?), 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)""")
                .param(universityCode)
                .update(keyHolder);
        return keyHolder.getKeyAs(Long.class);
    }
}
