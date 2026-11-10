package kr.bokjakbokjak.common;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {
    private static final String BEARER = "bearerAuth";

    @Bean
    OpenAPI bokjakOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("복작복작 API")
                        .description("대학생 기숙사 룸메이트 매칭 서비스")
                        .version("v1"))
                // 상대 경로로 두면 Nginx 뒤 HTTPS 도메인에서도 Try it out이 같은 주소로 요청한다
                .servers(List.of(new Server().url("/")))
                // 오른쪽 위 Authorize 버튼에 액세스 토큰을 넣으면 모든 요청에 Bearer 헤더가 붙는다
                .components(new Components().addSecuritySchemes(BEARER, new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER));
    }
}
