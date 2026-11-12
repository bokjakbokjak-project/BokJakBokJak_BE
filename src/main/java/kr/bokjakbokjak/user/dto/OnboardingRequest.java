package kr.bokjakbokjak.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.List;
import kr.bokjakbokjak.survey.dto.SurveySubmitRequest;
import kr.bokjakbokjak.user.entity.Gender;

/**
 * @param nickname 화면의 「이름」. 마이페이지 닉네임과 같은 값
 * @param priorities 설문 제출과 같은 형식의 카테고리 순위
 */
public record OnboardingRequest(
        @NotBlank(message = "학과를 입력해주세요.")
        @Size(max = 60, message = "학과는 60자 이하로 입력해주세요.")
        String department,
        @NotBlank(message = "학번을 입력해주세요.")
        @Pattern(regexp = "\\d{4,10}", message = "학번은 숫자 4~10자리로 입력해주세요.")
        String studentNumber,
        @NotBlank(message = "이름을 입력해주세요.")
        String nickname,
        @NotNull(message = "성별을 선택해주세요.")
        Gender gender,
        @NotNull(message = "출생년도를 입력해주세요.")
        Integer birthYear,
        @NotNull(message = "방 타입을 선택해주세요.")
        @Positive(message = "room_type_id는 1 이상이어야 합니다.")
        Long roomTypeId,
        @NotNull(message = "카테고리 순위를 입력해주세요.")
        List<SurveySubmitRequest.Priority> priorities) {
}
