package kr.bokjakbokjak.room.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Optional;
import kr.bokjakbokjak.ai.LlmClient;
import kr.bokjakbokjak.ai.LlmJsonParser;
import kr.bokjakbokjak.ai.LlmRequest;
import kr.bokjakbokjak.ai.ReasoningEffort;
import kr.bokjakbokjak.matching.MatchItemDto;
import kr.bokjakbokjak.matching.MatchLabel;
import kr.bokjakbokjak.matching.MatchResultDto;
import kr.bokjakbokjak.matching.MatchingEngine;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.json.JsonMapper;

class AgendaGeneratorTest {
    private static final String VALID = """
            {"agendas":[{"topic_code":"BEDTIME","title":"평일 소등 시간","content":"평일에는 몇 시에 방 불을 끌까요?"},\
            {"topic_code":"ALARM","title":"아침 알람","content":"아침 알람은 몇 번까지 울리게 할까요?"}]}""";

    @Test
    void picksTopicsNeedingAdjustmentWithHigherWeightFirst() {
        var llm = new ScriptedLlmClient(Optional.of(VALID));

        generator(llm, items()).generate(7L, 12L);

        assertThat(llm.requests).singleElement().satisfies(request -> {
            assertThat(request.reasoningEffort()).isEqualTo(ReasoningEffort.LOW);
            assertThat(request.systemPrompt()).contains("생활 규칙 조율을 돕는 사회자");
            assertThat(request.userPrompt()).isEqualTo("""
                    두 사람: A, B
                    1. topic_code: ALARM / 상황: 아침 알람이 울려요. / 판정: 조율 필요
                       - A 습관: 다섯 번 이상 / 허용: 한 번에 꺼 주면 좋겠다
                       - B 습관: 한 번에 끈다 / 허용: 두세 번까지 괜찮다
                    2. topic_code: BEDTIME / 상황: 평일 밤이에요. / 판정: 조율 필요
                       - A 습관: 새벽 1시 이후 / 허용: 12시 전에 꺼 주면 좋겠다
                       - B 습관: 11시 전 / 허용: 12시까지 괜찮다""");
        });
    }

    @Test
    void fillsWithWellMatchedTopicWhenFewTopicsNeedTalking() {
        List<MatchItemDto> items = List.of(
                item("FLOOR_CLEAN", MatchLabel.GOOD, "0.5"),
                item("DISHES", MatchLabel.NEED_CHECK, "0.1"),
                item("SMOKING", MatchLabel.GOOD, "0.9"));

        assertThat(AgendaTopicSelector.select(items)).extracting(MatchItemDto::topicCode)
                .containsExactly("DISHES", "SMOKING");
    }

    @Test
    void returnsAiAgendasInSelectedOrder() {
        var llm = new ScriptedLlmClient(Optional.of(VALID));

        List<GeneratedAgenda> agendas = generator(llm, items()).generate(7L, 12L);

        assertThat(agendas).containsExactly(
                new GeneratedAgenda("ALARM", "아침 알람", "아침 알람은 몇 번까지 울리게 할까요?"),
                new GeneratedAgenda("BEDTIME", "평일 소등 시간", "평일에는 몇 시에 방 불을 끌까요?"));
    }

    @Test
    void retriesOnceWhenAgendaIsMissingOrTooLong() {
        var llm = new ScriptedLlmClient(
                Optional.of("""
                        {"agendas":[{"topic_code":"ALARM","title":"아침 알람","content":"아침 알람은 몇 번까지 울리게 할까요?"},\
                        {"topic_code":"BEDTIME","title":"평일 소등 시간이라는 아주 길고 긴 제목입니다","content":"몇 시에 끌까요?"}]}"""),
                Optional.of(VALID));

        List<GeneratedAgenda> agendas = generator(llm, items()).generate(7L, 12L);

        assertThat(agendas).extracting(GeneratedAgenda::title).containsExactly("아침 알람", "평일 소등 시간");
        assertThat(llm.requests).hasSize(2);
    }

    @Test
    void fallsBackToDefaultAgendasAfterTwoFailures() {
        var llm = new ScriptedLlmClient(Optional.empty(), Optional.of("{\"agendas\":[]}"));

        List<GeneratedAgenda> agendas = generator(llm, items()).generate(7L, 12L);

        assertThat(agendas).containsExactly(
                new GeneratedAgenda("ALARM", "아침 알람", "아침 알람은 몇 번까지 울리게 할까요?"),
                new GeneratedAgenda("BEDTIME", "평일 소등 시간", "평일에는 몇 시에 방 불을 끌까요?"));
        assertThat(llm.requests).hasSize(2);
    }

    private static AgendaGenerator generator(LlmClient llmClient, List<MatchItemDto> items) {
        MatchingEngine engine = (viewerId, targetId) -> new MatchResultDto(viewerId, targetId,
                BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE, items);
        var mapper = JsonMapper.builder().propertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE).build();
        return new AgendaGenerator(engine, llmClient, new LlmJsonParser(mapper));
    }

    static List<MatchItemDto> items() {
        return List.of(
                item("FLOOR_CLEAN", MatchLabel.GOOD, "0.5"),
                new MatchItemDto("BEDTIME", "SLEEP_SCHEDULE", "평일 밤이에요.", BigDecimal.ZERO,
                        MatchLabel.NEED_ADJUST, new BigDecimal("0.1"), new BigDecimal("0.1"),
                        "새벽 1시 이후", "12시 전에 꺼 주면 좋겠다", "11시 전", "12시까지 괜찮다"),
                item("DISHES", MatchLabel.NEED_CHECK, "0.3"),
                new MatchItemDto("ALARM", "SLEEP_SCHEDULE", "아침 알람이 울려요.", BigDecimal.ZERO,
                        MatchLabel.NEED_ADJUST, new BigDecimal("0.2"), new BigDecimal("0.2"),
                        "다섯 번 이상", "한 번에 꺼 주면 좋겠다", "한 번에 끈다", "두세 번까지 괜찮다"));
    }

    private static MatchItemDto item(String topicCode, MatchLabel label, String weight) {
        return new MatchItemDto(topicCode, "CATEGORY", "상황", BigDecimal.ONE, label,
                new BigDecimal(weight), new BigDecimal(weight), "습관", "허용", "습관", "허용");
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
