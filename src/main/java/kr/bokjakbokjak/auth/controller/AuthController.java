package kr.bokjakbokjak.auth.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import kr.bokjakbokjak.auth.dto.DevLoginRequest;
import kr.bokjakbokjak.auth.dto.DevResetResponse;
import kr.bokjakbokjak.auth.dto.GuestLoginRequest;
import kr.bokjakbokjak.auth.dto.LoginRequest;
import kr.bokjakbokjak.auth.dto.LoginResponse;
import kr.bokjakbokjak.auth.dto.RefreshTokenRequest;
import kr.bokjakbokjak.auth.dto.TokenResponse;
import kr.bokjakbokjak.auth.service.AuthService;
import kr.bokjakbokjak.auth.service.DevAccessGuard;
import kr.bokjakbokjak.auth.service.DevResetService;
import kr.bokjakbokjak.common.ApiResponse;
import kr.bokjakbokjak.common.JwtService;
import kr.bokjakbokjak.common.SuccessCode;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "인증")
@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;
    private final DevResetService devResetService;

    public AuthController(AuthService authService, DevResetService devResetService) {
        this.authService = authService;
        this.devResetService = devResetService;
    }

    @Operation(summary = "로그인", description = "Google 학교 계정 ID 토큰으로 로그인한다. 처음이면 회원을 만든다.")
    @PostMapping("/login")
    public ApiResponse.Success<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.success(SuccessCode.LOGIN_SUCCESS, authService.login(request.idToken()));
    }

    @Operation(summary = "토큰 재발급", description = "리프레시 토큰으로 액세스·리프레시 토큰을 모두 새로 받는다. 쓴 리프레시 토큰은 폐기된다.")
    @PostMapping("/refresh")
    public ApiResponse.Success<TokenResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return ApiResponse.success(SuccessCode.TOKEN_REFRESH_SUCCESS, authService.refresh(request.refreshToken()));
    }

    @Operation(summary = "로그아웃", description = "현재 액세스 토큰을 차단하고 리프레시 토큰을 지운다.")
    @PostMapping("/logout")
    public ApiResponse.Success<Void> logout(
            Authentication authentication,
            @Valid @RequestBody RefreshTokenRequest request) {
        authService.logout((JwtService.AccessClaims) authentication.getDetails(), request.refreshToken());
        return ApiResponse.success(SuccessCode.LOGOUT_SUCCESS, null);
    }

    @Operation(summary = "체험 로그인 (심사·데모용)",
            description = "이름만으로 매번 새 체험 계정을 만들어 토큰을 발급한다. 동국대 남산학사 2인실·여성·온보딩 완료 상태의 "
                    + "테스트 계정이라 테스트 페르소나와 같은 후보군에 들어간다. next_step은 SURVEY.")
    @PostMapping("/guest-login")
    public ApiResponse.Success<LoginResponse> guestLogin(@RequestBody GuestLoginRequest request) {
        return ApiResponse.success(SuccessCode.LOGIN_SUCCESS, authService.guestLogin(request.nickname()));
    }

    @Operation(summary = "테스트 로그인 (개발·시연용)",
            description = "DEV_LOGIN_KEY가 설정된 서버에서 X-Dev-Login-Key가 맞고 테스트 유저일 때만 토큰을 발급한다. "
                    + "user_id와 nickname 중 하나로 테스트 유저를 찾는다.")
    @PostMapping("/dev-login")
    public ApiResponse.Success<LoginResponse> devLogin(
            @RequestHeader(name = DevAccessGuard.HEADER, required = false) String key,
            @Valid @RequestBody DevLoginRequest request) {
        return ApiResponse.success(SuccessCode.LOGIN_SUCCESS, authService.devLogin(key, request));
    }

    @Operation(summary = "테스트 계정 초기화 (개발·시연용)",
            description = "테스트 유저끼리 만든 매칭과 그 매칭의 메시지, 안건, 알림을 지운다. 실제 회원이 포함된 매칭은 남긴다.")
    @PostMapping("/dev-reset")
    public ApiResponse.Success<DevResetResponse> devReset(
            @RequestHeader(name = DevAccessGuard.HEADER, required = false) String key) {
        return ApiResponse.success(SuccessCode.DEV_RESET_SUCCESS, devResetService.reset(key));
    }
}
