package kr.bokjakbokjak.ai;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

public class AzureOpenAiLlmClient implements LlmClient {
    private static final Logger log = LoggerFactory.getLogger(AzureOpenAiLlmClient.class);

    private final AzureOpenAiProperties properties;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public AzureOpenAiLlmClient(AzureOpenAiProperties properties, RestClient restClient, ObjectMapper objectMapper) {
        this.properties = properties;
        this.restClient = restClient;
        this.objectMapper = objectMapper;
    }

    @Override
    public Optional<String> complete(LlmRequest request) {
        try {
            String response = restClient.post()
                    .uri(properties.endpoint().replaceAll("/+$", "")
                                    + "/openai/deployments/{deployment}/chat/completions?api-version={version}",
                            properties.deployment(), properties.apiVersion())
                    .header("api-key", properties.apiKey())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(objectMapper.writeValueAsString(body(request)))
                    .retrieve()
                    .body(String.class);
            return extractContent(response);
        } catch (RuntimeException exception) {
            log.warn("LLM request failed: {}", exception.getClass().getSimpleName());
            return Optional.empty();
        }
    }

    private Map<String, Object> body(LlmRequest request) {
        return Map.of(
                "messages", List.of(
                        Map.of("role", "system", "content", request.systemPrompt()),
                        Map.of("role", "user", "content", request.userPrompt())),
                "reasoning_effort", request.reasoningEffort().value(),
                "max_completion_tokens", request.maxCompletionTokens(),
                "response_format", Map.of("type", "json_object"));
    }

    private Optional<String> extractContent(String response) {
        JsonNode choice = objectMapper.readTree(response).path("choices").path(0);
        String finishReason = choice.path("finish_reason").asString("");
        String content = choice.path("message").path("content").asString("");
        if (!"stop".equals(finishReason) || content.isBlank()) {
            log.warn("LLM response unusable: finish_reason={}", finishReason);
            return Optional.empty();
        }
        return Optional.of(content);
    }
}
