package kr.bokjakbokjak.ai;

import java.util.Optional;

public interface LlmClient {
    Optional<String> complete(LlmRequest request);
}
