package kr.bokjakbokjak.survey;

import java.time.Instant;

public record SurveySubmitResponse(int revision, boolean surveyCompleted, Instant submittedAt, String nextStep) {
}
