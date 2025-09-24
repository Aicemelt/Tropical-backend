package com.tropical.backend.config.auth;

import com.tropical.backend.auth.entity.User;
import com.tropical.backend.auth.service.UserService;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * JWT 인증 필터 (온보딩 토큰 지원 + 성능 최적화)
 *
 * <p>
 * HTTP 요청에서 JWT 토큰을 추출하여 검증하고, 유효한 토큰인 경우
 * Spring Security Context에 사용자 인증 정보를 설정하는 필터입니다.
 * Authorization 헤더와 ACCESS_TOKEN 쿠키를 모두 지원하며,
 * ACCESS 토큰과 ONBOARDING 토큰을 구분하여 적절한 권한을 부여합니다.
 * </p>
 *
 * <p>주요 기능:</p>
 * <ul>
 *   <li>Authorization 헤더에서 Bearer 토큰 추출 (1순위)</li>
 *   <li>ACCESS_TOKEN 쿠키에서 토큰 추출 (2순위)</li>
 *   <li>JWT 토큰 유효성 검증 및 타입별 권한 부여</li>
 *   <li>토큰에서 사용자 정보 추출 및 DB 검증</li>
 *   <li>Spring Security Context에 인증 정보 설정</li>
 *   <li>성능 최적화: 중복 파싱 방지, 중복 인증 방지</li>
 * </ul>
 *
 * <p>지원하는 토큰 타입:</p>
 * <ul>
 *   <li>ACCESS: 온보딩 완료 후 모든 API 접근 가능 (ROLE_USER)</li>
 *   <li>ONBOARDING: 소셜 로그인 후 온보딩 API만 접근 가능 (ROLE_ONBOARDING)</li>
 * </ul>
 *
 * @author 왕택준
 * @version 0.5
 * @since 2025.09.15
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String ACCESS_TOKEN_COOKIE = "ACCESS_TOKEN";
    private static final String ONBOARDING_TOKEN_COOKIE = "ONBOARDING_TOKEN";

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

        // 2순위: 쿠키에서 토큰 추출 (ACCESS_TOKEN, ONBOARDING_TOKEN 모두 확인)
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (ACCESS_TOKEN_COOKIE.equals(cookie.getName()) ||
                    ONBOARDING_TOKEN_COOKIE.equals(cookie.getName())) {
                    String token = cookie.getValue();
                    if (StringUtils.hasText(token)) {
                        log.debug("JWT 토큰 추출 완료 (쿠키: {}) - URI: {}",
                                cookie.getName(), request.getRequestURI());
                        return token;
                    }
                }
            }
        }

        return null;
    }

    /**
     * JWT 토큰을 사용하여 인증 처리 (MVP용 성능 최적화 버전)
     *
     * <p>
     * 토큰에서 사용자 정보를 추출하고 데이터베이스에서 사용자를 조회한 후,
     * Spring Security Context에 인증 정보를 설정합니다.
     * </p>
     *
     * <p>성능 최적화 적용사항:</p>
     * <ul>
     *   <li>중복 파싱 방지: Claims를 한 번만 추출하여 재사용</li>
     *   <li>중복 인증 방지: 이미 인증된 컨텍스트는 재처리 안함</li>
     *   <li>직접 토큰 타입 체크: isXXXToken() 대신 Claims 직접 사용</li>
     *   <li>권한 매핑 헬퍼: 가독성과 유지보수성 향상</li>
     * </ul>
     *
     * <p>MVP 권한 시스템:</p>
     * <ul>
     *   <li>ROLE_USER (일반 API 접근) + ROLE_ONBOARDING (온보딩 전용) 만 사용</li>
     *   <li>복잡한 권한 체계는 MVP 범위를 벗어나므로 의도적으로 제외됨</li>
     * </ul>
     *
     * @param request HTTP 요청 객체
     * @param token   검증된 JWT 토큰
     */
    private void processAuthentication(HttpServletRequest request, String token) {
        try {
            // 1. 이미 인증된 컨텍스트면 재세팅 금지 (중복 필터 진입 방지, 성능 향상)
            if (SecurityContextHolder.getContext().getAuthentication() != null) {
                return;
            }

            // 2. 중복 파싱 방지: 한 번만 Claims 추출하고 캐시해서 사용
            Claims claims = jwtTokenProvider.parseClaims(token);
            String tokenType = String.valueOf(claims.get("tokenType"));
            Long userId = Long.valueOf(claims.getSubject());
            String email = claims.get("email", String.class);

            // 3. 토큰 타입 체크: ACCESS 또는 ONBOARDING만 허용
            if (!"ACCESS".equals(tokenType) && !"ONBOARDING".equals(tokenType)) {
                log.warn("인증에 사용할 수 없는 토큰 타입: {} - URI: {}", tokenType, request.getRequestURI());
                return;
            }

            // 4. JWT의 사용자 정보와 DB의 사용자 정보가 일치하는지 검증
            Optional<User> userOpt = userService.findUserForTokenValidation(userId, email);
            if (userOpt.isEmpty()) {
                log.warn("JWT 토큰의 사용자 정보가 DB와 일치하지 않음 - ID: {}, Email: {}", userId, email);
                return;
            }

            User user = userOpt.get();

            // 5. 토큰 타입에 따른 권한 설정
            Collection<? extends GrantedAuthority> authorities = mapAuthorities(tokenType);
            if (authorities.isEmpty()) {
                log.warn("알 수 없는 토큰 타입으로 권한 매핑 실패: {}", tokenType);
                return;
            }

            // 6. UserDetails 생성 및 SecurityContext에 인증 정보 설정
            UserDetails userDetails = createUserDetails(user, authorities);
            UsernamePasswordAuthenticationToken authenticationToken =
                    new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            userDetails.getAuthorities()
                    );

            authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authenticationToken);

            log.debug("JWT 인증 완료 - 사용자 ID: {}, 토큰 타입: {}, 권한: {}, URI: {}",
                    userId, tokenType, authorities, request.getRequestURI());

        } catch (Exception e) {
            log.warn("JWT 인증 처리 실패: {}", e.getMessage());
            SecurityContextHolder.clearContext();
        }
    }

    /**
     * 토큰 타입에 따른 권한 매핑 (MVP 전용)
     *
     * <p>
     * MVP 범위 준수 - 권한 시스템 최소화<br>
     * 현재 MVP 단계에서는 단 2개의 권한만 사용합니다:
     * </p>
     * <ul>
     *   <li>ROLE_USER: 온보딩 완료 후 모든 API 접근 가능</li>
     *   <li>ROLE_ONBOARDING: 소셜 로그인 후 온보딩 API만 접근 가능</li>
     * </ul>
     *
     * <p>
     * ⚠️ 의도적으로 제외된 권한들 (MVP 범위 초과)<br>
     * 다음 권한들은 MVP에 불필요하므로 의도적으로 구현하지 않았습니다:
     * </p>
     * <ul>
     *   <li>ROLE_ADMIN - 관리자 기능이 MVP에 없음</li>
     *   <li>ROLE_MANAGER - 중간 관리 권한이 MVP에 불필요</li>
     *   <li>ROLE_PREMIUM - 유료 기능이 MVP 범위 밖</li>
     *   <li>세분화된 권한 (READ/WRITE/DELETE) - 과도한 엔지니어링</li>
     * </ul>
     *
     * <p>
     * YAGNI 원칙 적용<br>
     * "You Aren't Gonna Need It" - 현재 실제로 사용하는 기능만 구현하고,
     * 추가 권한은 비즈니스 요구사항이 명확해진 후 도입 예정입니다.
     * </p>
     *
     * <p>
     * 향후 확장 방법<br>
     * 새로운 권한이 필요할 때는 이 메서드에 case를 추가하고
     * SecurityConfig에서 해당 권한에 대한 엔드포인트 매핑을 설정하면 됩니다.
     * </p>
     *
     * @param tokenType JWT 토큰의 tokenType 클레임 값 ("ACCESS" 또는 "ONBOARDING")
     * @return 해당 토큰 타입에 맞는 권한 목록
     */
    private Collection<? extends GrantedAuthority> mapAuthorities(String tokenType) {
        return switch (tokenType) {
            case "ACCESS" -> List.of(new SimpleGrantedAuthority("ROLE_USER"));       // 정식 사용자 권한
            case "ONBOARDING" -> List.of(new SimpleGrantedAuthority("ROLE_ONBOARDING")); // 온보딩 전용 권한
            default -> List.of(); // 알 수 없는 토큰 타입은 빈 권한 반환 (보안상 안전)
        };
    }

    /**
     * User 엔터티를 UserDetails 객체로 변환 (권한을 파라미터로 받도록 수정)
     *
     * <p>
     * Spring Security가 인식할 수 있는 UserDetails 인터페이스 구현체를 생성합니다.
     * username에는 사용자 ID를 사용하여 이후 서비스에서 ID 기반 조회가 쉽도록 하며,
     * 토큰 타입에 따라 동적으로 권한을 설정할 수 있도록 파라미터로 받습니다.
     * </p>
     *
     * @param user        사용자 엔터티
     * @param authorities 부여할 권한 목록 (토큰 타입에 따라 결정됨)
     * @return UserDetails 구현체
     */
    private UserDetails createUserDetails(User user, Collection<? extends GrantedAuthority> authorities) {
        return org.springframework.security.core.userdetails.User.builder()
                .username(String.valueOf(user.getId()))    // 주 식별자로 사용자 ID 사용
                .password("")                              // JWT 인증에서는 비밀번호 불필요
                .authorities(authorities)                  // 파라미터로 받은 권한 설정
                .accountExpired(false)
                .accountLocked(!user.isActive())           // 비활성 계정은 잠금 처리
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