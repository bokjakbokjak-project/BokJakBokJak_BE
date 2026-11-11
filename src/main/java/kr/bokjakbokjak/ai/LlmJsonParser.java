package kr.bokjakbokjak.ai;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Component
public class LlmJsonParser {
    private static final Logger log = LoggerFactory.getLogger(LlmJsonParser.class);
    private static final Pattern CODE_FENCE = Pattern.compile("^```(?:json)?\\s*(.*?)\\s*```$", Pattern.DOTALL);

    private final ObjectMapper objectMapper;

    public LlmJsonParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public <T> Optional<T> parse(String raw, Class<T> type) {
        try {
            return Optional.ofNullable(objectMapper.readValue(stripCodeFence(raw), type));
        } catch (JacksonException exception) {
            log.warn("LLM JSON parse failed for {}", type.getSimpleName());
            return Optional.empty();
        }
    }

    private String stripCodeFence(String raw) {
        String trimmed = raw.strip();
        Matcher matcher = CODE_FENCE.matcher(trimmed);
        return matcher.matches() ? matcher.group(1) : trimmed;
    }
}
