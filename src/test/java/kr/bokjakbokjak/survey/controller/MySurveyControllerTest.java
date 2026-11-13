package kr.bokjakbokjak.survey.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import kr.bokjakbokjak.common.JwtService;
import kr.bokjakbokjak.common.SurveyRevisionChangedEvent;
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
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@RecordApplicationEvents
class MySurveyControllerTest {
    private static final String CLEANING_FIRST = """
            [{"category_code":"CLEANING","rank":1},
             {"category_code":"SLEEP_SCHEDULE","rank":2},
             {"category_code":"PERSONAL_HYGIENE","rank":3}]""";

    @Autowired MockMvc mvc;
    @Autowired JwtService jwtService;
    @Autowired JdbcClient jdbc;
    @Autowired ApplicationEvents events;

    @DynamicPropertySource
    static void config(DynamicPropertyRegistry registry) {
        registry.add("app.jwt.secret-base64", () -> Base64.getEncoder().encodeToString(new byte[32]));
    }

    @Test
    void requiresAuthentication() throws Exception {
        mvc.perform(get("/api/me/survey")).andExpect(status().isUnauthorized());
        mvc.perform(put("/api/me/survey").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void returnsEmptyAnswersBeforeFirstSubmission() throws Exception {
        long userId = createUser(true);

        mvc.perform(get("/api/me/survey").header("Authorization", bearer(userId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("MY_SURVEY_SUCCESS"))
                .andExpect(jsonPath("$.data.revision").value(0))
                .andExpect(jsonPath("$.data.survey_completed").value(false))
                .andExpect(jsonPath("$.data.submitted_at").doesNotExist())
                .andExpect(jsonPath("$.data.priorities", hasSize(0)))
                .andExpect(jsonPath("$.data.answers", hasSize(0)));
    }

    @Test
    void firstSubmissionSavesAnswersAndPublishesRevision() throws Exception {
        long userId = createUser(true);

        submit(userId, 0, CLEANING_FIRST, answers())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SURVEY_SAVE_SUCCESS"))
                .andExpect(jsonPath("$.data.revision").value(1))
                .andExpect(jsonPath("$.data.survey_completed").value(true))
                .andExpect(jsonPath("$.data.submitted_at").exists())
                .andExpect(jsonPath("$.data.next_step").value("HOME"));

        mvc.perform(get("/api/me/survey").header("Authorization", bearer(userId)))
                .andExpect(jsonPath("$.data.revision").value(1))
                .andExpect(jsonPath("$.data.priorities[*].category_code")
                        .value(contains("CLEANING", "SLEEP_SCHEDULE", "PERSONAL_HYGIENE")))
                .andExpect(jsonPath("$.data.answers", hasSize(16)))
                .andExpect(jsonPath("$.data.answers[0].question_code").value("SLEEP_HABITS_H"))
                .andExpect(jsonPath("$.data.answers[15].question_code").value("SMOKING_T"))
                .andExpect(jsonPath("$.data.answers[15].option_code").value("OUTSIDE_LOW_ODOR"));

        assertThat(revisionEvents(userId)).containsExactly(1);
    }

    @Test
    void resendingSameContentKeepsRevision() throws Exception {
        long userId = createUser(true);
        submit(userId, 0, CLEANING_FIRST, answers()).andExpect(status().isOk());

        submit(userId, 0, CLEANING_FIRST, answers())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.revision").value(1));

        assertThat(revisionEvents(userId)).containsExactly(1);
    }

    @Test
    void changingAnswersOrPrioritiesRaisesRevision() throws Exception {
        long userId = createUser(true);
        submit(userId, 0, CLEANING_FIRST, answers()).andExpect(status().isOk());

        Map<String, String> changed = answers();
        changed.put("FLOOR_CLEAN_H", "DAILY");
        String fivePriorities = """
                [{"category_code":"SMOKING","rank":1},
                 {"category_code":"CLEANING","rank":2},
                 {"category_code":"SLEEP_HABITS","rank":3},
                 {"category_code":"SLEEP_SCHEDULE","rank":4},
                 {"category_code":"PERSONAL_HYGIENE","rank":5}]""";

        submit(userId, 1, fivePriorities, changed)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.revision").value(2));

        mvc.perform(get("/api/me/survey").header("Authorization", bearer(userId)))
                .andExpect(jsonPath("$.data.priorities", hasSize(5)))
                .andExpect(jsonPath("$.data.priorities[0].category_code").value("SMOKING"))
                .andExpect(jsonPath("$.data.answers[6].option_code").value("DAILY"));

        assertThat(revisionEvents(userId)).containsExactly(1, 2);
    }

    @Test
    void staleRevisionIsRejected() throws Exception {
        long userId = createUser(true);
        submit(userId, 0, CLEANING_FIRST, answers()).andExpect(status().isOk());

        Map<String, String> changed = answers();
        changed.put("ALARM_H", "ONCE");

        submit(userId, 0, CLEANING_FIRST, changed)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("SURVEY_REVISION_CONFLICT"));
    }

    @Test
    void onboardingMustBeCompletedFirst() throws Exception {
        long userId = createUser(false);

        submit(userId, 0, CLEANING_FIRST, answers())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ONBOARDING_REQUIRED"));
    }

    @Test
    void rejectsInvalidPriorities() throws Exception {
        long userId = createUser(true);
        String twoOnly = """
                [{"category_code":"CLEANING","rank":1},{"category_code":"SMOKING","rank":2}]""";
        String gapInRanks = """
                [{"category_code":"CLEANING","rank":1},{"category_code":"SMOKING","rank":2},
                 {"category_code":"SLEEP_HABITS","rank":4}]""";
        String duplicatedCategory = """
                [{"category_code":"CLEANING","rank":1},{"category_code":"CLEANING","rank":2},
                 {"category_code":"SLEEP_HABITS","rank":3}]""";
        String unknownCategory = """
                [{"category_code":"CLEANING","rank":1},{"category_code":"ODOR","rank":2},
                 {"category_code":"SLEEP_HABITS","rank":3}]""";

        for (String priorities : List.of(twoOnly, gapInRanks, duplicatedCategory, unknownCategory)) {
            submit(userId, 0, priorities, answers())
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("INVALID_INPUT"))
                    .andExpect(jsonPath("$.errors.priorities").exists());
        }
    }

    @Test
    void rejectsMissingOrWrongAnswers() throws Exception {
        long userId = createUser(true);

        Map<String, String> missing = answers();
        missing.remove("SMOKING_T");
        Map<String, String> otherQuestionsOption = answers();
        otherQuestionsOption.put("SMOKING_T", "DAILY");

        for (Map<String, String> answers : List.of(missing, otherQuestionsOption)) {
            submit(userId, 0, CLEANING_FIRST, answers)
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors.answers").exists());
        }

        mvc.perform(put("/api/me/survey").header("Authorization", bearer(userId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"priorities\":" + CLEANING_FIRST + "}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.expected_revision").exists())
                .andExpect(jsonPath("$.errors.answers").exists());
    }

    private ResultActions submit(long userId, int expectedRevision, String priorities, Map<String, String> answers)
            throws Exception {
        String answerJson = answers.entrySet().stream()
                .map(entry -> "{\"question_code\":\"%s\",\"option_code\":\"%s\"}"
                        .formatted(entry.getKey(), entry.getValue()))
                .collect(Collectors.joining(",", "[", "]"));
        String body = "{\"expected_revision\":%d,\"priorities\":%s,\"answers\":%s}"
                .formatted(expectedRevision, priorities, answerJson);
        return mvc.perform(put("/api/me/survey").header("Authorization", bearer(userId))
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private static Map<String, String> answers() {
        Map<String, String> answers = new LinkedHashMap<>();
        answers.put("SLEEP_HABITS_H", "UNKNOWN");
        answers.put("SLEEP_HABITS_T", "OCCASIONAL_OK");
        answers.put("BEDTIME_H", "FROM_22_TO_24");
        answers.put("BEDTIME_T", "BY_01");
        answers.put("ALARM_H", "AT_MOST_2");
        answers.put("ALARM_T", "UP_TO_2");
        answers.put("FLOOR_CLEAN_H", "WEEKLY");
        answers.put("FLOOR_CLEAN_T", "WEEKLY_MIN");
        answers.put("FOOD_WASTE_H", "SAME_DAY");
        answers.put("FOOD_WASTE_T", "SAME_DAY_OK");
        answers.put("BELONGINGS_H", "SAME_DAY");
        answers.put("BELONGINGS_T", "NO_SPACE_INVASION");
        answers.put("DISHES_H", "NEXT_DAY");
        answers.put("DISHES_T", "NEXT_DAY_OK");
        answers.put("SMOKING_H", "NON_SMOKER");
        answers.put("SMOKING_T", "OUTSIDE_LOW_ODOR");
        return answers;
    }

    private long createUser(boolean onboarded) {
        var keyHolder = new GeneratedKeyHolder();
        jdbc.sql("""
                        insert into app_user (onboarding_completed_at, survey_revision, created_at, updated_at)
                        values (%s, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)"""
                        .formatted(onboarded ? "CURRENT_TIMESTAMP" : "NULL"))
                .update(keyHolder);
        return keyHolder.getKeyAs(Long.class);
    }

    private List<Integer> revisionEvents(long userId) {
        return events.stream(SurveyRevisionChangedEvent.class)
                .filter(event -> event.userId() == userId)
                .map(SurveyRevisionChangedEvent::revision)
                .toList();
    }

    private String bearer(long userId) {
        return "Bearer " + jwtService.issueAccessToken(userId).accessToken();
    }
}
