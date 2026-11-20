package kr.bokjakbokjak.user.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import kr.bokjakbokjak.common.ApiResponse;
import kr.bokjakbokjak.common.SuccessCode;
import kr.bokjakbokjak.user.dto.MyStatusResponse;
import kr.bokjakbokjak.user.service.MyStatusService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "회원 상태")
@RestController
public class MyStatusController {
    private final MyStatusService myStatusService;

    public MyStatusController(MyStatusService myStatusService) {
        this.myStatusService = myStatusService;
    }

    @Operation(summary = "내 상태 조회",
            description = "온보딩·설문 완료 여부와 다음에 보여줄 화면(next_step), 최종 매칭 여부를 반환한다. "
                    + "앱 시작이나 첫 화면 진입 때 호출해 이동할 화면을 정한다.")
    @GetMapping("/api/me/status")
    public ApiResponse.Success<MyStatusResponse> getStatus(@AuthenticationPrincipal Long userId) {
        return ApiResponse.success(SuccessCode.MY_STATUS_SUCCESS, myStatusService.getStatus(userId));
    }
}
