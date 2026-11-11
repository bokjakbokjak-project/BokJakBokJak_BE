package kr.bokjakbokjak.ai;

public record AzureOpenAiProperties(String endpoint, String apiKey, String deployment, String apiVersion) {
    public boolean isConfigured() {
        return hasText(endpoint) && hasText(apiKey) && hasText(deployment) && hasText(apiVersion);
    }

    @Override
    public String toString() {
        return "AzureOpenAiProperties[endpoint=" + endpoint + ", deployment=" + deployment
                + ", apiVersion=" + apiVersion + ", apiKey=" + (hasText(apiKey) ? "****" : "") + "]";
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
