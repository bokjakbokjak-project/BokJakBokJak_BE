package kr.bokjakbokjak.room.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;
import kr.bokjakbokjak.ai.AzureOpenAiLlmClient;
import kr.bokjakbokjak.ai.AzureOpenAiProperties;
import kr.bokjakbokjak.ai.LlmJsonParser;
import kr.bokjakbokjak.matching.MatchItemDto;
import kr.bokjakbokjak.matching.MatchLabel;
import kr.bokjakbokjak.matching.MatchResultDto;
import kr.bokjakbokjak.matching.MatchingEngine;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.json.JsonMapper;

@EnabledIfEnvironmentVariable(named = "AZURE_OPENAI_API_KEY", matches = ".+")
class AgendaGeneratorLiveTest {

    @Test
    void generatesAgendasWithRealAzureOpenAi() {
        var properties = new AzureOpenAiProperties(
                System.getenv("AZURE_OPENAI_ENDPOINT"),
                System.getenv("AZURE_OPENAI_API_KEY"),
                System.getenv("AZURE_OPENAI_DEPLOYMENT"),
                System.getenv("AZURE_OPENAI_API_VERSION"));
        var mapper = JsonMapper.builder().propertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE).build();
        MatchingEngine engine = (viewerId, targetId) -> new MatchResultDto(viewerId, targetId,
                BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE, items());
        var generator = new AgendaGenerator(engine,
                new AzureOpenAiLlmClient(properties, RestClient.create(), mapper), new LlmJsonParser(mapper));

        long started = System.currentTimeMillis();
        List<GeneratedAgenda> agendas = generator.generate(1L, 2L);
        long elapsed = System.currentTimeMillis() - started;

        System.out.println("elapsed ms: " + elapsed);
        agendas.forEach(agenda -> System.out.println(agenda.topicCode() + " | " + agenda.title() + " | " + agenda.content()));
        assertThat(agendas).extracting(GeneratedAgenda::topicCode).containsExactly("BEDTIME", "SMOKING");
        assertThat(agendas).extracting(GeneratedAgenda::content).allMatch(content -> content.endsWith("까요?"));
    }

    private static List<MatchItemDto> items() {
        return List.of(
                item("BEDTIME", "SLEEP_SCHEDULE", "평일 밤, 다음 날 오전 수업이 있어요.", MatchLabel.NEED_ADJUST, "0.3",
                        "새벽 1시 이후에 잔다", "12시 전에는 불을 꺼 주면 좋겠다", "11시 전에 잔다", "12시까지는 괜찮다"),
                item("SMOKING", "SMOKING", "룸메이트가 흡연을 해요.", MatchLabel.NEED_ADJUST, "0.2",
                        "밖에서 피운다", "상관없다", "비흡연자다", "냄새가 안 나면 괜찮다"),
                item("DISHES", "PERSONAL_HYGIENE", "방 안에 사용한 컵이 놓여 있어요.", MatchLabel.GOOD, "0.4",
                        "그날 안에 정리한다", "다음 날까지는 괜찮다", "바로 정리한다", "그날 안에만 정리하면 괜찮다"));
    }

    private static MatchItemDto item(String topicCode, String categoryCode, String scenario, MatchLabel label,
            String weight, String viewerHabit, String viewerTolerance, String targetHabit, String targetTolerance) {
        return new MatchItemDto(topicCode, categoryCode, scenario, BigDecimal.ONE, label,
                new BigDecimal(weight), new BigDecimal(weight),
                viewerHabit, viewerTolerance, targetHabit, targetTolerance);
    }
}
