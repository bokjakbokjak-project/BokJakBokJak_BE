package kr.bokjakbokjak.survey;

import java.util.List;

public record SurveyCatalogResponse(List<Category> categories, List<Topic> topics) {

    public record Category(String categoryCode, String name, int displayOrder) {
    }

    public record Topic(
            String topicCode,
            String categoryCode,
            int displayOrder,
            String scenarioText,
            List<QuestionItem> questions) {
    }

    public record QuestionItem(
            String questionCode,
            int questionNumber,
            QuestionKind kind,
            String prompt,
            List<Option> options) {
    }

    public record Option(String optionCode, String label) {
    }
}
