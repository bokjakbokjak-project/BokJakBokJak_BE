package kr.bokjakbokjak.share;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import kr.bokjakbokjak.common.ApiResponse;
import kr.bokjakbokjak.common.SuccessCode;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "공유")
@RestController
public class ShareController {
    private final ShareMatchService shareMatchService;

    public ShareController(ShareMatchService shareMatchService) {
        this.shareMatchService = shareMatchService;
    }

    @Operation(summary = "공유 링크 매칭률 조회",
            description = "공유 링크를 연 사용자와 공유한 사용자의 매칭률(0~100)을 반환한다. 학교·기숙사·성별과 상관없이 계산한다. "
                    + "내가 설문 전이면 409 SURVEY_REQUIRED, 상대가 없거나 탈퇴했거나 설문을 낸 적 없으면 404, 본인이면 400.")
    @GetMapping("/api/share/{userId}/match-score")
    public ApiResponse.Success<ShareMatchScoreResponse> getMatchScore(
            @AuthenticationPrincipal Long viewerId,
            @PathVariable Long userId) {
        return ApiResponse.success(SuccessCode.SHARE_MATCH_SCORE_SUCCESS,
                shareMatchService.getMatchScore(viewerId, userId));
    }
}
