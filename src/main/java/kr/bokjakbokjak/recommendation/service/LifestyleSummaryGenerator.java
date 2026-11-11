package kr.bokjakbokjak.recommendation.service;

import java.util.List;
import java.util.Optional;
import kr.bokjakbokjak.ai.LlmClient;
import kr.bokjakbokjak.ai.LlmJsonParser;
import kr.bokjakbokjak.ai.LlmRequest;
import kr.bokjakbokjak.ai.ReasoningEffort;
import kr.bokjakbokjak.matching.SurveyProfileDto;
import org.springframework.stereotype.Component;

@Component
class LifestyleSummaryGenerator {
    private static final int MAX_ATTEMPTS = 2;
    private static final int MAX_COMPLETION_TOKENS = 2000;
    private static final int MIN_KEYWORDS = 3;
    private static final int MAX_KEYWORDS = 5;
    private static final int MAX_KEYWORD_LENGTH = 10;

    private final LlmClient llmClient;
    private final LlmJsonParser jsonParser;

    LifestyleSummaryGenerator(LlmClient llmClient, LlmJsonParser jsonParser) {
        this.llmClient = llmClient;
        this.jsonParser = jsonParser;
    }

    GeneratedLifestyleSummary generate(SurveyProfileDto profile) {
        LlmRequest request = new LlmRequest(LifestyleSummaryPrompt.SYSTEM, LifestyleSummaryPrompt.userInput(profile),
                ReasoningEffort.MINIMAL, MAX_COMPLETION_TOKENS);
        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
            Optional<GeneratedLifestyleSummary> result = llmClient.complete(request)
                    .flatMap(raw -> jsonParser.parse(raw, LlmOutput.class))
                    .flatMap(LifestyleSummaryGenerator::validate);
            if (result.isPresent()) {
                return result.get();
            }
        }
        return fallback(profile);
    }

    private static Optional<GeneratedLifestyleSummary> validate(LlmOutput output) {
        if (output.summary() == null || output.summary().isBlank() || output.keywords() == null) {
            return Optional.empty();
        }
        List<String> keywords = output.keywords().stream()
                .filter(keyword -> keyword != null && !keyword.isBlank())
                .map(String::strip)
                .filter(keyword -> keyword.codePointCount(0, keyword.length()) <= MAX_KEYWORD_LENGTH)
                .distinct()
                .limit(MAX_KEYWORDS)
                .toList();
        if (keywords.size() < MIN_KEYWORDS) {
            return Optional.empty();
        }
        return Optional.of(new GeneratedLifestyleSummary(output.summary().strip(), keywords, true));
    }

    private static GeneratedLifestyleSummary fallback(SurveyProfileDto profile) {
        List<String> categoryNames = profile.topCategories().stream()
                .map(SurveyProfileDto.CategoryRank::name)
                .toList();
        String summary = categoryNames.isEmpty()
                ? "생활습관 설문을 완료했어요."
                : String.join(", ", categoryNames) + " 순서로 생활습관을 중요하게 생각해요.";
        return new GeneratedLifestyleSummary(summary, categoryNames, false);
    }

    record LlmOutput(String summary, List<String> keywords) {
    }
}
