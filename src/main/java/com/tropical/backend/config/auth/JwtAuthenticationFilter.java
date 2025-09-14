package com.tropical.backend.config.auth;

import com.tropical.backend.auth.entity.User;
import com.tropical.backend.auth.service.UserService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.Optional;

/**
 * JWT 인증 필터 (쿠키 지원 추가)
 *
 * <p>
 * HTTP 요청에서 JWT 토큰을 추출하여 검증하고, 유효한 토큰인 경우
 * Spring Security Context에 사용자 인증 정보를 설정하는 필터입니다.
 * Authorization 헤더와 ACCESS_TOKEN 쿠키를 모두 지원합니다.
 * </p>
 *
 * <p>주요 기능:</p>
 * <ul>
 *   <li>Authorization 헤더에서 Bearer 토큰 추출 (1순위)</li>
 *   <li>ACCESS_TOKEN 쿠키에서 토큰 추출 (2순위)</li>
 *   <li>JWT 토큰 유효성 검증</li>
 *   <li>토큰에서 사용자 정보 추출 및 검증</li>
 *   <li>Spring Security Context에 인증 정보 설정</li>
 *   <li>인증 실패 시 적절한 로그 기록</li>
 * </ul>
 *
 * @author 왕택준
 * @version 0.3
 * @since 2025.09.14
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String ACCESS_TOKEN_COOKIE = "ACCESS_TOKEN";

    private final JwtTokenProvider jwtTokenProvider;
    private final UserService userService;

    /**
     * JWT 토큰을 검증하고 인증 정보를 SecurityContext에 설정
     *
     * @param request     HTTP 요청 객체
     * @param response    HTTP 응답 객체
     * @param filterChain 필터 체인
     * @throws ServletException 필터 처리 중 서블릿 예외
     * @throws IOException      입출력 예외
     */
    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        try {
            // 1. 요청에서 JWT 토큰 추출 (헤더 > 쿠키 순서)
            String token = extractTokenFromRequest(request);

            // 2. 토큰이 있고 유효한 경우 인증 처리
            if (StringUtils.hasText(token) && jwtTokenProvider.validateToken(token)) {
                processAuthentication(request, token);
            }

        } catch (Exception e) {
            // JWT 처리 중 예외가 발생해도 요청은 계속 진행
            // 인증이 필요한 엔드포인트에서는 SecurityConfig에서 401 응답 처리
            log.warn("JWT 인증 처리 중 오류 발생: {}", e.getMessage());
            SecurityContextHolder.clearContext();
        }

        // 다음 필터로 요청 전달
        filterChain.doFilter(request, response);
    }

    /**
     * HTTP 요청에서 JWT 토큰 추출 (헤더 > 쿠키 순서)
     *
     * <p>
     * 1순위: Authorization 헤더에서 Bearer 토큰
     * 2순위: ACCESS_TOKEN 쿠키에서 토큰
     * </p>
     *
     * @param request HTTP 요청 객체
     * @return 추출된 JWT 토큰 문자열, 없는 경우 null
     */
    private String extractTokenFromRequest(HttpServletRequest request) {
        // 1순위: Authorization 헤더
        String bearerToken = request.getHeader(AUTHORIZATION_HEADER);
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(BEARER_PREFIX)) {
            String token = bearerToken.substring(BEARER_PREFIX.length());
            log.debug("JWT 토큰 추출 완료 (헤더) - URI: {}", request.getRequestURI());
            return token;
        }

        // 2순위: ACCESS_TOKEN 쿠키
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (ACCESS_TOKEN_COOKIE.equals(cookie.getName())) {
                    String token = cookie.getValue();
                    if (StringUtils.hasText(token)) {
                        log.debug("JWT 토큰 추출 완료 (쿠키) - URI: {}", request.getRequestURI());
                        return token;
                    }
                }
            }
        }

        return null;
    }

    /**
     * JWT 토큰을 사용하여 인증 처리
     *
     * <p>
     * 토큰에서 사용자 정보를 추출하고 데이터베이스에서 사용자를 조회한 후,
     * Spring Security Context에 인증 정보를 설정합니다.
     * </p>
     *
     * @param request HTTP 요청 객체
     * @param token   검증된 JWT 토큰
     */
    private void processAuthentication(HttpServletRequest request, String token) {
        try {
            // Access Token인지 확인
            if (!jwtTokenProvider.isAccessToken(token)) {
                log.warn("Access Token이 아닌 토큰으로 인증 시도: {}", request.getRequestURI());
                return;
            }

            // 토큰에서 사용자 정보 추출
            Long userId = jwtTokenProvider.getUserIdFromToken(token);
            String email = jwtTokenProvider.getEmailFromToken(token);

            // JWT의 사용자 정보와 DB의 사용자 정보가 일치하는지 검증
            Optional<User> userOpt = userService.findUserForTokenValidation(userId, email);

            if (userOpt.isEmpty()) {
                log.warn("JWT 토큰의 사용자 정보가 DB와 일치하지 않음 - ID: {}, Email: {}", userId, email);
                return;
            }

            User user = userOpt.get();

            // UserDetails 생성 및 SecurityContext에 인증 정보 설정
            UserDetails userDetails = createUserDetails(user);
            UsernamePasswordAuthenticationToken authenticationToken =
                    new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            userDetails.getAuthorities()
                    );

            authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authenticationToken);

            log.debug("JWT 인증 완료 - 사용자 ID: {}, URI: {}", userId, request.getRequestURI());

        } catch (Exception e) {
            log.warn("JWT 인증 처리 실패: {}", e.getMessage());
            SecurityContextHolder.clearContext();
        }
    }

    /**
     * User 엔터티를 UserDetails 객체로 변환
     *
     * <p>
     * Spring Security가 인식할 수 있는 UserDetails 인터페이스 구현체를 생성합니다.
     * 현재는 권한(ROLE)을 단순하게 처리하지만, 향후 역할 기반 권한 시스템 확장 가능합니다.
     * </p>
     *
     * @param user 사용자 엔터티
     * @return UserDetails 구현체
     */
    private UserDetails createUserDetails(User user) {
        return org.springframework.security.core.userdetails.User.builder()
                .username(String.valueOf(user.getId()))  // username에 사용자 ID 사용
                .password("")  // JWT 인증에서는 비밀번호 불필요
                .authorities(Collections.singletonList(() -> "ROLE_USER"))  // 기본 사용자 권한
                .accountExpired(false)
                .accountLocked(!user.isActive())  // 비활성 계정은 잠금 처리
                .credentialsExpired(false)
                .disabled(false)
                .build();
    }

    /**
     * 필터 적용 여부 결정
     *
     * <p>
     * 특정 경로에 대해서는 JWT 인증 필터를 건너뛸 수 있습니다.
     * Public 엔드포인트들은 JWT 필터를 거치지 않도록 설정되어 있습니다.
     * </p>
     *
     * @param request HTTP 요청 객체
     * @return 필터를 적용하지 않으려면 true, 적용하려면 false
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();

        // JWT 필터를 적용하지 않을 경로들 (Public 엔드포인트)
        return path.equals("/") ||
                path.startsWith("/api/test") ||
                path.startsWith("/api/health") ||
                path.startsWith("/h2-console") ||
                path.startsWith("/actuator") ||
                // === 인증이 필요없는 Auth 엔드포인트들 ===
                path.equals("/api/auth/signup") ||           // 회원가입
                path.startsWith("/api/auth/verify") ||       // 이메일 인증
                path.equals("/api/auth/login") ||            // 로그인
                path.equals("/api/auth/token/refresh") ||    // 토큰 갱신
                path.startsWith("/login") ||                 // OAuth2 로그인
                path.startsWith("/oauth2");                  // OAuth2 콜백

        // 참고: SecurityConfig에서 permitAll()로 설정된 경로들과 동일하게 유지해야 함
        // 이렇게 하면 Public 엔드포인트는 JWT 필터를 완전히 건너뛰어서 성능상 이점이 있음
    }

    /**
     * 현재 인증된 사용자 정보 추출 유틸리티 메서드
     *
     * <p>
     * Controller나 Service에서 현재 인증된 사용자 정보를 쉽게 가져올 수 있도록
     * 정적 유틸리티 메서드를 제공합니다.
     * </p>
     *
     * @return 현재 인증된 사용자 ID, 인증되지 않은 경우 null
     */
    public static Long getCurrentUserId() {
        try {
            Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();

            if (principal instanceof UserDetails) {
                UserDetails userDetails = (UserDetails) principal;
                return Long.valueOf(userDetails.getUsername());
            }

        } catch (Exception e) {
            log.debug("현재 사용자 ID 추출 실패: {}", e.getMessage());
        }

        return null;
    }

    /**
     * 현재 인증된 사용자의 인증 여부 확인
     *
     * @return 인증된 사용자가 있으면 true, 없으면 false
     */
    public static boolean isAuthenticated() {
        try {
            return SecurityContextHolder.getContext().getAuthentication() != null &&
                    SecurityContextHolder.getContext().getAuthentication().isAuthenticated() &&
                    !SecurityContextHolder.getContext().getAuthentication().getPrincipal().equals("anonymousUser");
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * SecurityContext 클리어 유틸리티 메서드
     *
     * <p>
     * 로그아웃이나 토큰 무효화 시 사용할 수 있는 메서드입니다.
     * </p>
     */
    public static void clearSecurityContext() {
        SecurityContextHolder.clearContext();
        log.debug("Security Context 클리어 완료");
    }
}