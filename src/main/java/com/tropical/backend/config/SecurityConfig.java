package com.tropical.backend.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

/**
 * Spring Security JWT 인증 설정
 *
 * <p>
 * JWT 기반 인증 시스템을 위한 Spring Security 설정입니다.
 * 세션을 사용하지 않는 Stateless 방식으로 구성되어 있습니다.
 * </p>
 *
 * <p>주요 기능:</p>
 * <ul>
 *   <li>JWT 인증 필터 등록</li>
 *   <li>인증이 필요한/불필요한 엔드포인트 구분</li>
 *   <li>인증 실패 시 401 응답 처리</li>
 *   <li>CORS 설정으로 프론트엔드 연동 지원</li>
 *   <li>PasswordEncoder Bean 제공</li>
 * </ul>
 *
 * @author 왕택준
 * @version 0.2
 * @since 2025.09.13
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    /**
     * Spring Security 필터 체인 설정
     *
     * <p>
     * JWT 기반 인증을 위한 필터 체인을 구성합니다.
     * 세션을 사용하지 않고 매 요청마다 JWT 토큰을 검증합니다.
     * </p>
     *
     * @param http HttpSecurity 객체
     * @return 설정된 SecurityFilterChain
     * @throws Exception 설정 중 발생할 수 있는 예외
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // CSRF 비활성화 (JWT 사용 시 불필요)
                .csrf(csrf -> csrf.disable())

                // CORS 설정 적용
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // 세션 사용하지 않음 (JWT Stateless 인증)
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // 인증 실패 시 처리
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(jwtAuthenticationEntryPoint))

                // 인증/인가 규칙 설정
                .authorizeHttpRequests(auth -> auth
                        // 인증 불필요 경로 (Public Endpoints)
                        .requestMatchers("/").permitAll()
                        .requestMatchers("/api/test/**").permitAll()
                        .requestMatchers("/api/health").permitAll()

                        // 회원가입/로그인 관련 (인증 불필요)
                        .requestMatchers("/api/auth/signup").permitAll()
                        .requestMatchers("/api/auth/login").permitAll()
                        .requestMatchers("/api/auth/email/send-verification").permitAll()
                        .requestMatchers("/api/auth/email/verify").permitAll()
                        .requestMatchers("/api/auth/token/refresh").permitAll()

                        // OAuth2 소셜 로그인 (추후 추가 예정)
                        .requestMatchers("/oauth2/**").permitAll()
                        .requestMatchers("/login/oauth2/**").permitAll()

                        // 개발 도구 (개발 환경에서만 사용)
                        .requestMatchers("/h2-console/**").permitAll()
                        .requestMatchers("/actuator/**").permitAll()

                        // 인증 필요 경로
                        .requestMatchers("/api/auth/onboarding").authenticated()
                        .requestMatchers("/api/auth/status").authenticated()
                        .requestMatchers("/api/auth/logout").authenticated()
                        .requestMatchers("/api/me/**").authenticated()
                        .requestMatchers("/api/calendar/**").authenticated()
                        .requestMatchers("/api/diary/**").authenticated()
                        .requestMatchers("/api/todo/**").authenticated()
                        .requestMatchers("/api/bucket/**").authenticated()
                        .requestMatchers("/api/smalltalk/**").authenticated()

                        // 나머지 모든 요청은 인증 필요
                        .anyRequest().authenticated()
                )

                // H2 콘솔 사용을 위한 설정 (개발 환경)
                .headers(headers -> headers
                        .frameOptions(frameOptions -> frameOptions.disable()))  // 최신 API 사용

                // JWT 인증 필터를 UsernamePasswordAuthenticationFilter 앞에 추가
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * CORS 설정
     *
     * <p>
     * 프론트엔드와의 연동을 위한 Cross-Origin 요청 허용 설정입니다.
     * JWT 토큰을 Authorization 헤더로 전송할 수 있도록 구성되어 있습니다.
     * </p>
     *
     * @return CORS 설정이 적용된 CorsConfigurationSource
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // 허용할 오리진 (프론트엔드 도메인)
        configuration.setAllowedOrigins(Arrays.asList(
                "http://localhost:3000",  // React 개발 서버
                "http://localhost:5005"   // 설정 파일의 프론트엔드 URL
        ));

        // 허용할 HTTP 메서드
        configuration.setAllowedMethods(Arrays.asList(
                "GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"
        ));

        // 허용할 헤더 (JWT Authorization 헤더 포함)
        configuration.setAllowedHeaders(Arrays.asList(
                "Authorization",
                "Content-Type",
                "X-Requested-With",
                "Accept",
                "Origin",
                "Access-Control-Request-Method",
                "Access-Control-Request-Headers"
        ));

        // 노출할 헤더 (프론트엔드에서 읽을 수 있는 헤더)
        configuration.setExposedHeaders(Arrays.asList(
                "Authorization",
                "Content-Disposition"
        ));

        // 인증 정보 포함 허용 (JWT 토큰, 쿠키 등)
        configuration.setAllowCredentials(true);

        // preflight 요청 캐시 시간 (초)
        configuration.setMaxAge(3600L);

        // 모든 경로에 CORS 설정 적용
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);

        return source;
    }
}