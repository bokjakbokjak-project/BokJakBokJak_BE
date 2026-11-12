package kr.bokjakbokjak.auth.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import kr.bokjakbokjak.auth.dto.DevLoginRequest;
import kr.bokjakbokjak.auth.dto.LoginRequest;
import kr.bokjakbokjak.auth.dto.LoginResponse;
import kr.bokjakbokjak.auth.dto.RefreshTokenRequest;
import kr.bokjakbokjak.auth.dto.TokenResponse;
import kr.bokjakbokjak.auth.service.AuthService;
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

    public AuthController(AuthService authService) {
        this.authService = authService;
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

    @Operation(summary = "테스트 로그인 (개발·시연용)",
            description = "DEV_LOGIN_KEY가 설정된 서버에서 X-Dev-Login-Key가 맞고 테스트 유저일 때만 토큰을 발급한다.")
    @PostMapping("/dev-login")
    public ApiResponse.Success<LoginResponse> devLogin(
            @RequestHeader(name = "X-Dev-Login-Key", required = false) String key,
            @Valid @RequestBody DevLoginRequest request) {
        return ApiResponse.success(SuccessCode.LOGIN_SUCCESS, authService.devLogin(key, request.userId()));
    }
}
