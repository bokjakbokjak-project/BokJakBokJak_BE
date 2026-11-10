package kr.bokjakbokjak.common;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.Base64;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@SpringBootTest
@AutoConfigureMockMvc
@Import(CommonContractTest.TestController.class)
@ActiveProfiles("test")
class CommonContractTest {
    @Autowired MockMvc mvc;
    @Autowired JwtService jwtService;

    @DynamicPropertySource
    static void config(DynamicPropertyRegistry registry) {
        registry.add("app.jwt.secret-base64", () -> Base64.getEncoder().encodeToString(new byte[32]));
    }

    @Test
    void healthUsesTheSuccessEnvelope() throws Exception {
        mvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value("COMMON2000"))
                .andExpect(jsonPath("$.data.status").value("UP"));
    }

    @Test
    void missingAndMalformedTokensUseTheFailureEnvelope() throws Exception {
        mvc.perform(get("/api/test/principal"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("WWW-Authenticate", "Bearer"))
                .andExpect(jsonPath("$.errors").isMap());
        mvc.perform(get("/api/test/principal").header("Authorization", "Bearer invalid"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void signedTokenAuthenticatesTheSubject() throws Exception {
        mvc.perform(get("/api/test/principal").header("Authorization", bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.user_id").value(42));
    }

    @Test
    void malformedJsonAndValidationHaveStableErrors() throws Exception {
        mvc.perform(post("/api/test/body").header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON).content("{"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"));
        mvc.perform(post("/api/test/body").header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.display_name").exists());
    }

    @Test
    void businessConflictAndMissingRouteKeepHttpStatus() throws Exception {
        mvc.perform(get("/api/test/conflict").header("Authorization", bearer()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("SURVEY_REVISION_CONFLICT"));
        mvc.perform(get("/api/test/missing").header("Authorization", bearer()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
    }

    private String bearer() {
        return "Bearer " + jwtService.issueAccessToken(42).accessToken();
    }

    @RestController
    static class TestController {
        @GetMapping("/api/test/principal")
        ApiResponse.Success<Map<String, Long>> principal(@AuthenticationPrincipal Long userId) {
            return ApiResponse.success("OK", "조회했습니다.", Map.of("user_id", userId));
        }

        @PostMapping("/api/test/body")
        ApiResponse.Success<Body> body(@Valid @RequestBody Body body) {
            return ApiResponse.success("OK", "조회했습니다.", body);
        }

        @GetMapping("/api/test/conflict")
        void conflict() {
            throw new ApiException(ErrorCode.SURVEY_REVISION_CONFLICT);
        }

        record Body(@NotBlank String displayName) {
        }
    }
}
