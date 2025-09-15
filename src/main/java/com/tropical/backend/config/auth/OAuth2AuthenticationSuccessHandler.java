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
 * OAuth2 로그인 성공 처리 핸들러
 *
 * <p>
 * 소셜 로그인 성공 후 사용자 정보를 처리하고 적절한 페이지로 리다이렉트합니다.
 * 신규 사용자는 온보딩 페이지로, 기존 사용자는 대시보드로 안내합니다.
 * </p>
 *
 * @author 왕택준
 * @version 0.1
 * @since 2025.09.15
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final UserService userService;
    private final SocialAccountService socialAccountService;
    private final JwtTokenProvider jwtTokenProvider;

    @Value("${app.frontend.base-url:http://localhost:5005}")
    private String frontendBaseUrl;

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
                // 기존 사용자 로그인
                user = existingSocialAccount.get().getUser();
                userService.updateLastLoginTime(user.getId());
                log.info("기존 소셜 사용자 로그인 - 사용자 ID: {}, 제공자: {}", user.getId(), provider);
            } else {
                // 신규 사용자 생성
                user = userService.createSocialUser(socialUserInfo.email(), socialUserInfo.nickname());
                socialAccountService.createOrGetSocialAccount(user, provider, socialUserInfo.providerId());
                isNewUser = true;
                log.info("신규 소셜 사용자 생성 - 사용자 ID: {}, 제공자: {}", user.getId(), provider);
            }

            // 리다이렉트 URL 결정
            String redirectUrl = determineRedirectUrl(user, isNewUser);

            // 온보딩이 필요한 경우 임시 토큰 발급, 완료된 경우 정식 토큰 발급
            if (!user.isOnboardingCompleted()) {
                // 온보딩용 임시 토큰 발급 (30분)
                String tempToken = jwtTokenProvider.createAccessToken(user.getId(), user.getEmail());
                response.addCookie(CookieUtil.build("ACCESS_TOKEN", tempToken, 1800)); // 30분
                log.info("온보딩용 임시 토큰 발급 - 사용자 ID: {}", user.getId());
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

            // 프론트엔드로 리다이렉트
            getRedirectStrategy().sendRedirect(request, response, redirectUrl);

        } catch (Exception e) {
            log.error("OAuth2 로그인 처리 실패 - registrationId: {}, 오류: {}", registrationId, e.getMessage(), e);

            String errorUrl = UriComponentsBuilder.fromUriString(frontendBaseUrl + "/login")
                    .queryParam("error", "oauth2_failed")
                    .build().toUriString();

            getRedirectStrategy().sendRedirect(request, response, errorUrl);
        }
    }

    /**
     * 소셜 제공자별 사용자 정보 추출
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
     * 리다이렉트 URL 결정
     */
    private String determineRedirectUrl(User user, boolean isNewUser) {
        if (isNewUser || !user.isOnboardingCompleted()) {
            // 신규 사용자 또는 온보딩 미완료 → 온보딩 페이지
            return frontendBaseUrl + "/onboarding";
        } else {
            // 기존 사용자 → 대시보드
            return frontendBaseUrl + "/dashboard";
        }
    }

    /**
     * 소셜 사용자 정보 레코드
     */
    private record SocialUserInfo(String providerId, String email, String nickname) {
    }
}