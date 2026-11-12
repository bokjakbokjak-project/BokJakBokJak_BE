package kr.bokjakbokjak.ai;

import java.util.Optional;

public class StubLlmClient implements LlmClient {
    @Override
    public Optional<String> complete(LlmRequest request) {
        return Optional.empty();
    }
}
