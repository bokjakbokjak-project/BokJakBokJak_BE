package kr.bokjakbokjak.room.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Optional;
import kr.bokjakbokjak.ai.LlmJsonParser;
import kr.bokjakbokjak.ai.ReasoningEffort;
import kr.bokjakbokjak.room.service.AgendaGeneratorTest.ScriptedLlmClient;
import kr.bokjakbokjak.room.service.RuleSummaryPrompt.ChatLine;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.json.JsonMapper;

class RuleSummaryGeneratorTest {
    private static final List<ChatLine> CHAT = List.of(
            new ChatLine("A", "저는 보통 1시쯤 자요"),
            new ChatLine("B", "평일엔 12시에 끄면 어때요?"),
            new ChatLine("A", "좋아요 그렇게 해요"));

    @Test
    void sendsAgendaAndChatWithoutNicknames() {
        var llm = new ScriptedLlmClient(Optional.of("{\"rule\":\"평일에는 밤 12시에 불을 끄기로 했어요.\"}"));

        Optional<String> rule = generator(llm).summarize("평일 소등 시간", "평일에는 몇 시에 방 불을 끌까요?", CHAT);

        assertThat(rule).contains("평일에는 밤 12시에 불을 끄기로 했어요.");
        assertThat(llm.requests).singleElement().satisfies(request -> {
            assertThat(request.reasoningEffort()).isEqualTo(ReasoningEffort.LOW);
            assertThat(request.userPrompt()).isEqualTo("""
                    안건: 평일 소등 시간 / 평일에는 몇 시에 방 불을 끌까요?
                    대화
                    A: 저는 보통 1시쯤 자요
                    B: 평일엔 12시에 끄면 어때요?
                    A: 좋아요 그렇게 해요""");
        });
    }

    @Test
    void noAgreementIsLeftEmptyWithoutRetry() {
        var llm = new ScriptedLlmClient(Optional.of("{\"rule\":null}"));

        assertThat(generator(llm).summarize("평일 소등 시간", "몇 시에 끌까요?", CHAT)).isEmpty();
        assertThat(llm.requests).hasSize(1);
    }

    @Test
    void retriesInvalidAnswerOnce() {
        var llm = new ScriptedLlmClient(
                Optional.of("{\"rule\":\"" + "가".repeat(61) + "\"}"),
                Optional.of("{\"rule\":\"평일에는 밤 12시에 불을 끄기로 했어요.\"}"));

        assertThat(generator(llm).summarize("평일 소등 시간", "몇 시에 끌까요?", CHAT))
                .contains("평일에는 밤 12시에 불을 끄기로 했어요.");
        assertThat(llm.requests).hasSize(2);
    }

    @Test
    void givesUpAfterTwoFailures() {
        var llm = new ScriptedLlmClient(Optional.empty(), Optional.of("not json"));

        assertThat(generator(llm).summarize("평일 소등 시간", "몇 시에 끌까요?", CHAT)).isEmpty();
        assertThat(llm.requests).hasSize(2);
    }

    @Test
    void noChatMeansNoCall() {
        var llm = new ScriptedLlmClient();

        assertThat(generator(llm).summarize("평일 소등 시간", "몇 시에 끌까요?", List.of())).isEmpty();
        assertThat(llm.requests).isEmpty();
    }

    private static RuleSummaryGenerator generator(ScriptedLlmClient llm) {
        var mapper = JsonMapper.builder().propertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE).build();
        return new RuleSummaryGenerator(llm, new LlmJsonParser(mapper));
    }
}
