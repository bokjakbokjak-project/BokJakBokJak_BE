package kr.bokjakbokjak.recommendation.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Base64;
import java.util.Optional;
import kr.bokjakbokjak.ai.LlmClient;
import kr.bokjakbokjak.common.ApiException;
import kr.bokjakbokjak.matching.SurveyProfileDto;
import kr.bokjakbokjak.matching.SurveyProfileQueryService;
import kr.bokjakbokjak.recommendation.dto.LifestyleSummaryResponse;
import kr.bokjakbokjak.recommendation.repository.LifestyleSummaryRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
@ActiveProfiles("test")
class LifestyleSummaryServiceTest {
    private static final Long USER = 7L;
    private static final String AI_RESPONSE = """
            {"summary":"거의 매일 바닥을 청소해요. 첫 알람에 바로 일어나요.","keywords":["매일 청소","첫 알람","비흡연"]}""";

    @MockitoBean SurveyProfileQueryService surveyProfileQueryService;
    @MockitoBean LlmClient llmClient;
    @Autowired LifestyleSummaryService lifestyleSummaryService;
    @Autowired LifestyleSummaryRepository repository;

    @DynamicPropertySource
    static void config(DynamicPropertyRegistry registry) {
        registry.add("app.jwt.secret-base64", () -> Base64.getEncoder().encodeToString(new byte[32]));
    }

    @AfterEach
    void cleanUp() {
        repository.deleteAll();
    }

    @Test
    void generatesOnceAndReusesWhileRevisionIsSame() {
        when(surveyProfileQueryService.getSurveyProfile(USER)).thenReturn(profile(2));
        when(llmClient.complete(any())).thenReturn(Optional.of(AI_RESPONSE));

        LifestyleSummaryResponse first = lifestyleSummaryService.getSummary(USER);
        LifestyleSummaryResponse second = lifestyleSummaryService.getSummary(USER);

        assertThat(first.userId()).isEqualTo(USER);
        assertThat(first.summary()).isEqualTo("거의 매일 바닥을 청소해요. 첫 알람에 바로 일어나요.");
        assertThat(first.keywords()).containsExactly("매일 청소", "첫 알람", "비흡연");
        assertThat(first.generatedAt()).isNotNull();
        assertThat(second.summary()).isEqualTo(first.summary());
        assertThat(second.keywords()).isEqualTo(first.keywords());
        verify(llmClient, times(1)).complete(any());
    }

    @Test
    void regeneratesWhenRevisionChanges() {
        when(llmClient.complete(any())).thenReturn(Optional.of(AI_RESPONSE));
        when(surveyProfileQueryService.getSurveyProfile(USER)).thenReturn(profile(2));
        lifestyleSummaryService.getSummary(USER);

        when(surveyProfileQueryService.getSurveyProfile(USER)).thenReturn(profile(3));
        lifestyleSummaryService.getSummary(USER);

        verify(llmClient, times(2)).complete(any());
        assertThat(repository.findAll()).singleElement()
                .satisfies(saved -> assertThat(saved.getSurveyRevision()).isEqualTo(3));
    }

    @Test
    void fallbackIsReturnedButNotSaved() {
        when(surveyProfileQueryService.getSurveyProfile(USER)).thenReturn(profile(2));
        when(llmClient.complete(any())).thenReturn(Optional.empty());

        LifestyleSummaryResponse response = lifestyleSummaryService.getSummary(USER);

        assertThat(response.summary()).isEqualTo("청소 청결, 기상 알람, 흡연 유무 순서로 생활습관을 중요하게 생각해요.");
        assertThat(response.keywords()).containsExactly("청소 청결", "기상 알람", "흡연 유무");
        assertThat(repository.findByUserId(USER)).isEmpty();
    }

    @Test
    void missingOrIncompleteUserIsTargetWithdrawn() {
        when(surveyProfileQueryService.getSurveyProfile(USER)).thenThrow(new IllegalArgumentException("no survey"));

        ApiException exception = assertThrows(ApiException.class, () -> lifestyleSummaryService.getSummary(USER));

        assertThat(exception.code()).isEqualTo("TARGET_WITHDRAWN");
        assertThat(exception.status()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    private static SurveyProfileDto profile(int revision) {
        SurveyProfileDto base = LifestyleSummaryGeneratorTest.profile();
        return new SurveyProfileDto(USER, revision, base.topCategories(), base.topics());
    }
}
