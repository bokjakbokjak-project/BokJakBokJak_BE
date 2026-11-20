package kr.bokjakbokjak.user.dto;

import kr.bokjakbokjak.auth.dto.NextStep;

/**
 * 앱 시작이나 첫 화면 진입 때 이동할 화면을 정하는 데 쓴다.
 *
 * @param surveyUpdateRequired 설문을 낸 적은 있지만 문항이 바뀌어 다시 해야 하는지
 * @param nextStep 로그인 응답의 next_step과 같은 기준
 * @param matchingId 최종 매칭된 매칭 ID. 매칭 전이면 null
 */
public record MyStatusResponse(
        boolean onboardingCompleted,
        boolean surveyCompleted,
        boolean surveyUpdateRequired,
        NextStep nextStep,
        boolean matched,
        Long matchingId) {
}
