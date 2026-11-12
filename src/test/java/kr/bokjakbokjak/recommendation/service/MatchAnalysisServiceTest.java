package kr.bokjakbokjak.recommendation.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.math.BigDecimal;
import java.time.Year;
import java.time.ZoneId;
import java.util.Base64;
import java.util.Optional;
import kr.bokjakbokjak.ai.LlmClient;
import kr.bokjakbokjak.common.ApiException;
import kr.bokjakbokjak.matching.MatchResultDto;
import kr.bokjakbokjak.matching.MatchingEngine;
import kr.bokjakbokjak.recommendation.dto.MatchAnalysisResponse;
import kr.bokjakbokjak.recommendation.repository.AiAnalysisRepository;
import kr.bokjakbokjak.support.FakeUserQueryService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
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
class MatchAnalysisServiceTest {
    private static final long VIEWER = 7L;
    private static final long MINT = 12L;

    @MockitoBean MatchingEngine matchingEngine;
    @MockitoBean LlmClient llmClient;

    @Autowired MatchAnalysisService matchAnalysisService;
    @Autowired FakeUserQueryService users;
    @Autowired AiAnalysisRepository aiAnalysisRepository;

    @DynamicPropertySource
    static void config(DynamicPropertyRegistry registry) {
        registry.add("app.jwt.secret-base64", () -> Base64.getEncoder().encodeToString(new byte[32]));
    }

    @BeforeEach
    void setUp() {
        users.clear();
        users.addUser(VIEWER, "책갈피", 1);
        users.addUser(MINT, "민트초코", 1);
        given(matchingEngine.calculate(anyLong(), anyLong())).willAnswer(invocation -> new MatchResultDto(
                invocation.getArgument(0), invocation.getArgument(1), new BigDecimal("0.8749"),
                BigDecimal.ONE, BigDecimal.ONE, MatchAnalysisGeneratorTest.items()));
        given(llmClient.complete(any())).willReturn(Optional.of(MatchAnalysisGeneratorTest.VALID));
    }

    @AfterEach
    void cleanUp() {
        aiAnalysisRepository.deleteAll();
        users.clear();
    }

    @Test
    void analyzesAndSavesForNextTime() {
        MatchAnalysisResponse first = matchAnalysisService.getAnalysis(VIEWER, MINT);
        MatchAnalysisResponse second = matchAnalysisService.getAnalysis(VIEWER, MINT);

        assertThat(first.target().userId()).isEqualTo(MINT);
        assertThat(first.target().nickname()).isEqualTo("민트초코");
        assertThat(first.target().age()).isEqualTo(Year.now(ZoneId.of("Asia/Seoul")).getValue() - 2005 + 1);
        assertThat(first.target().department()).isEqualTo("컴퓨터공학과");
        assertThat(first.matchScore()).isEqualTo(87);
        assertThat(first.goodPoints()).hasSize(2);
        assertThat(first.adjustPoints()).hasSize(1);
        assertThat(first.checkPoints()).hasSize(1);
        assertThat(first.analyzedAt()).isNotNull();
        assertThat(second.goodPoints()).isEqualTo(first.goodPoints());
        assertThat(aiAnalysisRepository.count()).isEqualTo(1);
        verify(llmClient, times(1)).complete(any());
    }

    @Test
    void surveyChangeCreatesNewAnalysis() {
        matchAnalysisService.getAnalysis(VIEWER, MINT);
        users.addUser(MINT, "민트초코", 2);

        matchAnalysisService.getAnalysis(VIEWER, MINT);

        verify(llmClient, times(2)).complete(any());
        assertThat(aiAnalysisRepository.findByViewerIdAndTargetId(VIEWER, MINT).orElseThrow()
                .getTargetSurveyRevision()).isEqualTo(2);
        assertThat(aiAnalysisRepository.count()).isEqualTo(1);
    }

    @Test
    void defaultSentencesAreNotSaved() {
        given(llmClient.complete(any())).willReturn(Optional.empty());

        MatchAnalysisResponse response = matchAnalysisService.getAnalysis(VIEWER, MINT);

        assertThat(response.adjustPoints()).containsExactly("식기 정리 기준이 서로 달라요. 대화에서 미리 정해 두면 좋아요.");
        assertThat(aiAnalysisRepository.count()).isZero();
    }

    @Test
    void viewerMustCompleteSurvey() {
        users.addUser(VIEWER, "책갈피", 0);

        assertRejected(MINT, "ONBOARDING_REQUIRED", HttpStatus.CONFLICT);
    }

    @Test
    void withdrawnOrMissingTargetIsNotFound() {
        users.withdraw(MINT);

        assertRejected(MINT, "TARGET_WITHDRAWN", HttpStatus.NOT_FOUND);
        assertRejected(999L, "TARGET_WITHDRAWN", HttpStatus.NOT_FOUND);
    }

    @Test
    void selfOrTargetWithoutSurveyCannotBeAnalyzed() {
        users.addUser(MINT, "민트초코", 0);

        assertRejected(VIEWER, "TARGET_NOT_ELIGIBLE", HttpStatus.FORBIDDEN);
        assertRejected(MINT, "TARGET_NOT_ELIGIBLE", HttpStatus.FORBIDDEN);
    }

    private void assertRejected(long targetId, String code, HttpStatus status) {
        ApiException exception = assertThrows(ApiException.class,
                () -> matchAnalysisService.getAnalysis(VIEWER, targetId));
        assertThat(exception.code()).isEqualTo(code);
        assertThat(exception.status()).isEqualTo(status);
    }
}
