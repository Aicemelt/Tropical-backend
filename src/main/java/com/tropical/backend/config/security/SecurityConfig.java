package com.tropical.backend.config.security;

import com.tropical.backend.config.auth.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Spring Security 보안 설정 클래스
 *
 * <p>
 * JWT 기반 인증 시스템과 CORS 설정을 통합한 Spring Security 보안 구성입니다.
 * 세션을 사용하지 않는 Stateless 방식으로 구성되어 있으며,
 * 인증 실패(401)와 권한 부족(403) 상황을 일관된 JSON 응답으로 처리합니다.
 * </p>
 *
 * <p>주요 기능:</p>
 * <ul>
 *   <li>JWT 인증 필터 체인 구성</li>
 *   <li>API 엔드포인트별 인증/인가 규칙 정의</li>
 *   <li>CORS 설정 통합 적용</li>
 *   <li>401/403 예외 처리 핸들러 바인딩</li>
 *   <li>CSRF 비활성화 및 세션 Stateless 설정</li>
 * </ul>
 *
 * @author 왕택준
 * @version 0.2
 * @since 2025.09.14
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;      // JWT 인증 필터
    private final AuthenticationEntryPoint jwtAuthenticationEntryPoint; // 401 인증 실패 처리
    private final CustomAccessDeniedHandler customAccessDeniedHandler;  // 403 권한 부족 처리

    /**
     * Spring Security 필터 체인 설정
     *
     * <p>
     * JWT 기반 인증을 위한 보안 필터 체인을 구성합니다.
     * API 서버 특성에 맞춰 CSRF를 비활성화하고 세션을 사용하지 않도록 설정하며,
     * CorsConfig에서 정의한 CORS 정책을 적용합니다.
     * </p>
     *
     * <p>보안 정책:</p>
     * <ul>
     *   <li>CSRF: API 서버이므로 비활성화</li>
     *   <li>세션: JWT 기반이므로 STATELESS 모드</li>
     *   <li>CORS: CorsConfig Bean의 설정 자동 적용</li>
     *   <li>필터 순서: JWT 필터를 UsernamePasswordAuthenticationFilter 앞에 배치</li>
     * </ul>
     *
     * <p>인가 규칙:</p>
     * <ul>
     *   <li>Public 엔드포인트: /api/health, /api/auth/*, OAuth2 경로</li>
     *   <li>인증 필요: 나머지 모든 API 엔드포인트</li>
     * </ul>
     *
     * @param http HttpSecurity 설정 객체
     * @return 구성된 SecurityFilterChain
     * @throws Exception 보안 설정 중 발생할 수 있는 예외
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // CSRF 비활성화: JWT 기반 API 서버에서는 불필요
                .csrf(csrf -> csrf.disable())

                // CORS 설정: CorsConfig의 Bean을 자동으로 적용
                .cors(cors -> {
                    // CorsConfig.corsConfigurationSource() Bean이 자동으로 적용됨
                })

                // 세션 관리: JWT 기반 Stateless 인증 사용
                .sessionManagement(sm -> sm
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // 예외 처리: 401(인증 실패)과 403(권한 부족) 핸들러 바인딩
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(jwtAuthenticationEntryPoint)  // 401 처리
                        .accessDeniedHandler(customAccessDeniedHandler)         // 403 처리
                )

                // 인가 규칙 정의: 엔드포인트별 접근 권한 설정
                .authorizeHttpRequests(auth -> auth
                        // Public 엔드포인트 (인증 불필요)
                        .requestMatchers(
                                "/api/health",           // 헬스 체크
                                "/api/auth/register",    // 회원가입
                                "/api/auth/verify",      // 이메일 인증
                                "/api/auth/login",       // 로그인
                                "/api/auth/refresh",     // 토큰 갱신
                                "/login/**",             // OAuth2 로그인 경로
                                "/oauth2/**"             // OAuth2 콜백 경로
                        ).permitAll()

                        // 나머지 모든 요청은 인증 필요
                        .anyRequest().authenticated()
                )

                // JWT 인증 필터를 Spring Security 필터 체인에 추가
                // UsernamePasswordAuthenticationFilter 앞에 배치하여 우선 처리
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    // 주의사항: CORS Bean은 CorsConfig에서 정의하므로 여기서는 생성하지 않습니다.
    // 중복 Bean 생성을 방지하여 설정 충돌을 방지합니다.
}