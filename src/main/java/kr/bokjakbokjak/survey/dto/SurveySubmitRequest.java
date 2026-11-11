package kr.bokjakbokjak.survey.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.util.List;

public record SurveySubmitRequest(
        @NotNull(message = "expected_revision을 입력해주세요.")
        @PositiveOrZero(message = "expected_revision은 0 이상이어야 합니다.")
        Integer expectedRevision,
        @NotNull(message = "카테고리 순위를 입력해주세요.")
        List<Priority> priorities,
        @NotNull(message = "필수 문항을 모두 제출해주세요.")
        List<Answer> answers) {

    public record Priority(String categoryCode, Integer rank) {
    }

    public record Answer(String questionCode, String optionCode) {
    }
}
