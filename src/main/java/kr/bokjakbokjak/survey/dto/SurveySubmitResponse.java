package kr.bokjakbokjak.survey.dto;

import java.time.Instant;

public record SurveySubmitResponse(int revision, boolean surveyCompleted, Instant submittedAt, String nextStep) {
}
