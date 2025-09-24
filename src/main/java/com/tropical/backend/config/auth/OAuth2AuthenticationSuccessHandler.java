package com.tropical.backend.config.auth;

import com.tropical.backend.auth.entity.SocialAccount;
import com.tropical.backend.auth.entity.User;
import com.tropical.backend.auth.service.SocialAccountService;
import com.tropical.backend.auth.service.UserService;
import com.tropical.backend.common.util.CookieUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;

/**
 * OAuth2 로그인 성공 처리 핸들러 (환경변수 기반 동적 URL 지원)
 *
 * <p>
 * 소셜 로그인 성공 후 사용자 정보를 처리하고 적절한 페이지로 리다이렉트합니다.
 * 환경변수를 통해 동적 호스트를 지원하여 localhost와 IP 주소 접속을 모두 처리할 수 있습니다.
 * 신규 사용자는 온보딩 페이지로, 기존 사용자는 대시보드로 안내합니다.
 * </p>
 *
 * <p>주요 기능:</p>
 * <ul>
 *   <li>환경변수 기반 동적 프론트엔드 URL 생성 (USE_DYNAMIC_HOST)</li>
 *   <li>온보딩 미완료 사용자에게 ONBOARDING_TOKEN 발급</li>
 *   <li>온보딩 완료 사용자에게 정식 ACCESS_TOKEN/REFRESH_TOKEN 발급</li>
 *   <li>localhost와 IP 주소 접속 모두 지원</li>
 * </ul>
 *
 * @author 왕택준
 * @version 0.2
 * @since 2025.09.24
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final UserService userService;
    private final SocialAccountService socialAccountService;
    private final JwtTokenProvider jwtTokenProvider;

    /**
     * 프론트엔드 포트 번호
     *
     * <p>동적 URL 생성 시 사용될 프론트엔드 포트</p>
     */
    @Value("${app.frontend.port:5005}")
    private String frontendPort;

    /**
     * 동적 호스트 사용 여부
     *
     * <p>true: 현재 요청 호스트에 맞춰 동적 생성, false: localhost 고정</p>
     */
    @Value("${app.frontend.use-dynamic-host:true}")
    private boolean useDynamicHost;

    /**
     * 운영환경용 고정 프론트엔드 도메인
     *
     * <p>값이 있으면 동적 호스트보다 우선 적용됨</p>
     */
    @Value("${app.frontend.domain:}")
    private String frontendDomain;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {

        OAuth2AuthenticationToken oauth2Token = (OAuth2AuthenticationToken) authentication;
        OAuth2User oauth2User = oauth2Token.getPrincipal();
        String registrationId = oauth2Token.getAuthorizedClientRegistrationId();

        log.info("OAuth2 로그인 성공 - registrationId: {}", registrationId);

        try {
            // 소셜 제공자 변환
            SocialAccount.SocialProvider provider = socialAccountService.getSocialProviderFromRegistrationId(registrationId);

            // 소셜 사용자 정보 추출
            SocialUserInfo socialUserInfo = extractSocialUserInfo(provider, oauth2User);

            // 기존 소셜 계정 조회
            Optional<SocialAccount> existingSocialAccount = socialAccountService
                    .findActiveSocialAccount(provider, socialUserInfo.providerId());

            User user;
            boolean isNewUser = false;

            if (existingSocialAccount.isPresent()) {
                Long userId = existingSocialAccount.get().getUser().getId(); // ID만 안전하게 추출
                user = userService.getById(userId);
                userService.updateLastLoginTime(user.getId());
                log.info("기존 소셜 사용자 로그인 - 사용자 ID: {}, 제공자: {}", user.getId(), provider);
            } else {
                // 신규 사용자 생성
                user = userService.createSocialUser(socialUserInfo.email(), socialUserInfo.nickname());
                socialAccountService.createOrGetSocialAccount(user, provider, socialUserInfo.providerId());
                isNewUser = true;
                log.info("신규 소셜 사용자 생성 - 사용자 ID: {}, 제공자: {}", user.getId(), provider);
            }

            // 동적 리다이렉트 URL 결정
            String redirectUrl = determineRedirectUrl(request, user, isNewUser);

            // 온보딩이 필요한 경우 임시 토큰 발급, 완료된 경우 정식 토큰 발급
            if (!user.isOnboardingCompleted()) {
                // 온보딩용 토큰 발급 (30분)
                String onboardingToken = jwtTokenProvider.createOnboardingToken(user.getId(), user.getEmail());
                response.addCookie(CookieUtil.build("ONBOARDING_TOKEN", onboardingToken, 1800)); // 30분
                log.info("온보딩용 토큰 발급 - 사용자 ID: {}", user.getId());
            } else {
                // 정식 JWT 토큰 발급
                String accessToken = jwtTokenProvider.createAccessToken(user.getId(), user.getEmail());
                String refreshToken = jwtTokenProvider.createRefreshToken(user.getId());

                int accessMaxAge = jwtTokenProvider.getAccessMaxAge();
                int refreshMaxAge = jwtTokenProvider.getRefreshMaxAge();

                response.addCookie(CookieUtil.build("ACCESS_TOKEN", accessToken, accessMaxAge));
                response.addCookie(CookieUtil.build("REFRESH_TOKEN", refreshToken, refreshMaxAge));
                log.info("정식 JWT 토큰 발급 - 사용자 ID: {}", user.getId());
            }

            // 프론트엔드로 동적 리다이렉트
            getRedirectStrategy().sendRedirect(request, response, redirectUrl);

        } catch (Exception e) {
            log.error("OAuth2 로그인 처리 실패 - registrationId: {}, 오류: {}", registrationId, e.getMessage(), e);

            // 에러 시에도 동적 URL 사용
            String frontendBaseUrl = getFrontendBaseUrl(request);
            String errorUrl = UriComponentsBuilder.fromUriString(frontendBaseUrl + "/login")
                    .queryParam("error", "oauth2_failed")
                    .build().toUriString();

            getRedirectStrategy().sendRedirect(request, response, errorUrl);
        }
    }

    /**
     * 환경변수 기반 프론트엔드 URL 동적 생성
     *
     * <p>
     * 설정 우선순위에 따라 프론트엔드 URL을 결정합니다:
     * 1순위: 운영환경 고정 도메인 (FRONTEND_DOMAIN)
     * 2순위: 동적 호스트 (현재 요청 호스트 + 프론트엔드 포트)
     * 3순위: localhost 폴백
     * </p>
     *
     * <p>동적 URL 생성 예시:</p>
     * <ul>
     *   <li>localhost:9005 요청 → http://localhost:5005</li>
     *   <li>IPv4 주소:9005 요청 → http://IPv4 주소:5005</li>
     *   <li>FRONTEND_DOMAIN 설정 시 → 설정된 도메인 사용</li>
     * </ul>
     *
     * @param request HTTP 요청 객체 (호스트 정보 추출용)
     * @return 동적으로 생성된 프론트엔드 베이스 URL
     */
    private String getFrontendBaseUrl(HttpServletRequest request) {
        // 1순위: 운영환경에서 고정 도메인이 설정된 경우 우선 적용
        if (!frontendDomain.isEmpty()) {
            log.debug("고정 프론트엔드 도메인 사용: {}", frontendDomain);
            return frontendDomain;
        }

        // 2순위: 개발환경에서 동적 호스트 사용
        if (useDynamicHost) {
            String scheme = request.getScheme(); // http or https
            String serverName = request.getServerName(); // localhost or IP
            String dynamicUrl = String.format("%s://%s:%s", scheme, serverName, frontendPort);
            log.debug("동적 프론트엔드 URL 생성: {} (요청 호스트: {})", dynamicUrl, serverName);
            return dynamicUrl;
        }

        // 3순위: 폴백 - localhost 고정 사용
        String fallbackUrl = String.format("http://localhost:%s", frontendPort);
        log.debug("폴백 프론트엔드 URL 사용: {}", fallbackUrl);
        return fallbackUrl;
    }

    /**
     * 소셜 제공자별 사용자 정보 추출
     *
     * <p>
     * 각 소셜 제공자(Google, Kakao, Naver)의 API 응답 구조에 맞춰
     * 사용자 식별자, 이메일, 닉네임을 추출합니다.
     * </p>
     *
     * @param provider   소셜 제공자 (GOOGLE, KAKAO, NAVER)
     * @param oauth2User OAuth2 인증 후 받은 사용자 정보
     * @return 추출된 소셜 사용자 정보
     */
    private SocialUserInfo extractSocialUserInfo(SocialAccount.SocialProvider provider, OAuth2User oauth2User) {
        Map<String, Object> attributes = oauth2User.getAttributes();

        return switch (provider) {
            case GOOGLE -> {
                yield new SocialUserInfo(
                        (String) attributes.get("sub"),           // 구글 사용자 ID
                        (String) attributes.get("email"),        // 이메일
                        (String) attributes.get("name")          // 이름
                );
            }
            case KAKAO -> {
                Map<String, Object> kakaoAccount = (Map<String, Object>) attributes.get("kakao_account");
                Map<String, Object> profile = (Map<String, Object>) kakaoAccount.get("profile");

                yield new SocialUserInfo(
                        String.valueOf(attributes.get("id")),    // 카카오 사용자 ID
                        (String) kakaoAccount.get("email"),      // 이메일
                        (String) profile.get("nickname")         // 닉네임
                );
            }
            case NAVER -> {
                Map<String, Object> naverResponse = (Map<String, Object>) attributes.get("response");

                yield new SocialUserInfo(
                        (String) naverResponse.get("id"),        // 네이버 사용자 ID
                        (String) naverResponse.get("email"),     // 이메일
                        (String) naverResponse.get("name")       // 이름
                );
            }
        };
    }

    /**
     * 동적 리다이렉트 URL 결정
     *
     * <p>
     * 사용자 상태에 따라 적절한 페이지로 리다이렉트할 URL을 생성합니다.
     * 환경변수 설정에 따라 동적으로 호스트를 결정하여 IP 주소 접속을 지원합니다.
     * </p>
     *
     * <p>리다이렉트 규칙:</p>
     * <ul>
     *   <li>신규 사용자 → /onboarding (온보딩 진행)</li>
     *   <li>온보딩 미완료 사용자 → /onboarding (온보딩 완료 필요)</li>
     *   <li>온보딩 완료 사용자 → /dashboard (정상 사용)</li>
     * </ul>
     *
     * @param request   HTTP 요청 객체 (동적 호스트 결정용)
     * @param user      로그인한 사용자 정보
     * @param isNewUser 신규 사용자 여부
     * @return 리다이렉트할 전체 URL
     */
    private String determineRedirectUrl(HttpServletRequest request, User user, boolean isNewUser) {
        String frontendBaseUrl = getFrontendBaseUrl(request);

        if (isNewUser || !user.isOnboardingCompleted()) {
            // 신규 사용자 또는 온보딩 미완료 → 온보딩 페이지
            String onboardingUrl = frontendBaseUrl + "/onboarding";
            log.debug("온보딩 페이지로 리다이렉트: {}", onboardingUrl);
            return onboardingUrl;
        } else {
            // 기존 사용자 → 대시보드
            String dashboardUrl = frontendBaseUrl + "/dashboard";
            log.debug("대시보드로 리다이렉트: {}", dashboardUrl);
            return dashboardUrl;
        }
    }

    /**
     * 소셜 사용자 정보 레코드
     *
     * <p>
     * 소셜 제공자에서 추출한 사용자 정보를 담는 불변 객체입니다.
     * providerId는 각 소셜 서비스의 고유 사용자 식별자입니다.
     * </p>
     *
     * @param providerId 소셜 제공자별 고유 사용자 식별자
     * @param email      사용자 이메일 주소
     * @param nickname   사용자 닉네임 또는 이름
     */
    private record SocialUserInfo(String providerId, String email, String nickname) {
    }
}