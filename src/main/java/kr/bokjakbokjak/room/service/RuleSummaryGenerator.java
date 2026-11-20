package kr.bokjakbokjak.room.service;

import java.util.List;
import java.util.Optional;
import kr.bokjakbokjak.ai.LlmClient;
import kr.bokjakbokjak.ai.LlmJsonParser;
import kr.bokjakbokjak.ai.LlmRequest;
import kr.bokjakbokjak.ai.ReasoningEffort;
import kr.bokjakbokjak.room.service.RuleSummaryPrompt.ChatLine;
import org.springframework.stereotype.Component;

@Component
class RuleSummaryGenerator {
    private static final int MAX_ATTEMPTS = 2;
    private static final int MAX_COMPLETION_TOKENS = 2000;
    private static final int MAX_RULE_LENGTH = 60;

    private final LlmClient llmClient;
    private final LlmJsonParser jsonParser;

    RuleSummaryGenerator(LlmClient llmClient, LlmJsonParser jsonParser) {
        this.llmClient = llmClient;
        this.jsonParser = jsonParser;
    }

    Optional<String> summarize(String title, String content, List<ChatLine> lines) {
        if (lines.isEmpty()) {
            return Optional.empty();
        }
        LlmRequest request = new LlmRequest(RuleSummaryPrompt.SYSTEM,
                RuleSummaryPrompt.userInput(title, content, lines), ReasoningEffort.LOW, MAX_COMPLETION_TOKENS);
        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
            Optional<LlmOutput> output = llmClient.complete(request)
                    .flatMap(raw -> jsonParser.parse(raw, LlmOutput.class));
            if (output.isEmpty()) {
                continue;
            }
            String rule = output.get().rule();
            if (rule == null) {
                return Optional.empty();
            }
            String stripped = rule.strip();
            if (!stripped.isEmpty() && stripped.codePointCount(0, stripped.length()) <= MAX_RULE_LENGTH) {
                return Optional.of(stripped);
            }
        }
        return Optional.empty();
    }

    record LlmOutput(String rule) {
    }
}
