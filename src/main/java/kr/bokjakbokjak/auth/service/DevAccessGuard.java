package kr.bokjakbokjak.auth.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import kr.bokjakbokjak.auth.config.AuthProperties;
import kr.bokjakbokjak.common.ApiException;
import kr.bokjakbokjak.common.ErrorCode;
import org.springframework.stereotype.Component;

/** 개발·시연용 API(테스트 로그인, 테스트 계정 초기화)의 키를 확인한다. */
@Component
public class DevAccessGuard {
    public static final String HEADER = "X-Dev-Login-Key";

    private final AuthProperties properties;

    public DevAccessGuard(AuthProperties properties) {
        this.properties = properties;
    }

    /** 키가 설정되지 않은 서버에서는 API가 없는 것처럼 404, 키가 틀리면 401 */
    public void check(String key) {
        if (!properties.devLoginEnabled()) {
            throw new ApiException(ErrorCode.RESOURCE_NOT_FOUND);
        }
        if (key == null || !MessageDigest.isEqual(
                key.getBytes(StandardCharsets.UTF_8), properties.devLoginKey().getBytes(StandardCharsets.UTF_8))) {
            throw new ApiException(ErrorCode.INVALID_DEV_LOGIN_KEY);
        }
    }
}
