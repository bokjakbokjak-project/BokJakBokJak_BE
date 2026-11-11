package kr.bokjakbokjak.recommendation.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import kr.bokjakbokjak.ai.AzureOpenAiLlmClient;
import kr.bokjakbokjak.ai.AzureOpenAiProperties;
import kr.bokjakbokjak.ai.LlmJsonParser;
import kr.bokjakbokjak.matching.SurveyProfileDto;
import kr.bokjakbokjak.matching.SurveyProfileDto.CategoryRank;
import kr.bokjakbokjak.matching.SurveyProfileDto.TopicAnswer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.json.JsonMapper;

@EnabledIfEnvironmentVariable(named = "AZURE_OPENAI_API_KEY", matches = ".+")
class LifestyleSummaryLiveTest {

    @Test
    void generatesSummaryWithRealAzureOpenAi() {
        var properties = new AzureOpenAiProperties(
                System.getenv("AZURE_OPENAI_ENDPOINT"),
                System.getenv("AZURE_OPENAI_API_KEY"),
                System.getenv("AZURE_OPENAI_DEPLOYMENT"),
                System.getenv("AZURE_OPENAI_API_VERSION"));
        var mapper = JsonMapper.builder().build();
        var generator = new LifestyleSummaryGenerator(
                new AzureOpenAiLlmClient(properties, RestClient.create(), mapper), new LlmJsonParser(mapper));

        long started = System.currentTimeMillis();
        GeneratedLifestyleSummary result = generator.generate(profile());
        long elapsed = System.currentTimeMillis() - started;

        System.out.println("elapsed ms: " + elapsed);
        System.out.println("summary: " + result.summary());
        System.out.println("keywords: " + result.keywords());
        assertThat(result.aiGenerated()).isTrue();
        assertThat(result.keywords()).hasSizeBetween(3, 5);
    }

    private static SurveyProfileDto profile() {
        return new SurveyProfileDto(1L, 1,
                List.of(new CategoryRank("CLEANING", "청소 청결", 1),
                        new CategoryRank("SLEEP_SCHEDULE", "기상 알람", 2),
                        new CategoryRank("SMOKING", "흡연 유무", 3)),
                List.of(topic("SLEEP_HABITS", "SLEEP_HABITS", "잠버릇", "잠버릇이 있나요?",
                                "가끔 잠꼬대를 한다", "조용하기만 하면 상관없다"),
                        topic("BEDTIME", "SLEEP_SCHEDULE", "기상 알람", "평일에 보통 몇 시에 자나요?",
                                "12시에서 1시 사이", "1시 전에는 불을 꺼 주면 좋겠다"),
                        topic("ALARM", "SLEEP_SCHEDULE", "기상 알람", "알람을 몇 번 만에 끄나요?",
                                "첫 알람에 바로 일어난다", "두세 번까지는 괜찮다"),
                        topic("FLOOR_CLEAN", "CLEANING", "청소 청결", "바닥을 얼마나 자주 청소하나요?",
                                "거의 매일 한다", "주 1회 이상이면 괜찮다"),
                        topic("FOOD_WASTE", "CLEANING", "청소 청결", "먹다 남긴 음식은 언제 버리나요?",
                                "바로 버린다", "바로 버려 주면 좋겠다"),
                        topic("BELONGINGS", "PERSONAL_HYGIENE", "개인 청결", "사용한 물건은 언제 정리하나요?",
                                "바로 정리한다", "그날 안에만 정리하면 괜찮다"),
                        topic("DISHES", "PERSONAL_HYGIENE", "개인 청결", "사용한 컵이나 그릇은 언제 치우나요?",
                                "그날 안에 정리한다", "다음 날까지는 괜찮다"),
                        topic("SMOKING", "SMOKING", "흡연 유무", "흡연을 하나요?",
                                "비흡연자다", "흡연자와는 같은 방이 어렵다")));
    }

    private static TopicAnswer topic(String topicCode, String categoryCode, String categoryName,
            String habitQuestion, String habitAnswer, String toleranceAnswer) {
        return new TopicAnswer(topicCode, categoryCode, categoryName, "", habitQuestion, habitAnswer,
                "", toleranceAnswer);
    }
}
