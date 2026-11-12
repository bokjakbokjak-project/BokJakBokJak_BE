package kr.bokjakbokjak.recommendation.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import kr.bokjakbokjak.ai.LlmJsonParser;
import kr.bokjakbokjak.ai.ReasoningEffort;
import kr.bokjakbokjak.matching.MatchItemDto;
import kr.bokjakbokjak.matching.MatchLabel;
import kr.bokjakbokjak.recommendation.service.LifestyleSummaryGeneratorTest.ScriptedLlmClient;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.json.JsonMapper;

class MatchAnalysisGeneratorTest {
    static final String VALID = """
            {"items":[\
            {"topic_code":"FLOOR_CLEAN","sentence":"두 분 모두 바닥 청소를 자주 해서 청결 기준이 비슷해요."},\
            {"topic_code":"DISHES","sentence":"식기를 치우는 시점이 달라요. 언제까지 둘지 미리 정해 두면 좋아요."},\
            {"topic_code":"SLEEP_HABITS","sentence":"상대가 잠버릇 질문에 '잘 모르겠다'고 답했어요. 대화에서 확인해 보세요."},\
            {"topic_code":"BEDTIME","sentence":"두 분 모두 비슷한 시간에 잠들어서 소등 시간이 비슷해요."}]}""";

    @Test
    void sendsTopicsWithoutNicknames() {
        var llm = new ScriptedLlmClient(Optional.of(VALID));

        generator(llm).generate(items());

        assertThat(llm.requests).singleElement().satisfies(request -> {
            assertThat(request.reasoningEffort()).isEqualTo(ReasoningEffort.LOW);
            assertThat(request.systemPrompt()).contains("룸메이트 매칭 결과를 설명하는 도우미");
            assertThat(request.userPrompt()).startsWith("""
                    1. topic_code: FLOOR_CLEAN / 상황: 바닥에 먼지가 보여요. / 판정: 잘 맞음
                    - 나 습관: 매일 / 허용: 주 1회
                    - 상대 습관: 주 2~3회 / 허용: 주 1회
                    2. topic_code: DISHES""");
        });
    }

    @Test
    void classifiesSentencesByEngineLabelInViewerPriority() {
        GeneratedMatchAnalysis analysis = generator(new ScriptedLlmClient(Optional.of(VALID))).generate(items());

        assertThat(analysis.aiGenerated()).isTrue();
        assertThat(analysis.goodPoints()).containsExactly(
                "두 분 모두 비슷한 시간에 잠들어서 소등 시간이 비슷해요.",
                "두 분 모두 바닥 청소를 자주 해서 청결 기준이 비슷해요.");
        assertThat(analysis.adjustPoints()).containsExactly("식기를 치우는 시점이 달라요. 언제까지 둘지 미리 정해 두면 좋아요.");
        assertThat(analysis.checkPoints()).containsExactly("상대가 잠버릇 질문에 '잘 모르겠다'고 답했어요. 대화에서 확인해 보세요.");
    }

    @Test
    void retriesWhenTopicIsMissing() {
        var llm = new ScriptedLlmClient(
                Optional.of("{\"items\":[{\"topic_code\":\"FLOOR_CLEAN\",\"sentence\":\"비슷해요.\"}]}"),
                Optional.of(VALID));

        GeneratedMatchAnalysis analysis = generator(llm).generate(items());

        assertThat(analysis.aiGenerated()).isTrue();
        assertThat(llm.requests).hasSize(2);
    }

    @Test
    void fallsBackToDefaultSentencesAfterTwoFailures() {
        var llm = new ScriptedLlmClient(Optional.empty(), Optional.of("not json"));

        GeneratedMatchAnalysis analysis = generator(llm).generate(items());

        assertThat(analysis.aiGenerated()).isFalse();
        assertThat(analysis.goodPoints()).containsExactly("잠드는 시간 기준이 비슷해요.", "바닥 청소 기준이 비슷해요.");
        assertThat(analysis.adjustPoints()).containsExactly("식기 정리 기준이 서로 달라요. 대화에서 미리 정해 두면 좋아요.");
        assertThat(analysis.checkPoints()).containsExactly("잠버릇 질문에 '잘 모르겠다'는 답이 있어요. 대화에서 확인해 보세요.");
    }

    @Test
    void noTopicsMeansNoAiCall() {
        var llm = new ScriptedLlmClient(Optional.of(VALID));

        GeneratedMatchAnalysis analysis = generator(llm).generate(List.of());

        assertThat(analysis.goodPoints()).isEmpty();
        assertThat(llm.requests).isEmpty();
    }

    private static MatchAnalysisGenerator generator(ScriptedLlmClient llm) {
        var mapper = JsonMapper.builder().propertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE).build();
        return new MatchAnalysisGenerator(llm, new LlmJsonParser(mapper));
    }

    static List<MatchItemDto> items() {
        return List.of(
                item("FLOOR_CLEAN", "바닥에 먼지가 보여요.", MatchLabel.GOOD, "0.3",
                        "매일", "주 1회", "주 2~3회", "주 1회"),
                item("DISHES", "방 안에 컵이 놓여 있어요.", MatchLabel.NEED_ADJUST, "0.1",
                        "바로 정리", "그날 안에", "다음 날", "괜찮다"),
                item("SLEEP_HABITS", "룸메이트가 자고 있어요.", MatchLabel.NEED_CHECK, "0.2",
                        "없다", "없으면 좋겠다", "잘 모르겠다", "상관없다"),
                item("BEDTIME", "평일 밤이에요.", MatchLabel.GOOD, "0.5",
                        "12시 전", "1시까지", "12시 전", "1시까지"));
    }

    private static MatchItemDto item(String topicCode, String scenario, MatchLabel label, String viewerWeight,
            String viewerHabit, String viewerTolerance, String targetHabit, String targetTolerance) {
        return new MatchItemDto(topicCode, "CATEGORY", scenario, BigDecimal.ONE, label,
                new BigDecimal(viewerWeight), BigDecimal.ONE, viewerHabit, viewerTolerance, targetHabit,
                targetTolerance);
    }
}
