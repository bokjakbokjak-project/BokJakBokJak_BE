package kr.bokjakbokjak.survey.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import kr.bokjakbokjak.matching.SurveyProfileDto;
import kr.bokjakbokjak.matching.SurveyProfileQueryService;
import kr.bokjakbokjak.survey.dto.SurveySubmitRequest;
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
class SurveyProfileServiceTest {
    @Autowired SurveyProfileQueryService surveyProfileQueryService;
    @Autowired MySurveyService mySurveyService;
    @Autowired JdbcClient jdbc;

    @DynamicPropertySource
    static void config(DynamicPropertyRegistry registry) {
        registry.add("app.jwt.secret-base64", () -> Base64.getEncoder().encodeToString(new byte[32]));
    }

    @Test
    void returnsTopThreeCategoriesAndTopicSentences() {
        long userId = createUser();
        submit(userId, 0, List.of("SMOKING", "CLEANING", "SLEEP_SCHEDULE", "PERSONAL_HYGIENE"),
                Map.of("BEDTIME_H", "FROM_00_TO_01"));

        SurveyProfileDto profile = surveyProfileQueryService.getSurveyProfile(userId);

        assertThat(profile.userId()).isEqualTo(userId);
        assertThat(profile.surveyRevision()).isEqualTo(1);
        assertThat(profile.topCategories())
                .extracting(SurveyProfileDto.CategoryRank::code, SurveyProfileDto.CategoryRank::name,
                        SurveyProfileDto.CategoryRank::rank)
                .containsExactly(
                        tuple("SMOKING", "흡연 유무", 1),
                        tuple("CLEANING", "청소 청결", 2),
                        tuple("SLEEP_SCHEDULE", "기상 알람", 3));

        assertThat(profile.topics())
                .extracting(SurveyProfileDto.TopicAnswer::topicCode)
                .containsExactly("SLEEP_HABITS", "BEDTIME", "ALARM", "FLOOR_CLEAN",
                        "FOOD_WASTE", "BELONGINGS", "DISHES", "SMOKING");

        SurveyProfileDto.TopicAnswer bedtime = profile.topics().get(1);
        assertThat(bedtime.categoryCode()).isEqualTo("SLEEP_SCHEDULE");
        assertThat(bedtime.categoryName()).isEqualTo("기상 알람");
        assertThat(bedtime.scenarioText()).startsWith("밤 12시가 되었어요.");
        assertThat(bedtime.habitQuestion()).isEqualTo("평일에 나는 보통 몇 시쯤 잠자리에 드나요?");
        assertThat(bedtime.habitAnswer()).isEqualTo("새벽 12시~1시");
        assertThat(bedtime.toleranceQuestion())
                .isEqualTo("룸메이트가 방 안에서 깨어 있어도 괜찮은 마감 시간은 언제까지인가요?");
        assertThat(bedtime.toleranceAnswer()).isEqualTo("새벽 1시까지");

        SurveyProfileDto.TopicAnswer smoking = profile.topics().get(7);
        assertThat(smoking.habitAnswer()).isEqualTo("비흡연자다");
        assertThat(smoking.toleranceAnswer()).isEqualTo("흡연 여부는 상관없다");
    }

    @Test
    void reflectsLatestRevisionAfterResubmit() {
        long userId = createUser();
        submit(userId, 0, List.of("CLEANING", "SLEEP_SCHEDULE", "PERSONAL_HYGIENE"), Map.of());
        submit(userId, 1, List.of("SLEEP_HABITS", "CLEANING", "SMOKING"), Map.of("BEDTIME_T", "BY_23"));

        SurveyProfileDto profile = surveyProfileQueryService.getSurveyProfile(userId);

        assertThat(profile.surveyRevision()).isEqualTo(2);
        assertThat(profile.topCategories())
                .extracting(SurveyProfileDto.CategoryRank::code)
                .containsExactly("SLEEP_HABITS", "CLEANING", "SMOKING");
        assertThat(profile.topics().get(1).toleranceAnswer()).isEqualTo("밤 11시까지");
    }

    @Test
    void rejectsUnknownUserAndUnfinishedSurvey() {
        long notYet = createUser();

        assertThrows(IllegalArgumentException.class, () -> surveyProfileQueryService.getSurveyProfile(notYet));
        assertThrows(IllegalArgumentException.class, () -> surveyProfileQueryService.getSurveyProfile(-1L));
    }

    private void submit(long userId, int expectedRevision, List<String> categories, Map<String, String> overrides) {
        Map<String, String> answers = new LinkedHashMap<>(Map.ofEntries(
                Map.entry("SLEEP_HABITS_H", "RARE"), Map.entry("SLEEP_HABITS_T", "OCCASIONAL_OK"),
                Map.entry("BEDTIME_H", "FROM_23_TO_24"), Map.entry("BEDTIME_T", "BY_01"),
                Map.entry("ALARM_H", "ONCE"), Map.entry("ALARM_T", "UP_TO_2"),
                Map.entry("FLOOR_CLEAN_H", "WEEKLY"), Map.entry("FLOOR_CLEAN_T", "WEEKLY_MIN"),
                Map.entry("FOOD_WASTE_H", "SAME_DAY"), Map.entry("FOOD_WASTE_T", "SAME_DAY_OK"),
                Map.entry("BELONGINGS_H", "IMMEDIATELY"), Map.entry("BELONGINGS_T", "SAME_DAY_OK"),
                Map.entry("DISHES_H", "SAME_DAY"), Map.entry("DISHES_T", "SAME_DAY_OK"),
                Map.entry("SMOKING_H", "NON_SMOKER"), Map.entry("SMOKING_T", "ANY_OK")));
        answers.putAll(overrides);

        List<SurveySubmitRequest.Priority> priorities = new ArrayList<>();
        for (int i = 0; i < categories.size(); i++) {
            priorities.add(new SurveySubmitRequest.Priority(categories.get(i), i + 1));
        }
        mySurveyService.submit(userId, new SurveySubmitRequest(expectedRevision, priorities, answers.entrySet().stream()
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
