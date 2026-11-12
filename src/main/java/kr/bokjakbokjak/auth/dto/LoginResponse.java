package kr.bokjakbokjak.auth.dto;

import kr.bokjakbokjak.user.entity.User;

/** expires_in, refresh_expires_in은 초 단위 */
public record LoginResponse(
        String accessToken,
        String tokenType,
        long expiresIn,
        String refreshToken,
        long refreshExpiresIn,
        Long userId,
        Long universityId,
        boolean onboardingCompleted,
        boolean surveyCompleted,
        NextStep nextStep) {

    public static LoginResponse of(User user, TokenResponse tokens) {
        return new LoginResponse(
                tokens.accessToken(),
                tokens.tokenType(),
                tokens.expiresIn(),
                tokens.refreshToken(),
                tokens.refreshExpiresIn(),
                user.getId(),
                user.getUniversityId(),
                user.isOnboardingCompleted(),
                user.isSurveyCompleted(),
                NextStep.of(user));
    }
}
