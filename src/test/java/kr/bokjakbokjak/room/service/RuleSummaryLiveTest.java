package kr.bokjakbokjak.room.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Optional;
import kr.bokjakbokjak.ai.AzureOpenAiLlmClient;
import kr.bokjakbokjak.ai.AzureOpenAiProperties;
import kr.bokjakbokjak.ai.LlmJsonParser;
import kr.bokjakbokjak.room.service.RuleSummaryPrompt.ChatLine;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.json.JsonMapper;

@EnabledIfEnvironmentVariable(named = "AZURE_OPENAI_API_KEY", matches = ".+")
class RuleSummaryLiveTest {

    @Test
    void summarizesAgreedRule() {
        Optional<String> rule = generator().summarize("평일 소등 시간", "평일에는 몇 시에 방 불을 끌까요?", List.of(
                new ChatLine("A", "저는 보통 1시쯤 자는 편이에요"),
                new ChatLine("B", "저는 아침 수업이 있어서 12시 전에는 자야 해요"),
                new ChatLine("A", "그럼 평일엔 12시에 끄고 저는 스탠드 쓸게요"),
                new ChatLine("B", "좋아요! 그렇게 해요")));

        System.out.println("agreed: " + rule.orElse("(없음)"));
        assertThat(rule).isPresent();
        assertThat(rule.get()).contains("12시").doesNotContain("A", "B");
    }

    @Test
    void leavesEmptyWithoutAgreement() {
        Optional<String> rule = generator().summarize("아침 알람", "아침 알람은 몇 번까지 울리게 할까요?", List.of(
                new ChatLine("A", "저는 알람을 여러 번 맞춰야 일어나요"),
                new ChatLine("B", "음 저는 소리에 좀 예민한데 생각해볼게요")));

        System.out.println("no agreement: " + rule.orElse("(없음)"));
        assertThat(rule).isEmpty();
    }

    private static RuleSummaryGenerator generator() {
        var properties = new AzureOpenAiProperties(
                System.getenv("AZURE_OPENAI_ENDPOINT"),
                System.getenv("AZURE_OPENAI_API_KEY"),
                System.getenv("AZURE_OPENAI_DEPLOYMENT"),
                System.getenv("AZURE_OPENAI_API_VERSION"));
        var mapper = JsonMapper.builder().propertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE).build();
        return new RuleSummaryGenerator(new AzureOpenAiLlmClient(properties, RestClient.create(), mapper),
                new LlmJsonParser(mapper));
    }
}
