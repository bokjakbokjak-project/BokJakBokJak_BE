package kr.bokjakbokjak.ai;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

class LlmJsonParserTest {
    private final LlmJsonParser parser = new LlmJsonParser(JsonMapper.builder().build());

    record Output(String summary, List<String> keywords) {
    }

    @Test
    void parsesPlainJson() {
        assertThat(parser.parse("{\"summary\":\"요약\",\"keywords\":[\"a\"]}", Output.class))
                .contains(new Output("요약", List.of("a")));
    }

    @Test
    void stripsCodeFence() {
        String raw = """
                ```json
                {"summary":"요약","keywords":["a","b"]}
                ```
                """;

        assertThat(parser.parse(raw, Output.class)).contains(new Output("요약", List.of("a", "b")));
    }

    @Test
    void invalidJsonIsEmpty() {
        assertThat(parser.parse("요약입니다", Output.class)).isEmpty();
        assertThat(parser.parse("{\"summary\":", Output.class)).isEmpty();
    }
}
