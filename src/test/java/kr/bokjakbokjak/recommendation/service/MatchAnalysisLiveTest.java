package kr.bokjakbokjak.recommendation.service;

import static org.assertj.core.api.Assertions.assertThat;

import kr.bokjakbokjak.ai.AzureOpenAiLlmClient;
import kr.bokjakbokjak.ai.AzureOpenAiProperties;
import kr.bokjakbokjak.ai.LlmJsonParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.json.JsonMapper;

@EnabledIfEnvironmentVariable(named = "AZURE_OPENAI_API_KEY", matches = ".+")
class MatchAnalysisLiveTest {

    @Test
    void analyzesWithRealAzureOpenAi() {
        var properties = new AzureOpenAiProperties(
                System.getenv("AZURE_OPENAI_ENDPOINT"),
                System.getenv("AZURE_OPENAI_API_KEY"),
                System.getenv("AZURE_OPENAI_DEPLOYMENT"),
                System.getenv("AZURE_OPENAI_API_VERSION"));
        var mapper = JsonMapper.builder().propertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE).build();
        var generator = new MatchAnalysisGenerator(
                new AzureOpenAiLlmClient(properties, RestClient.create(), mapper), new LlmJsonParser(mapper));

        long started = System.currentTimeMillis();
        GeneratedMatchAnalysis analysis = generator.generate(MatchAnalysisGeneratorTest.items());
        long elapsed = System.currentTimeMillis() - started;

        System.out.println("elapsed ms: " + elapsed);
        analysis.goodPoints().forEach(sentence -> System.out.println("good | " + sentence));
        analysis.adjustPoints().forEach(sentence -> System.out.println("adjust | " + sentence));
        analysis.checkPoints().forEach(sentence -> System.out.println("check | " + sentence));
        assertThat(analysis.aiGenerated()).isTrue();
        assertThat(analysis.goodPoints()).hasSize(2);
        assertThat(analysis.adjustPoints()).hasSize(1);
        assertThat(analysis.checkPoints()).hasSize(1);
    }
}
