package kr.bokjakbokjak.user.dto;

import kr.bokjakbokjak.auth.dto.NextStep;

public record OnboardingResponse(boolean onboardingCompleted, boolean surveyCompleted, NextStep nextStep) {
}
