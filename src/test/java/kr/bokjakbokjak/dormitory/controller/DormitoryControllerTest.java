package kr.bokjakbokjak.dormitory.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.util.Base64;
import java.util.List;
import kr.bokjakbokjak.common.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DormitoryControllerTest {
    @Autowired MockMvc mvc;
    @Autowired JwtService jwtService;
    @Autowired JdbcClient jdbc;

    @DynamicPropertySource
    static void config(DynamicPropertyRegistry registry) {
        registry.add("app.jwt.secret-base64", () -> Base64.getEncoder().encodeToString(new byte[32]));
    }

    @Test
    void searchShowsOwnSchoolTwoPersonRoomsGroupedByDormitory() throws Exception {
        long donggukUser = createUser("DONGGUK");

        String body = search(donggukUser, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("DORMITORY_LIST_SUCCESS"))
                .andReturn().getResponse().getContentAsString();

        List<String> names = JsonPath.read(body, "$.data.dormitories[*].name");
        assertThat(names).contains("고양학사", "남산학사");
        List<String> namsanRooms = JsonPath.read(body,
                "$.data.dormitories[?(@.dormitory_key == '남산학사')].room_types[*].name");
        assertThat(namsanRooms).containsExactly("2인실");
        List<Integer> capacities = JsonPath.read(body, "$.data.dormitories[*].room_types[*].capacity");
        assertThat(capacities).containsOnly(2);

        String filtered = search(donggukUser, " 남산 ").andReturn().getResponse().getContentAsString();
        assertThat((List<String>) JsonPath.read(filtered, "$.data.dormitories[*].name")).containsExactly("남산학사");

        String otherSchool = search(createUser("KOOKMIN"), null).andReturn().getResponse().getContentAsString();
        assertThat((List<String>) JsonPath.read(otherSchool, "$.data.dormitories[*].name"))
                .doesNotContain("남산학사", "고양학사");

        search(donggukUser, "가".repeat(81))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.keyword").exists());
    }

    @Test
    void registerReusesSameTagIgnoringSpaces() throws Exception {
        long userId = createUser("DONGGUK");

        String created = register(userId, "새봄 학사", "2인실 A", 2)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("DORMITORY_TAG_READY"))
                .andExpect(jsonPath("$.data.dormitory_key").value("새봄학사"))
                .andExpect(jsonPath("$.data.name").value("새봄 학사"))
                .andExpect(jsonPath("$.data.room_types[0].name").value("2인실 A"))
                .andReturn().getResponse().getContentAsString();
        int roomTypeId = JsonPath.read(created, "$.data.room_types[0].room_type_id");

        register(userId, " 새봄학사", "2인실A ", 2)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("새봄 학사"))
                .andExpect(jsonPath("$.data.room_types[0].room_type_id").value(roomTypeId));

        String found = search(userId, "새봄").andReturn().getResponse().getContentAsString();
        assertThat((List<Integer>) JsonPath.read(found, "$.data.dormitories[*].room_types[*].room_type_id"))
                .containsExactly(roomTypeId);

        long seeded = jdbc.sql("select id from dormitory_room_type where dormitory_key = '남산학사' and name = '2인실'")
                .query(Long.class).single();
        register(userId, "남산학사", "2인실", 2)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.room_types[0].room_type_id").value((int) seeded));
    }

    @Test
    void registerValidatesInput() throws Exception {
        long userId = createUser("DONGGUK");

        register(userId, "사인실학사", "4인실", 4)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("UNSUPPORTED_ROOM_CAPACITY"));
        register(userId, " ", "2인실", 2)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.name").exists());
    }

    private ResultActions search(long userId, String keyword) throws Exception {
        var request = get("/api/dormitories").header("Authorization", bearer(userId));
        if (keyword != null) {
            request.param("keyword", keyword);
        }
        return mvc.perform(request);
    }

    private ResultActions register(long userId, String name, String roomTypeName, int capacity) throws Exception {
        return mvc.perform(post("/api/dormitories")
                .header("Authorization", bearer(userId))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"name":"%s","room_type_name":"%s","capacity":%d}"""
                        .formatted(name, roomTypeName, capacity)));
    }

    private String bearer(long userId) {
        return "Bearer " + jwtService.issueAccessToken(userId).accessToken();
    }

    private long createUser(String universityCode) {
        var keyHolder = new GeneratedKeyHolder();
        jdbc.sql("""
                        insert into app_user (university_id, survey_revision, created_at, updated_at)
                        values ((select id from university where code = ?), 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)""")
                .param(universityCode)
                .update(keyHolder);
        return keyHolder.getKeyAs(Long.class);
    }
}
