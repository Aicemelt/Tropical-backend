package com.tropical.backend.config.web;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * CORS(Cross-Origin Resource Sharing) 설정 클래스
 *
 * <p>
 * 프론트엔드와 백엔드 간의 Cross-Origin 요청을 안전하게 처리하기 위한
 * CORS 정책을 정의합니다. JWT 토큰 기반 인증과 쿠키 사용을 지원하도록
 * 설정되어 있습니다.
 * </p>
 *
 * <p>주요 기능:</p>
 * <ul>
 *   <li>허용된 오리진에서의 API 접근 허용</li>
 *   <li>Authorization 헤더와 쿠키 사용 지원</li>
 *   <li>프리플라이트 요청 처리 최적화</li>
 *   <li>응답 헤더 노출 설정으로 브라우저 접근성 향상</li>
 * </ul>
 *
 * @author 왕택준
 * @version 0.1
 * @since 2025.09.14
 */
@Configuration
public class CorsConfig {

    /**
     * CORS 설정을 위한 CorsConfigurationSource Bean 생성
     *
     * <p>
     * 프론트엔드(localhost:5005)와의 안전한 통신을 위해 CORS 정책을 설정합니다.
     * JWT 토큰과 쿠키 인증 방식을 모두 지원하며, 보안을 위해 정확한 오리진만 허용합니다.
     * </p>
     *
     * <p>설정된 정책:</p>
     * <ul>
     *   <li>허용 오리진: http://localhost:5005 (정확한 오리진 명시)</li>
     *   <li>허용 메서드: GET, POST, PUT, DELETE, PATCH, OPTIONS</li>
     *   <li>허용 헤더: 모든 헤더 (*)</li>
     *   <li>노출 헤더: Set-Cookie, Authorization (브라우저 접근용)</li>
     *   <li>인증 정보 허용: true (쿠키 및 Authorization 헤더 사용)</li>
     *   <li>프리플라이트 캐시: 3600초 (1시간)</li>
     * </ul>
     *
     * @return 설정된 CORS 정책이 적용된 CorsConfigurationSource
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration conf = new CorsConfiguration();

        // 정확한 오리진만 명시 (와일드카드 지양으로 보안 강화)
        conf.setAllowedOrigins(List.of("http://localhost:5005"));

        // 허용할 HTTP 메서드 (RESTful API 지원)
        conf.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));

        // 허용할 요청 헤더 (모든 헤더 허용)
        conf.setAllowedHeaders(List.of("*"));

        // 브라우저에서 읽을 수 있도록 노출할 응답 헤더
        // JWT 토큰과 쿠키 처리를 위해 필수
        conf.setExposedHeaders(List.of("Set-Cookie", "Authorization"));

        // 인증 정보 포함 허용 (쿠키, Authorization 헤더 사용 시 필수)
        conf.setAllowCredentials(true);

        // 프리플라이트 요청 캐시 시간(초) - 성능 최적화
        conf.setMaxAge(3600L);

        // 모든 경로에 CORS 설정 적용
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", conf);

        return source;
    }
}