package kr.bokjakbokjak.ai;

import java.net.http.HttpClient;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.ObjectMapper;

@Configuration
public class LlmClientConfig {
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(5);
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(30);

    @Bean
    AzureOpenAiProperties azureOpenAiProperties(
            @Value("${AZURE_OPENAI_ENDPOINT:}") String endpoint,
            @Value("${AZURE_OPENAI_API_KEY:}") String apiKey,
            @Value("${AZURE_OPENAI_DEPLOYMENT:}") String deployment,
            @Value("${AZURE_OPENAI_API_VERSION:}") String apiVersion) {
        return new AzureOpenAiProperties(endpoint, apiKey, deployment, apiVersion);
    }

    @Bean
    LlmClient llmClient(AzureOpenAiProperties properties, ObjectMapper objectMapper) {
        if (!properties.isConfigured()) {
            return new StubLlmClient();
        }
        var httpClient = HttpClient.newBuilder().connectTimeout(CONNECT_TIMEOUT).build();
        var requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(READ_TIMEOUT);
        RestClient restClient = RestClient.builder().requestFactory(requestFactory).build();
        return new AzureOpenAiLlmClient(properties, restClient, objectMapper);
    }
}
