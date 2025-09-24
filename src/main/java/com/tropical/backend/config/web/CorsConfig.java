package com.tropical.backend.config.web;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * CORS(Cross-Origin Resource Sharing) 동적 설정 클래스
 *
 * <p>
 * 환경변수를 통해 허용된 호스트 목록을 관리하여 개발환경에서
 * localhost와 IP 주소 접속을 모두 지원합니다. JWT 토큰 기반 인증과
 * 쿠키 사용을 지원하도록 설정되어 있습니다.
 * </p>
 *
 * <p>주요 기능:</p>
 * <ul>
 *   <li>환경변수 기반 동적 오리진 생성 (ALLOWED_HOSTS)</li>
 *   <li>localhost와 IP 주소 접속 모두 지원</li>
 *   <li>Authorization 헤더와 쿠키 사용 지원</li>
 *   <li>프리플라이트 요청 처리 최적화</li>
 * </ul>
 *
 * @author 왕택준
 * @version 0.2
 * @since 2025.09.24
 */
@Slf4j
@Configuration
public class CorsConfig {

    /**
     * 허용된 호스트 목록 (환경변수에서 가져옴)
     *
     * <p>쉼표로 구분된 호스트 목록 (예: localhost,172.30.1.26,192.168.1.100)</p>
     */
    @Value("${app.allowed-hosts:localhost}")
    private String allowedHostsStr;

    /**
     * 프론트엔드 포트 번호
     *
     * <p>CORS 오리진 생성 시 사용될 프론트엔드 포트</p>
     */
    @Value("${app.frontend.port:5005}")
    private String frontendPort;

    /**
     * 환경변수 기반 동적 CORS 설정
     *
     * <p>
     * ALLOWED_HOSTS 환경변수에서 허용된 호스트 목록을 읽어와서
     * 동적으로 CORS 오리진을 생성합니다. 이를 통해 개발환경에서
     * localhost든 IP 주소든 자유롭게 접속할 수 있습니다.
     * </p>
     *
     * <p>동적 오리진 생성 예시:</p>
     * <ul>
     *   <li>ALLOWED_HOSTS=localhost → http://localhost:5005</li>
     *   <li>ALLOWED_HOSTS=localhost,172.30.1.26 → http://localhost:5005, http://172.30.1.26:5005</li>
     * </ul>
     *
     * <p>설정된 정책:</p>
     * <ul>
     *   <li>허용 오리진: 환경변수 기반 동적 생성</li>
     *   <li>허용 메서드: GET, POST, PUT, DELETE, PATCH, OPTIONS</li>
     *   <li>허용 헤더: 모든 헤더 (*)</li>
     *   <li>노출 헤더: Set-Cookie, Authorization (브라우저 접근용)</li>
     *   <li>인증 정보 허용: true (쿠키 및 Authorization 헤더 사용)</li>
     *   <li>프리플라이트 캐시: 3600초 (1시간)</li>
     * </ul>
     *
     * @return 환경변수 기반으로 설정된 CorsConfigurationSource
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration conf = new CorsConfiguration();

        // 환경변수에서 허용된 호스트 목록을 읽어와 동적으로 오리진 생성
        String[] allowedHosts = allowedHostsStr.split(",");
        List<String> allowedOrigins = Arrays.stream(allowedHosts)
                .flatMap(host -> Stream.of(
                        String.format("http://%s:%s", host.trim(), frontendPort),
                        String.format("https://%s:%s", host.trim(), frontendPort)
                ))
                .collect(Collectors.toList());

        // 동적으로 생성된 오리진 목록 설정
        conf.setAllowedOrigins(allowedOrigins);

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

        // 설정된 오리진 목록 로깅 (디버깅용)
        log.info("CORS 허용 Origins: {}", allowedOrigins);

        return source;
    }
}