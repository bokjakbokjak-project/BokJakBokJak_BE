package kr.bokjakbokjak.auth.dto;

import kr.bokjakbokjak.user.entity.User;

/** 로그인 후 FE가 이동할 화면. */
public enum NextStep {
    ONBOARDING,
    SURVEY,
    HOME;

    public static NextStep of(User user) {
        if (!user.isOnboardingCompleted()) {
            return ONBOARDING;
        }
        return user.isSurveyCompleted() ? HOME : SURVEY;
    }
}
