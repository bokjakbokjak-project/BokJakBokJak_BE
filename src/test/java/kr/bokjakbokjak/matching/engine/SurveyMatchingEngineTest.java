package kr.bokjakbokjak.matching.engine;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import kr.bokjakbokjak.matching.MatchLabel;
import kr.bokjakbokjak.matching.MatchResultDto;
import kr.bokjakbokjak.matching.MatchingEngine;
import kr.bokjakbokjak.survey.dto.SurveySubmitRequest;
import kr.bokjakbokjak.survey.entity.Question;
import kr.bokjakbokjak.survey.entity.QuestionKind;
import kr.bokjakbokjak.survey.entity.QuestionOption;
import kr.bokjakbokjak.survey.repository.QuestionOptionRepository;
import kr.bokjakbokjak.survey.repository.QuestionRepository;
import kr.bokjakbokjak.survey.service.MySurveyService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest
@ActiveProfiles("test")
class SurveyMatchingEngineTest {
    @Autowired MatchingEngine matchingEngine;
    @Autowired MySurveyService mySurveyService;
    @Autowired QuestionRepository questionRepository;
    @Autowired QuestionOptionRepository optionRepository;
    @Autowired JdbcClient jdbc;

    @DynamicPropertySource
    static void config(DynamicPropertyRegistry registry) {
        registry.add("app.jwt.secret-base64", () -> Base64.getEncoder().encodeToString(new byte[32]));
    }

    @Test
    void realEngineIsRegisteredByDefault() {
        assertThat(matchingEngine).isInstanceOf(SurveyMatchingEngine.class);
    }

    @Test
    void calculatesFromSavedSurveys() {
        long a = createUser();
        long b = createUser();
        submit(a, List.of("CLEANING", "SLEEP_SCHEDULE", "PERSONAL_HYGIENE"), Map.of("BEDTIME_T", "BY_01"));
        submit(b, List.of("SMOKING", "SLEEP_HABITS", "CLEANING"), Map.of("BEDTIME_H", "FROM_00_TO_02"));

        MatchResultDto result = matchingEngine.calculate(a, b);
        MatchResultDto reversed = matchingEngine.calculate(b, a);

        assertThat(result.viewerId()).isEqualTo(a);
        assertThat(result.items()).hasSize(8);
        assertThat(result.items().get(7).topicCode()).isEqualTo("TEMPERATURE");
        assertThat(result.items().get(1).topicCode()).isEqualTo("BEDTIME");
        assertThat(result.items().get(1).label()).isEqualTo(MatchLabel.NEED_ADJUST);
        assertThat(result.items().get(1).targetHabit()).isEqualTo("새벽 12시~2시");
        assertThat(result.viewerToTarget()).isLessThan(result.targetToViewer());
        assertThat(reversed.viewerToTarget()).isEqualByComparingTo(result.targetToViewer());
        assertThat(reversed.mutualScore()).isEqualByComparingTo(result.mutualScore());
    }

    @Test
    void rejectsSelfAndUnfinishedSurvey() {
        long done = createUser();
        long notYet = createUser();
        submit(done, List.of("CLEANING", "SLEEP_SCHEDULE", "PERSONAL_HYGIENE"), Map.of());

        assertThrows(IllegalArgumentException.class, () -> matchingEngine.calculate(done, done));
        assertThrows(IllegalArgumentException.class, () -> matchingEngine.calculate(done, notYet));
        assertThrows(IllegalArgumentException.class, () -> matchingEngine.calculate(done, -1L));
    }

    @Test
    void userWaitingForResurveyIsCalculatedWithOldAnswers() {
        long done = createUser();
        long waiting = createUser();
        submit(done, List.of("CLEANING", "SLEEP_SCHEDULE", "TEMPERATURE"), Map.of());
        submit(waiting, List.of("CLEANING", "SLEEP_SCHEDULE", "SMOKING"), Map.of());
        // 설문 개편 직후처럼 새 문항 답이 없고 설문 완료 시각이 비어 있는 상태
        jdbc.sql("delete from survey_answer where user_id = ? and question_code like 'TEMPERATURE%'")
                .param(waiting).update();
        jdbc.sql("update app_user set survey_completed_at = null where id = ?").param(waiting).update();

        MatchResultDto result = matchingEngine.calculate(done, waiting);

        var temperature = result.items().get(result.items().size() - 1);
        assertThat(temperature.topicCode()).isEqualTo("TEMPERATURE");
        assertThat(temperature.label()).isEqualTo(MatchLabel.NEED_CHECK);
        assertThat(temperature.targetHabit()).isEqualTo("아직 답하지 않았어요");
        assertThat(result.mutualScore()).isLessThan(java.math.BigDecimal.ONE);
    }

    @Test
    void seededPersonasCanBeMatched() {
        List<Long> personas = jdbc.sql("select id from app_user where test_user = TRUE order by id")
                .query(Long.class).list();

        for (Long persona : personas) {
            assertThat(jdbc.sql("select count(*) from survey_answer where user_id = ?")
                    .param(persona).query(Integer.class).single()).isEqualTo(16);
        }
        assertThatCode(() -> matchingEngine.calculate(personas.get(0), personas.get(1))).doesNotThrowAnyException();
    }

    @Test
    void everySeededOptionPairCanBeJudged() {
        Map<String, List<String>> optionsByQuestion = optionRepository.findAll().stream()
                .collect(Collectors.groupingBy(
                        QuestionOption::getQuestionCode,
                        Collectors.mapping(QuestionOption::getCode, Collectors.toList())));
        Map<String, Map<QuestionKind, String>> questionsByTopic = questionRepository.findAll().stream()
                .collect(Collectors.groupingBy(
                        Question::getTopicCode, Collectors.toMap(Question::getKind, Question::getCode)));

        questionsByTopic.forEach((topic, questions) -> {
            for (String habit : optionsByQuestion.get(questions.get(QuestionKind.HABIT))) {
                for (String tolerance : optionsByQuestion.get(questions.get(QuestionKind.TOLERANCE))) {
                    assertThatCode(() -> TopicRules.judge(topic, habit, tolerance)).doesNotThrowAnyException();
                }
            }
        });
    }

    private void submit(long userId, List<String> categories, Map<String, String> overrides) {
        Map<String, String> answers = new java.util.LinkedHashMap<>(Map.ofEntries(
                Map.entry("SLEEP_HABITS_H", "RARE"), Map.entry("SLEEP_HABITS_T", "OCCASIONAL_OK"),
                Map.entry("BEDTIME_H", "FROM_22_TO_24"), Map.entry("BEDTIME_T", "BY_01"),
                Map.entry("ALARM_H", "ONCE"), Map.entry("ALARM_T", "UP_TO_2"),
                Map.entry("FLOOR_CLEAN_H", "WEEKLY"), Map.entry("FLOOR_CLEAN_T", "WEEKLY_MIN"),
                Map.entry("FOOD_WASTE_H", "SAME_DAY"), Map.entry("FOOD_WASTE_T", "SAME_DAY_OK"),
                Map.entry("BELONGINGS_H", "IMMEDIATELY"), Map.entry("BELONGINGS_T", "SAME_DAY_OK"),
                Map.entry("SMOKING_H", "NON_SMOKER"), Map.entry("SMOKING_T", "ANY_OK"),
                Map.entry("TEMPERATURE_H", "LONG"), Map.entry("TEMPERATURE_T", "LONG_OK")));
        answers.putAll(overrides);

        List<SurveySubmitRequest.Priority> priorities = new java.util.ArrayList<>();
        for (int i = 0; i < categories.size(); i++) {
            priorities.add(new SurveySubmitRequest.Priority(categories.get(i), i + 1));
        }
        mySurveyService.submit(userId, new SurveySubmitRequest(0, priorities, answers.entrySet().stream()
                .map(entry -> new SurveySubmitRequest.Answer(entry.getKey(), entry.getValue()))
                .toList()));
    }

    private long createUser() {
        var keyHolder = new GeneratedKeyHolder();
        jdbc.sql("""
                        insert into app_user (onboarding_completed_at, survey_revision, created_at, updated_at)
                        values (CURRENT_TIMESTAMP, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)""")
                .update(keyHolder);
        return keyHolder.getKeyAs(Long.class);
    }
}
