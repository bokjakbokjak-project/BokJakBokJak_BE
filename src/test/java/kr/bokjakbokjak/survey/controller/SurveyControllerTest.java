package kr.bokjakbokjak.survey.controller;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Base64;
import kr.bokjakbokjak.common.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SurveyControllerTest {
    @Autowired MockMvc mvc;
    @Autowired JwtService jwtService;

    @DynamicPropertySource
    static void config(DynamicPropertyRegistry registry) {
        registry.add("app.jwt.secret-base64", () -> Base64.getEncoder().encodeToString(new byte[32]));
    }

    @Test
    void requiresAuthentication() throws Exception {
        mvc.perform(get("/api/surveys/current"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void returnsCategoriesAndTopicsInDisplayOrder() throws Exception {
        mvc.perform(get("/api/surveys/current").header("Authorization", bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value("SURVEY_SCHEMA_SUCCESS"))
                .andExpect(jsonPath("$.data.categories[*].category_code").value(contains(
                        "SLEEP_HABITS", "SLEEP_SCHEDULE", "CLEANING", "PERSONAL_HYGIENE", "SMOKING")))
                .andExpect(jsonPath("$.data.topics[*].topic_code").value(contains(
                        "SLEEP_HABITS", "BEDTIME", "ALARM", "FLOOR_CLEAN",
                        "FOOD_WASTE", "BELONGINGS", "DISHES", "SMOKING")))
                .andExpect(jsonPath("$.data.topics[*].category_code").value(contains(
                        "SLEEP_HABITS", "SLEEP_SCHEDULE", "SLEEP_SCHEDULE", "CLEANING",
                        "CLEANING", "PERSONAL_HYGIENE", "PERSONAL_HYGIENE", "SMOKING")));
    }

    @Test
    void eachTopicHasHabitThenToleranceQuestion() throws Exception {
        mvc.perform(get("/api/surveys/current").header("Authorization", bearer()))
                .andExpect(jsonPath("$.data.topics[*].questions[*].question_number").value(contains(
                        1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16)))
                .andExpect(jsonPath("$.data.topics[*].questions[0].kind").value(everyItem(is("HABIT"))))
                .andExpect(jsonPath("$.data.topics[*].questions[1].kind").value(everyItem(is("TOLERANCE"))))
                .andExpect(jsonPath("$.data.topics[*].questions[*].options[*]", hasSize(73)));
    }

    @Test
    void returnsConfirmedWordingAndOptionOrder() throws Exception {
        mvc.perform(get("/api/surveys/current").header("Authorization", bearer()))
                .andExpect(jsonPath("$.data.topics[0].scenario_text")
                        .value("밤에 함께 자고 있는데, 한 사람이 자는 동안 코를 골거나 잠꼬대를 해요."))
                .andExpect(jsonPath("$.data.topics[0].questions[0].question_code").value("SLEEP_HABITS_H"))
                .andExpect(jsonPath("$.data.topics[0].questions[0].prompt")
                        .value("나는 코골이, 잠꼬대 같은 잠버릇이 어느 정도 있는 편인가요?"))
                .andExpect(jsonPath("$.data.topics[0].questions[0].options[*].option_code").value(contains(
                        "FREQUENT", "OCCASIONAL", "RARE", "NONE", "UNKNOWN")))
                .andExpect(jsonPath("$.data.topics[1].questions[0].options", hasSize(7)))
                .andExpect(jsonPath("$.data.topics[7].questions[1].question_code").value("SMOKING_T"))
                .andExpect(jsonPath("$.data.topics[7].questions[1].options[3].label")
                        .value("흡연자와는 같은 방이 어렵다"));
    }

    private String bearer() {
        return "Bearer " + jwtService.issueAccessToken(42).accessToken();
    }
}
