package com.tropical.backend.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * OpenAPI/Swagger 설정 클래스.
 *
 * <p>
 * 개발/테스트 환경에서 Swagger UI를 통해 API 문서를 자동 생성합니다.
 * JWT Bearer 인증 스키마를 포함하여,
 * 보안이 필요한 API 엔드포인트에 대해 문서 상에서도 인증을 시도할 수 있습니다.
 * </p>
 *
 * <p>
 * 운영 환경(prod)에서는 보안을 위해 Swagger를 비활성화합니다.
 * </p>
 *
 * @author  왕택준
 * @version 0.1
 * @since   2025.09.17
 */
@Configuration
@Profile("!prod")  // 운영 환경에서는 Swagger 비활성화
public class OpenApiConfig {

    /**
     * OpenAPI 3.0 스펙 설정 Bean.
     *
     * <p>
     * - API 기본 정보(title, version, description)<br>
     * - JWT Bearer 인증 스키마 등록<br>
     * - SecurityRequirement를 통해 전역 인증 설정<br>
     * </p>
     *
     * @return OpenAPI 객체 (Swagger UI가 이를 기반으로 문서화)
     */
    @Bean
    public OpenAPI tropicalOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Tropical API")              // API 제목
                        .version("v1.0")                    // API 버전
                        .description("Tropical 캘린더 백엔드 API")) // API 설명
                .components(new Components()
                        .addSecuritySchemes("bearerAuth",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)   // 인증 타입: HTTP
                                        .scheme("bearer")                 // Bearer 인증
                                        .bearerFormat("JWT")))            // JWT 포맷 지정
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth")); // 보안 요구사항 추가
    }
}
