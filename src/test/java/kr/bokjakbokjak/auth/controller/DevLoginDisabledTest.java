package kr.bokjakbokjak.auth.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Base64;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

/** DEV_LOGIN_KEY가 비어 있으면 테스트 로그인 API는 없는 것처럼 동작한다. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DevLoginDisabledTest {
    @Autowired MockMvc mvc;

    @DynamicPropertySource
    static void config(DynamicPropertyRegistry registry) {
        registry.add("app.jwt.secret-base64", () -> Base64.getEncoder().encodeToString(new byte[32]));
    }

    @Test
    void devLoginIsHiddenWithoutKey() throws Exception {
        mvc.perform(post("/api/auth/dev-login")
                        .header("X-Dev-Login-Key", "")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"user_id\":1000}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
    }
}
