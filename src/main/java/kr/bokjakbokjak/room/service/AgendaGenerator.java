package kr.bokjakbokjak.room.service;

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
import kr.bokjakbokjak.matching.MatchingEngine;
import org.springframework.stereotype.Component;

@Component
class AgendaGenerator {
    private static final int MAX_ATTEMPTS = 2;
    private static final int MAX_COMPLETION_TOKENS = 2000;
    private static final int MAX_TITLE_LENGTH = 20;
    private static final int MAX_CONTENT_LENGTH = 80;

    private final MatchingEngine matchingEngine;
    private final LlmClient llmClient;
    private final LlmJsonParser jsonParser;

    AgendaGenerator(MatchingEngine matchingEngine, LlmClient llmClient, LlmJsonParser jsonParser) {
        this.matchingEngine = matchingEngine;
        this.llmClient = llmClient;
        this.jsonParser = jsonParser;
    }

    List<GeneratedAgenda> generate(Long requesterId, Long receiverId) {
        List<MatchItemDto> topics = AgendaTopicSelector.select(
                matchingEngine.calculate(requesterId, receiverId).items());
        LlmRequest request = new LlmRequest(AgendaPrompt.SYSTEM, AgendaPrompt.userInput(topics),
                ReasoningEffort.LOW, MAX_COMPLETION_TOKENS);
        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
            Optional<List<GeneratedAgenda>> result = llmClient.complete(request)
                    .flatMap(raw -> jsonParser.parse(raw, LlmOutput.class))
                    .flatMap(output -> validate(output, topics));
            if (result.isPresent()) {
                return result.get();
            }
        }
        return fallback(topics);
    }

    private static Optional<List<GeneratedAgenda>> validate(LlmOutput output, List<MatchItemDto> topics) {
        if (output.agendas() == null) {
            return Optional.empty();
        }
        Map<String, GeneratedAgenda> byTopic = output.agendas().stream()
                .filter(agenda -> agenda != null && agenda.topicCode() != null)
                .collect(Collectors.toMap(GeneratedAgenda::topicCode, Function.identity(), (first, second) -> first));
        List<GeneratedAgenda> agendas = topics.stream()
                .map(topic -> byTopic.get(topic.topicCode()))
                .filter(AgendaGenerator::isValid)
                .map(agenda -> new GeneratedAgenda(agenda.topicCode(), agenda.title().strip(), agenda.content().strip()))
                .toList();
        return agendas.size() == topics.size() ? Optional.of(agendas) : Optional.empty();
    }

    private static boolean isValid(GeneratedAgenda agenda) {
        return agenda != null
                && hasLength(agenda.title(), MAX_TITLE_LENGTH)
                && hasLength(agenda.content(), MAX_CONTENT_LENGTH);
    }

    private static boolean hasLength(String text, int maxLength) {
        if (text == null || text.isBlank()) {
            return false;
        }
        String stripped = text.strip();
        return stripped.codePointCount(0, stripped.length()) <= maxLength;
    }

    private static List<GeneratedAgenda> fallback(List<MatchItemDto> topics) {
        return topics.stream().map(topic -> DefaultAgendas.of(topic.topicCode())).toList();
    }

    record LlmOutput(List<GeneratedAgenda> agendas) {
    }
}
