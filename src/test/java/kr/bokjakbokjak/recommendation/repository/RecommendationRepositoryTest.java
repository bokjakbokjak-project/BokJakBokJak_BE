package kr.bokjakbokjak.recommendation.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Base64;
import kr.bokjakbokjak.recommendation.entity.AiAnalysis;
import kr.bokjakbokjak.recommendation.entity.LifestyleSummary;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class RecommendationRepositoryTest {
    @Autowired AiAnalysisRepository aiAnalysisRepository;
    @Autowired LifestyleSummaryRepository lifestyleSummaryRepository;

    @DynamicPropertySource
    static void config(DynamicPropertyRegistry registry) {
        registry.add("app.jwt.secret-base64", () -> Base64.getEncoder().encodeToString(new byte[32]));
    }

    @Test
    void analysisIsStoredPerViewerAndTarget() {
        AiAnalysis saved = aiAnalysisRepository.saveAndFlush(analysis(7L, 12L));

        assertThat(aiAnalysisRepository.findByViewerIdAndTargetId(7L, 12L)).get()
                .satisfies(found -> {
                    assertThat(found.getId()).isEqualTo(saved.getId());
                    assertThat(found.getGoodPoints()).isEqualTo("[\"소등 시간이 비슷해요.\"]");
                    assertThat(found.getViewerSurveyRevision()).isEqualTo(1);
                    assertThat(found.getTargetSurveyRevision()).isEqualTo(3);
                    assertThat(found.getUpdatedAt()).isNotNull();
                });
        assertThat(aiAnalysisRepository.findByViewerIdAndTargetId(12L, 7L)).isEmpty();
    }

    @Test
    void duplicateAnalysisIsRejected() {
        aiAnalysisRepository.saveAndFlush(analysis(7L, 12L));

        assertThrows(DataIntegrityViolationException.class,
                () -> aiAnalysisRepository.saveAndFlush(analysis(7L, 12L)));
    }

    @Test
    void deletesAnalysesWhereUserIsViewerOrTarget() {
        aiAnalysisRepository.save(analysis(7L, 12L));
        aiAnalysisRepository.save(analysis(12L, 7L));
        aiAnalysisRepository.saveAndFlush(analysis(12L, 13L));

        assertThat(aiAnalysisRepository.deleteAllByUser(7L)).isEqualTo(2);
        assertThat(aiAnalysisRepository.findByViewerIdAndTargetId(12L, 13L)).isPresent();
    }

    @Test
    void lifestyleSummaryIsUniquePerUser() {
        lifestyleSummaryRepository.saveAndFlush(
                LifestyleSummary.create(7L, 1, "12시 전에 자요.", "[\"12시 전 취침\"]"));

        assertThat(lifestyleSummaryRepository.findByUserId(7L)).get()
                .extracting(LifestyleSummary::getSurveyRevision).isEqualTo(1);
        assertThrows(DataIntegrityViolationException.class, () -> lifestyleSummaryRepository.saveAndFlush(
                LifestyleSummary.create(7L, 2, "다른 요약", "[]")));
    }

    @Test
    void deletesLifestyleSummaryOfUser() {
        lifestyleSummaryRepository.save(LifestyleSummary.create(7L, 1, "요약", "[]"));
        lifestyleSummaryRepository.saveAndFlush(LifestyleSummary.create(12L, 1, "요약", "[]"));

        assertThat(lifestyleSummaryRepository.deleteAllByUserId(7L)).isEqualTo(1);
        assertThat(lifestyleSummaryRepository.findByUserId(7L)).isEmpty();
        assertThat(lifestyleSummaryRepository.findByUserId(12L)).isPresent();
    }

    private AiAnalysis analysis(Long viewerId, Long targetId) {
        return AiAnalysis.create(viewerId, targetId, 1, 3,
                "[\"소등 시간이 비슷해요.\"]", "[]", "[]");
    }
}
