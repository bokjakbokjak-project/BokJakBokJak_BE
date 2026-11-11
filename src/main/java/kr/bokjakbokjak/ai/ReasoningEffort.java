package kr.bokjakbokjak.ai;

public enum ReasoningEffort {
    MINIMAL("minimal"),
    LOW("low");

    private final String value;

    ReasoningEffort(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }
}
