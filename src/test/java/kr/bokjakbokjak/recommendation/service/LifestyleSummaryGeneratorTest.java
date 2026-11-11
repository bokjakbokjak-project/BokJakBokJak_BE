package kr.bokjakbokjak.recommendation.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Optional;
import kr.bokjakbokjak.ai.LlmClient;
import kr.bokjakbokjak.ai.LlmJsonParser;
import kr.bokjakbokjak.ai.LlmRequest;
import kr.bokjakbokjak.ai.ReasoningEffort;
import kr.bokjakbokjak.matching.SurveyProfileDto;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

class LifestyleSummaryGeneratorTest {
    private static final String VALID = """
            {"summary":"거의 매일 바닥을 청소해요. 첫 알람에 바로 일어나요.","keywords":["매일 청소","첫 알람","비흡연"]}""";

    @Test
    void returnsValidAiSummary() {
        var llm = new ScriptedLlmClient(Optional.of(VALID));

        GeneratedLifestyleSummary result = generator(llm).generate(profile());

        assertThat(result.aiGenerated()).isTrue();
        assertThat(result.summary()).isEqualTo("거의 매일 바닥을 청소해요. 첫 알람에 바로 일어나요.");
        assertThat(result.keywords()).containsExactly("매일 청소", "첫 알람", "비흡연");
        assertThat(llm.requests).singleElement().satisfies(request -> {
            assertThat(request.reasoningEffort()).isEqualTo(ReasoningEffort.MINIMAL);
            assertThat(request.systemPrompt()).contains("생활습관 설문을 짧게 소개하는 도우미");
            assertThat(request.userPrompt()).startsWith("중요 카테고리: 1순위 청소 청결, 2순위 기상 알람, 3순위 흡연 유무");
            assertThat(request.userPrompt()).contains("1. 청소 청결 / 바닥을 얼마나 자주 청소하나요? → 습관: 거의 매일 한다 / 허용: 주 1회 이상이면 괜찮다");
        });
    }

    @Test
    void cleansKeywords() {
        var llm = new ScriptedLlmClient(Optional.of("""
                {"summary":"요약이에요.","keywords":[" 매일 청소 ","매일 청소","열 글자가 넘는 아주 긴 키워드","첫 알람","비흡연","그날 정리","알람 1번",""]}"""));

        GeneratedLifestyleSummary result = generator(llm).generate(profile());

        assertThat(result.keywords()).containsExactly("매일 청소", "첫 알람", "비흡연", "그날 정리", "알람 1번");
    }

    @Test
    void retriesOnceWhenFirstResponseIsInvalid() {
        var llm = new ScriptedLlmClient(Optional.of("{\"summary\":\"\",\"keywords\":[]}"), Optional.of(VALID));

        GeneratedLifestyleSummary result = generator(llm).generate(profile());

        assertThat(result.aiGenerated()).isTrue();
        assertThat(llm.requests).hasSize(2);
    }

    @Test
    void fallsBackToCategoriesAfterTwoFailures() {
        var llm = new ScriptedLlmClient(Optional.empty(), Optional.of("{\"summary\":\"요약\",\"keywords\":[\"하나\"]}"));

        GeneratedLifestyleSummary result = generator(llm).generate(profile());

        assertThat(result.aiGenerated()).isFalse();
        assertThat(result.summary()).isEqualTo("청소 청결, 기상 알람, 흡연 유무 순서로 생활습관을 중요하게 생각해요.");
        assertThat(result.keywords()).containsExactly("청소 청결", "기상 알람", "흡연 유무");
        assertThat(llm.requests).hasSize(2);
    }

    private static LifestyleSummaryGenerator generator(LlmClient llmClient) {
        return new LifestyleSummaryGenerator(llmClient, new LlmJsonParser(JsonMapper.builder().build()));
    }

    static SurveyProfileDto profile() {
        return new SurveyProfileDto(7L, 2,
                List.of(new SurveyProfileDto.CategoryRank("CLEANING", "청소 청결", 1),
                        new SurveyProfileDto.CategoryRank("SLEEP_SCHEDULE", "기상 알람", 2),
                        new SurveyProfileDto.CategoryRank("SMOKING", "흡연 유무", 3)),
                List.of(new SurveyProfileDto.TopicAnswer("FLOOR_CLEAN", "CLEANING", "청소 청결",
                                "바닥에 먼지가 보여요.", "바닥을 얼마나 자주 청소하나요?", "거의 매일 한다",
                                "룸메이트는 얼마나 자주 청소하면 좋을까요?", "주 1회 이상이면 괜찮다"),
                        new SurveyProfileDto.TopicAnswer("ALARM", "SLEEP_SCHEDULE", "기상 알람",
                                "아침 알람이 울려요.", "알람을 몇 번 만에 끄나요?", "첫 알람에 바로 일어난다",
                                "룸메이트 알람은 몇 번까지 괜찮나요?", "두세 번까지는 괜찮다")));
    }

    static final class ScriptedLlmClient implements LlmClient {
        private final Deque<Optional<String>> responses;
        final List<LlmRequest> requests = new ArrayList<>();

        @SafeVarargs
        ScriptedLlmClient(Optional<String>... responses) {
            this.responses = new ArrayDeque<>(List.of(responses));
        }

        @Override
        public Optional<String> complete(LlmRequest request) {
            requests.add(request);
            return responses.isEmpty() ? Optional.empty() : responses.poll();
        }
    }
}
