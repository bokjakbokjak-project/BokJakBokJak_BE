package kr.bokjakbokjak.recommendation.service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import kr.bokjakbokjak.ai.LlmClient;
import kr.bokjakbokjak.ai.LlmJsonParser;
import kr.bokjakbokjak.ai.LlmRequest;
import kr.bokjakbokjak.ai.ReasoningEffort;
import kr.bokjakbokjak.matching.MatchItemDto;
import kr.bokjakbokjak.matching.MatchLabel;
import org.springframework.stereotype.Component;

@Component
class MatchAnalysisGenerator {
    private static final int MAX_ATTEMPTS = 2;
    private static final int MAX_COMPLETION_TOKENS = 2000;
    private static final int MAX_SENTENCE_LENGTH = 120;
    private static final Comparator<MatchItemDto> VIEWER_PRIORITY =
            Comparator.comparing(MatchItemDto::viewerWeight, Comparator.reverseOrder());

    private final LlmClient llmClient;
    private final LlmJsonParser jsonParser;

    MatchAnalysisGenerator(LlmClient llmClient, LlmJsonParser jsonParser) {
        this.llmClient = llmClient;
        this.jsonParser = jsonParser;
    }

    GeneratedMatchAnalysis generate(List<MatchItemDto> items) {
        if (items.isEmpty()) {
            return new GeneratedMatchAnalysis(List.of(), List.of(), List.of());
        }
        LlmRequest request = new LlmRequest(MatchAnalysisPrompt.SYSTEM, MatchAnalysisPrompt.userInput(items),
                ReasoningEffort.LOW, MAX_COMPLETION_TOKENS);
        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
            Optional<Map<String, String>> sentences = llmClient.complete(request)
                    .flatMap(raw -> jsonParser.parse(raw, LlmOutput.class))
                    .flatMap(output -> validate(output, items));
            if (sentences.isPresent()) {
                return classify(items, sentences.get());
            }
        }
        return classify(items, items.stream()
                .collect(Collectors.toMap(MatchItemDto::topicCode, DefaultMatchSentences::of, (first, second) -> first)));
    }

    private static Optional<Map<String, String>> validate(LlmOutput output, List<MatchItemDto> items) {
        if (output.items() == null) {
            return Optional.empty();
        }
        Map<String, String> sentences = output.items().stream()
                .filter(item -> item != null && item.topicCode() != null && isValid(item.sentence()))
                .collect(Collectors.toMap(Item::topicCode, item -> item.sentence().strip(), (first, second) -> first));
        boolean coversAll = items.stream().allMatch(item -> sentences.containsKey(item.topicCode()));
        return coversAll ? Optional.of(sentences) : Optional.empty();
    }

    private static boolean isValid(String sentence) {
        if (sentence == null || sentence.isBlank()) {
            return false;
        }
        String stripped = sentence.strip();
        return stripped.codePointCount(0, stripped.length()) <= MAX_SENTENCE_LENGTH;
    }

    private static GeneratedMatchAnalysis classify(List<MatchItemDto> items, Map<String, String> sentences) {
        Map<MatchLabel, List<String>> byLabel = items.stream()
                .sorted(VIEWER_PRIORITY)
                .collect(Collectors.groupingBy(MatchItemDto::label,
                        Collectors.mapping(item -> sentences.get(item.topicCode()), Collectors.toList())));
        Function<MatchLabel, List<String>> section = label -> byLabel.getOrDefault(label, List.of());
        return new GeneratedMatchAnalysis(section.apply(MatchLabel.GOOD), section.apply(MatchLabel.NEED_ADJUST),
                section.apply(MatchLabel.NEED_CHECK));
    }

    record LlmOutput(List<Item> items) {
    }

    record Item(String topicCode, String sentence) {
    }
}
