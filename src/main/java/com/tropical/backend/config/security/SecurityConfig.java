package com.tropical.backend.config.security;

import com.tropical.backend.config.auth.CustomAccessDeniedHandler;
import com.tropical.backend.config.auth.JwtAuthenticationEntryPoint;
import com.tropical.backend.config.auth.JwtAuthenticationFilter;
import com.tropical.backend.config.auth.OAuth2AuthenticationSuccessHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Spring Security 보안 설정 클래스 (OAuth2 및 온보딩 토큰 지원)
 *
 * <p>
 * JWT 기반 인증 시스템과 OAuth2 소셜 로그인을 통합한 Spring Security 보안 구성입니다.
 * 세션을 사용하지 않는 Stateless 방식으로 구성되어 있으며,
 * 인증 실패(401)와 권한 부족(403) 상황을 일관된 JSON 응답으로 처리합니다.
 * </p>
 *
 * <p>주요 기능:</p>
 * <ul>
 *   <li>JWT 인증 필터 체인 구성 (ACCESS, ONBOARDING 토큰 지원)</li>
 *   <li>OAuth2 소셜 로그인 통합 (Google, Kakao, Naver)</li>
 *   <li>API 엔드포인트별 인증/인가 규칙 정의</li>
 *   <li>CORS 설정 통합 적용</li>
 *   <li>일관된 JSON 형식의 401/403 예외 처리</li>
 *   <li>CSRF 비활성화 및 세션 Stateless 설정</li>
 * </ul>
 *
 * <p>보안 정책:</p>
 * <ul>
 *   <li>인증 방식: JWT 토큰 기반 (쿠키 및 Authorization 헤더 지원)</li>
 *   <li>세션 관리: STATELESS (JWT 토큰으로만 상태 관리)</li>
 *   <li>CSRF: API 서버 특성상 비활성화</li>
 *   <li>권한 체계: ROLE_USER, ROLE_ONBOARDING으로 구분</li>
 * </ul>
 *
 * @author 왕택준
 * @version 0.5
 * @since 2025.09.15
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true) // 메서드 레벨 보안 어노테이션 활성화 (@PreAuthorize, @PostAuthorize)
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;           // JWT 인증 필터
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;   // 401 인증 실패 처리
    private final CustomAccessDeniedHandler customAccessDeniedHandler;       // 403 권한 부족 처리
    private final OAuth2AuthenticationSuccessHandler oauth2SuccessHandler;   // OAuth2 로그인 성공 처리

    /**
     * Spring Security 필터 체인 설정
     *
     * <p>
     * JWT 기반 인증과 OAuth2 소셜 로그인을 지원하는 보안 필터 체인을 구성합니다.
     * 온보딩 토큰 시스템을 통해 소셜 로그인 후 추가 정보 입력 단계를 안전하게 처리하며,
     * 기존 사용자와의 호환성을 유지하면서 점진적인 권한 시스템 도입을 지원합니다.
     * </p>
     *
     * <p>보안 설정 세부사항:</p>
     * <ul>
     *   <li><strong>CSRF 비활성화</strong>: REST API 특성상 CSRF 토큰 불필요</li>
     *   <li><strong>CORS 활성화</strong>: CorsConfig Bean 자동 적용</li>
     *   <li><strong>세션 정책</strong>: STATELESS (JWT만 사용, 서버 세션 없음)</li>
     *   <li><strong>OAuth2 통합</strong>: 소셜 로그인 후 온보딩 플로우 연동</li>
     * </ul>
     *
     * <p>필터 체인 순서:</p>
     * <ol>
     *   <li>CORS 필터 (Spring Boot 자동 설정)</li>
     *   <li>JWT 인증 필터 (커스텀)</li>
     *   <li>UsernamePasswordAuthenticationFilter (기본)</li>
     *   <li>OAuth2 로그인 필터 (Spring Security 제공)</li>
     * </ol>
     *
     * @param http HttpSecurity 설정 객체
     * @return 구성된 SecurityFilterChain
     * @throws Exception 보안 설정 중 발생할 수 있는 예외
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // ===============================
                // 기본 보안 정책 설정
                // ===============================

                // CSRF 비활성화: JWT 기반 API 서버에서는 불필요
                // SPA(Single Page Application)와 모바일 앱에서 사용하는 REST API는
                // 일반적으로 CSRF 공격에 취약하지 않음
                .csrf(csrf -> csrf.disable())

                // 폼 로그인 비활성화: /login 경로로 인한 500 에러 방지
                .formLogin(form -> form.disable())

                // CORS 설정: CorsConfig의 corsConfigurationSource() Bean을 자동으로 적용
                // 프론트엔드 도메인(localhost:5005)에서의 API 호출을 허용
                .cors(cors -> {
                    // CorsConfig.corsConfigurationSource() Bean이 자동으로 적용됨
                    // 별도 설정 불필요 - Spring Boot의 자동 구성 활용
                })

                // 세션 관리: JWT 기반 Stateless 인증 사용
                // 서버에서 세션을 생성하거나 저장하지 않음
                // 모든 인증 정보는 JWT 토큰에 포함되어 클라이언트가 관리
                .sessionManagement(sm -> sm
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // ===============================
                // OAuth2 소셜 로그인 설정
                // ===============================

                // OAuth2 로그인 활성화 및 성공/실패 핸들러 설정
                .oauth2Login(oauth2 -> oauth2
                        // 소셜 로그인 성공 시 커스텀 핸들러로 온보딩 토큰 생성
                        .successHandler(oauth2SuccessHandler)
                        // 소셜 로그인 실패 시 프론트엔드 로그인 페이지로 리다이렉트
                        .failureUrl("/login?error=oauth2_failed")
                )

                // ===============================
                // 예외 처리 설정
                // ===============================

                // 인증/인가 실패 시 일관된 JSON 형식의 에러 응답 제공
                .exceptionHandling(ex -> ex
                        // 401 Unauthorized: 인증되지 않은 사용자가 보호된 리소스 접근 시
                        .authenticationEntryPoint(jwtAuthenticationEntryPoint)
                        // 403 Forbidden: 인증된 사용자지만 권한이 부족한 경우
                        .accessDeniedHandler(customAccessDeniedHandler)
                )

                // ===============================
                // URL별 접근 권한 설정
                // ===============================

                .authorizeHttpRequests(auth -> auth
                                // Public 엔드포인트: 인증 없이 누구나 접근 가능
                                .requestMatchers(

                                        // ===============================
                                        // 브라우저 잡음 제거용 경로
                                        // ===============================
                                        "/favicon.ico",                // 브라우저 파비콘 요청 (401 에러 방지)
                                        "/css/**", "/js/**", "/images/**",  // 정적 리소스 (필요시)
                                        "/.well-known/**",             // 브라우저 자동 요청 경로 (DevTools 등)
                                        "/error",                      // Spring Boot 기본 에러 페이지

                                        // ===============================
                                        // API 문서화 도구 (개발/테스트용)
                                        // ===============================
                                        "/swagger-ui/**",              // Swagger UI 메인 페이지 및 리소스
                                        "/swagger-ui.html",            // Swagger UI 직접 접근 경로
                                        "/v3/api-docs/**",             // OpenAPI 3.0 JSON/YAML 문서
                                        "/swagger-resources/**",       // Swagger 메타데이터 리소스
                                        "/webjars/**",                 // Swagger UI용 웹 라이브러리 (Bootstrap, jQuery 등)

                                        // ===============================
                                        // 공휴일 정보 조회 API (Public)
                                        // ===============================
                                        "/api/holidays/**",          // 모든 공휴일 관련 엔드포인트

                                        // ===============================
                                        // 실제 서비스 공개 API
                                        // ===============================
                                        "/api/health",                 // 서버 상태 체크 (모니터링용)
                                        "/api/auth/signup",            // 이메일 회원가입
                                        "/api/auth/verify",            // 이메일 인증 확인
                                        "/api/auth/verify/resend",     // 이메일 인증 코드 재발송
                                        "/api/auth/login",             // 이메일 로그인
                                        "/api/auth/token/refresh",     // JWT 토큰 갱신
                                        "/api/auth/logout",            // 로그아웃 (쿠키 삭제)
                                        "/login/**",                   // OAuth2 로그인 시작 경로
                                        "/oauth2/**"                   // OAuth2 콜백 및 처리 경로
                                ).permitAll()

                                // 온보딩 전용 엔드포인트: ONBOARDING 권한 또는 USER 권한 모두 허용
                                // 이렇게 설정하면:
                                // 1. 소셜 로그인 후 ONBOARDING 토큰으로 접근 가능
                                // 2. 기존 USER 권한 사용자도 온보딩 정보 수정 가능
                                .requestMatchers("/api/auth/onboarding")
                                .hasAnyRole("ONBOARDING", "USER")

                                // 나머지 모든 API 엔드포인트: 기본 인증만 필요 (권한 무관)
                                // 현재는 호환성을 위해 authenticated()만 체크
                                // 향후 권한 시스템이 완전히 구축되면 hasRole("USER")로 변경 예정
                                .anyRequest().authenticated()

                        /* 향후 마이그레이션 계획:
                         * 1단계 (현재): .anyRequest().authenticated() - 기존 호환성 유지
                         * 2단계: User 엔티티에 권한 필드 추가, 기존 사용자들에게 ROLE_USER 부여
                         * 3단계: .anyRequest().hasRole("USER")로 변경하여 엄격한 권한 제어
                         */
                )

                // ===============================
                // 커스텀 필터 추가
                // ===============================

                // JWT 인증 필터를 Spring Security 필터 체인에 추가
                // UsernamePasswordAuthenticationFilter 앞에 배치하여 JWT 토큰을 먼저 검사
                // 이렇게 하면 JWT 토큰이 있는 경우 폼 기반 인증을 건너뛸 수 있음
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /*
     * 추가 정보:
     *
     * 1. CORS 설정은 CorsConfig 클래스에서 별도 관리됨
     *    - CorsConfigurationSource Bean이 자동으로 적용됨
     *    - 중복 설정을 방지하여 설정 충돌 없음
     *
     * 2. JWT 필터는 OncePerRequestFilter를 상속받아 요청당 한 번만 실행됨
     *    - 토큰 추출: Authorization 헤더 > ACCESS_TOKEN 쿠키 순서
     *    - 토큰 검증: JwtTokenProvider에서 서명 및 만료시간 체크
     *    - 사용자 인증: UserService에서 DB 사용자 정보와 대조
     *
     * 3. OAuth2 플로우:
     *    - 사용자가 소셜 로그인 버튼 클릭 → /oauth2/authorization/{provider}
     *    - OAuth2 제공자에서 인증 후 /login/oauth2/code/{provider}로 콜백
     *    - OAuth2AuthenticationSuccessHandler에서 온보딩 토큰 생성 및 리다이렉트
     *
     * 4. 보안 고려사항:
     *    - JWT 시크릿 키는 환경변수로 관리 (application.yml에서 참조)
     *    - 토큰 만료시간은 설정 파일에서 조정 가능
     *    - HTTPS 사용을 강력히 권장 (운영 환경)
     */
}