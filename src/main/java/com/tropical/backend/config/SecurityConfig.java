package com.tropical.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

/**
 * Spring Security 기본 설정
 *
 * <p>
 * 현재는 기본 인증 설정과 PasswordEncoder Bean만 제공합니다.
 * OAuth2와 JWT 처리는 추후 단계적으로 추가할 예정입니다.
 * </p>
 *
 * <p>주요 기능:</p>
 * <ul>
 *   <li>PasswordEncoder Bean 등록 (UserService에서 사용)</li>
 *   <li>CORS 설정 (프론트엔드 연동 준비)</li>
 *   <li>기본 Security 필터 체인 구성</li>
 *   <li>개발 단계를 위한 임시 permitAll 설정</li>
 * </ul>
 *
 * @author 왕택준
 * @version 0.1
 * @since 2025.09.13
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /**
     * 비밀번호 암호화 Bean
     *
     * <p>
     * UserService에서 로컬 계정 생성 시 비밀번호 해시화에 사용됩니다.
     * BCrypt 알고리즘을 사용하여 안전한 단방향 암호화를 제공합니다.
     * </p>
     *
     * @return BCrypt 기반 PasswordEncoder
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Spring Security 필터 체인 설정
     *
     * <p>
     * 현재는 개발 단계이므로 모든 경로를 허용합니다.
     * 추후 Controller 구현 후 적절한 인증/인가 규칙을 적용할 예정입니다.
     * </p>
     *
     * @param http HttpSecurity 객체
     * @return 설정된 SecurityFilterChain
     * @throws Exception 설정 중 발생할 수 있는 예외
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // CSRF 비활성화 (REST API 사용)
                .csrf(csrf -> csrf.disable())

                // CORS 설정 적용
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // 세션 사용하지 않음 (JWT 기반 인증 준비)
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // 인증/인가 규칙 설정
                .authorizeHttpRequests(auth -> auth
                        // TODO: 개발 단계 - 모든 요청 허용
                        // 추후 Controller 구현 후 적절한 권한 설정 필요
                        .requestMatchers("/api/auth/**").permitAll()  // 인증 관련 API
                        .requestMatchers("/h2-console/**").permitAll()  // H2 콘솔 (개발용)
                        .requestMatchers("/actuator/**").permitAll()  // 모니터링 (개발용)
                        .anyRequest().permitAll()  // 임시: 모든 요청 허용
                )

                // H2 콘솔 사용을 위한 설정
                .headers(headers -> headers
                        .frameOptions().disable());

        return http.build();
    }

    /**
     * CORS 설정
     *
     * <p>
     * 프론트엔드와의 연동을 위한 Cross-Origin 요청 허용 설정입니다.
     * application.yml의 cors 설정과 연동됩니다.
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

        // 허용할 헤더
        configuration.setAllowedHeaders(Arrays.asList("*"));

        // 인증 정보 포함 허용 (JWT 토큰 등)
        configuration.setAllowCredentials(true);

        // 모든 경로에 CORS 설정 적용
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);

        return source;
    }
}