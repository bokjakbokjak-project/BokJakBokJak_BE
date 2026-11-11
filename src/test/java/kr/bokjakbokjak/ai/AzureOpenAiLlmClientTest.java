package kr.bokjakbokjak.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.json.JsonMapper;

class AzureOpenAiLlmClientTest {
    private static final String URL =
            "https://bokjak-ai.openai.azure.com/openai/deployments/bokjak-chat/chat/completions?api-version=2025-04-01-preview";
    private static final LlmRequest REQUEST = new LlmRequest("system", "user", ReasoningEffort.MINIMAL, 2000);

    private MockRestServiceServer server;
    private AzureOpenAiLlmClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        var properties = new AzureOpenAiProperties(
                "https://bokjak-ai.openai.azure.com/", "test-key", "bokjak-chat", "2025-04-01-preview");
        client = new AzureOpenAiLlmClient(properties, builder.build(), JsonMapper.builder().build());
    }

    @Test
    void sendsReasoningOptionsAndReturnsContent() {
        server.expect(requestTo(URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("api-key", "test-key"))
                .andExpect(jsonPath("$.messages[0].role").value("system"))
                .andExpect(jsonPath("$.messages[0].content").value("system"))
                .andExpect(jsonPath("$.messages[1].role").value("user"))
                .andExpect(jsonPath("$.reasoning_effort").value("minimal"))
                .andExpect(jsonPath("$.max_completion_tokens").value(2000))
                .andExpect(jsonPath("$.response_format.type").value("json_object"))
                .andExpect(jsonPath("$.temperature").doesNotExist())
                .andRespond(withSuccess(completion("stop", "{\\\"summary\\\":\\\"요약\\\"}"), MediaType.APPLICATION_JSON));

        assertThat(client.complete(REQUEST)).contains("{\"summary\":\"요약\"}");
        server.verify();
    }

    @Test
    void truncatedResponseIsUnusable() {
        server.expect(requestTo(URL))
                .andRespond(withSuccess(completion("length", "{\\\"summary\\\":"), MediaType.APPLICATION_JSON));

        assertThat(client.complete(REQUEST)).isEmpty();
    }

    @Test
    void serverErrorIsUnusable() {
        server.expect(requestTo(URL)).andRespond(withServerError());

        assertThat(client.complete(REQUEST)).isEmpty();
    }

    @Test
    void propertiesHideApiKey() {
        var properties = new AzureOpenAiProperties("https://example", "secret-key", "bokjak-chat", "v1");

        assertThat(properties.toString()).doesNotContain("secret-key");
        assertThat(properties.isConfigured()).isTrue();
        assertThat(new AzureOpenAiProperties("https://example", "", "bokjak-chat", "v1").isConfigured()).isFalse();
    }

    private static String completion(String finishReason, String content) {
        return """
                {"choices":[{"finish_reason":"%s","message":{"role":"assistant","content":"%s"}}]}
                """.formatted(finishReason, content);
    }
}
