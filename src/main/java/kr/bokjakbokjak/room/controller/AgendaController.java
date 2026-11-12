package kr.bokjakbokjak.room.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import kr.bokjakbokjak.common.ApiResponse;
import kr.bokjakbokjak.common.SuccessCode;
import kr.bokjakbokjak.room.dto.AgendaDecideResponse;
import kr.bokjakbokjak.room.service.AgendaService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "안건")
@RestController
@RequestMapping("/api/matchings/{matchingId}/agendas")
public class AgendaController {
    private final AgendaService agendaService;

    public AgendaController(AgendaService agendaService) {
        this.agendaService = agendaService;
    }

    @Operation(summary = "안건 확정", description = "한 명만 눌러도 확정되고 다음 안건이 시작된다. 이미 확정된 안건이면 현재 상태를 반환한다.")
    @PatchMapping("/{agendaId}/decide")
    public ApiResponse.Success<AgendaDecideResponse> decide(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long matchingId,
            @PathVariable Long agendaId) {
        return ApiResponse.success(SuccessCode.AGENDA_DECIDE_SUCCESS,
                agendaService.decide(userId, matchingId, agendaId));
    }
}
