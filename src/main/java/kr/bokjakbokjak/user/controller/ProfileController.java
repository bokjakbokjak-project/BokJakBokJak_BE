package kr.bokjakbokjak.user.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import kr.bokjakbokjak.common.ApiResponse;
import kr.bokjakbokjak.common.SuccessCode;
import kr.bokjakbokjak.user.dto.MyProfileResponse;
import kr.bokjakbokjak.user.dto.NicknameRequest;
import kr.bokjakbokjak.user.dto.NicknameResponse;
import kr.bokjakbokjak.user.dto.RoommateProfileResponse;
import kr.bokjakbokjak.user.service.ProfileService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "프로필")
@RestController
public class ProfileController {
    private final ProfileService profileService;

    public ProfileController(ProfileService profileService) {
        this.profileService = profileService;
    }

    @Operation(summary = "내 성향 프로필 조회",
            description = "카테고리 5개·주제 8개의 습관 질문, 나의 성향, 원하는 성향을 표시 순서대로 반환한다. 설문 전이면 409.")
    @GetMapping("/api/me/profile")
    public ApiResponse.Success<MyProfileResponse> getMyProfile(@AuthenticationPrincipal Long userId) {
        return ApiResponse.success(SuccessCode.MY_PROFILE_SUCCESS, profileService.getMyProfile(userId));
    }

    @Operation(summary = "닉네임 변경", description = "앞뒤 공백을 뺀 1~20자. 설문 차수와 매칭 점수는 바뀌지 않는다.")
    @PatchMapping("/api/me/profile")
    public ApiResponse.Success<NicknameResponse> changeNickname(
            @AuthenticationPrincipal Long userId,
            @RequestBody NicknameRequest request) {
        return ApiResponse.success(SuccessCode.NICKNAME_UPDATE_SUCCESS, profileService.changeNickname(userId, request));
    }

    @Operation(summary = "추천 후보 상세 프로필 조회",
            description = "같은 학교이고 설문을 마친 후보의 답변을 내 성향 프로필과 같은 구조로 반환한다.")
    @GetMapping("/api/roommates/{candidateId}/profile")
    public ApiResponse.Success<RoommateProfileResponse> getRoommateProfile(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long candidateId) {
        return ApiResponse.success(SuccessCode.ROOMMATE_PROFILE_SUCCESS,
                profileService.getRoommateProfile(userId, candidateId));
    }
}
