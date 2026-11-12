package kr.bokjakbokjak.dormitory.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record DormitoryRegisterRequest(
        @NotBlank(message = "기숙사명을 입력해주세요.")
        @Size(max = 80, message = "기숙사명은 80자 이하로 입력해주세요.")
        String name,
        @NotBlank(message = "방 타입 이름을 입력해주세요.")
        @Size(max = 60, message = "방 타입 이름은 60자 이하로 입력해주세요.")
        String roomTypeName,
        @NotNull(message = "방 인원을 입력해주세요.")
        Integer capacity) {
}
