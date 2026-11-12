package kr.bokjakbokjak.dormitory.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import kr.bokjakbokjak.common.ApiResponse;
import kr.bokjakbokjak.common.SuccessCode;
import kr.bokjakbokjak.dormitory.dto.DormitoryListResponse;
import kr.bokjakbokjak.dormitory.dto.DormitoryRegisterRequest;
import kr.bokjakbokjak.dormitory.dto.DormitoryResponse;
import kr.bokjakbokjak.dormitory.service.DormitoryService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "기숙사")
@RestController
@RequestMapping("/api/dormitories")
public class DormitoryController {
    private final DormitoryService dormitoryService;

    public DormitoryController(DormitoryService dormitoryService) {
        this.dormitoryService = dormitoryService;
    }

    @Operation(summary = "기숙사·방 타입 조회", description = "본인 학교 기숙사를 이름으로 검색한다. MVP는 2인실만 보여준다.")
    @GetMapping
    public ApiResponse.Success<DormitoryListResponse> search(
            @AuthenticationPrincipal Long userId,
            @RequestParam(required = false) String keyword) {
        return ApiResponse.success(SuccessCode.DORMITORY_LIST_SUCCESS, dormitoryService.search(userId, keyword));
    }

    @Operation(summary = "기숙사·방 타입 태그 등록",
            description = "검색에 없는 기숙사·방 타입을 등록한다. 같은 태그는 기존 ID를 돌려준다.")
    @PostMapping
    public ApiResponse.Success<DormitoryResponse> register(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody DormitoryRegisterRequest request) {
        return ApiResponse.success(SuccessCode.DORMITORY_TAG_READY, dormitoryService.register(userId, request));
    }
}
