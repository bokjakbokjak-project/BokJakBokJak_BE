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

/** 체험 로그인을 끄면 없는 API처럼 동작한다. */
@SpringBootTest(properties = "app.auth.guest-login-enabled=false")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class GuestLoginDisabledTest {
    @Autowired MockMvc mvc;

    @DynamicPropertySource
    static void config(DynamicPropertyRegistry registry) {
        registry.add("app.jwt.secret-base64", () -> Base64.getEncoder().encodeToString(new byte[32]));
    }

    @Test
    void guestLoginIsHiddenWhenDisabled() throws Exception {
        mvc.perform(post("/api/auth/guest-login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nickname\":\"체험자\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
    }
}
