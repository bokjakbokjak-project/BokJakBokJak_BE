package kr.bokjakbokjak.room.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import kr.bokjakbokjak.common.ApiResponse;
import kr.bokjakbokjak.common.SuccessCode;
import kr.bokjakbokjak.room.dto.MatchingEndResponse;
import kr.bokjakbokjak.room.service.MatchingEndService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "매칭 중지·확정")
@RestController
@RequestMapping("/api/matchings/{matchingId}")
public class MatchingController {
    private final MatchingEndService matchingEndService;

    public MatchingController(MatchingEndService matchingEndService) {
        this.matchingEndService = matchingEndService;
    }

    @Operation(summary = "매칭 중지", description = "대화 중인 매칭만 중지할 수 있고, 중지한 상대와는 다시 대화할 수 없다.")
    @PatchMapping("/end")
    public ApiResponse.Success<MatchingEndResponse> end(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long matchingId) {
        return ApiResponse.success(SuccessCode.MATCH_END_SUCCESS, matchingEndService.end(userId, matchingId));
    }
}
