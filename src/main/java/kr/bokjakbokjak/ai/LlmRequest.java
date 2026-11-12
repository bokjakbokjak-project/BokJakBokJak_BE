package kr.bokjakbokjak.ai;

public record LlmRequest(
        String systemPrompt,
        String userPrompt,
        ReasoningEffort reasoningEffort,
        int maxCompletionTokens) {
}
