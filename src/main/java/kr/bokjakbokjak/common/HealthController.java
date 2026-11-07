package kr.bokjakbokjak.common;

import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {
    @GetMapping("/api/health")
    ApiResponse.Success<Map<String, String>> health() {
        return ApiResponse.success(Map.of("status", "UP"));
    }
}
