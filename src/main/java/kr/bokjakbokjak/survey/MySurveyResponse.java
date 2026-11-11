package kr.bokjakbokjak.survey;

import java.time.Instant;
import java.util.List;

public record MySurveyResponse(
        int revision,
        boolean surveyCompleted,
        Instant submittedAt,
        List<SurveySubmitRequest.Priority> priorities,
        List<SurveySubmitRequest.Answer> answers) {
}
