package com.tropical.backend.auth.controller;

import com.tropical.backend.auth.dto.request.LoginRequest;
import com.tropical.backend.auth.dto.request.OnboardingRequest;
import com.tropical.backend.auth.dto.request.SignupRequest;
import com.tropical.backend.auth.dto.response.TokenResponse;
import com.tropical.backend.auth.dto.response.UserResponse;
import com.tropical.backend.auth.entity.User;
import com.tropical.backend.auth.service.EmailService;
import com.tropical.backend.auth.service.UserConsentService;
import com.tropical.backend.auth.service.UserService;
import com.tropical.backend.common.util.CookieUtil;
import com.tropical.backend.config.auth.JwtAuthenticationFilter;
import com.tropical.backend.config.auth.JwtTokenProvider;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.Map;

/**
 * JWT 기반 인증 API 컨트롤러 (이메일 인증 완료)
 *
 * <p>
 * JWT 토큰을 사용하는 로컬 계정 회원가입, 로그인, 온보딩 등
 * 사용자 인증과 관련된 REST API 엔드포인트를 제공합니다.
 * Authorization 헤더와 HttpOnly 쿠키를 모두 지원하는 하이브리드 방식입니다.
 * </p>
 *
 * <p>주요 기능:</p>
 * <ul>
 *   <li>로컬 계정 회원가입 (이메일 인증 메일 자동 발송)</li>
 *   <li>JWT 기반 로그인 및 토큰 발급 (이메일 인증 확인)</li>
 *   <li>이메일 인증 처리 및 재발송</li>
 *   <li>온보딩 프로세스 (JWT 인증 필요)</li>
 *   <li>사용자 인증 상태 확인</li>
 *   <li>로그아웃 처리 (쿠키 삭제 포함)</li>
 *   <li>토큰 갱신 (쿠키 업데이트 포함)</li>
 * </ul>
 *
 * @author 왕택준
 * @version 0.4
 * @since 2025.09.15
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final UserService userService;
    private final UserConsentService userConsentService;
    private final JwtTokenProvider jwtTokenProvider;
    private final EmailService emailService;

    @Value("${app.frontend.base-url:http://localhost:3000}")
    private String frontendBaseUrl;

    @Value("${app.backend.base-url:http://localhost:8080}")
    private String backendBaseUrl;

    /**
     * 로컬 계정 회원가입 (이메일 인증 메일 발송)
     *
     * <p>
     * 이메일과 비밀번호를 사용하는 로컬 계정을 생성합니다.
     * 회원가입 성공 후 이메일 인증 메일을 자동 발송하며,
     * JWT 토큰을 발급하여 자동 로그인 처리합니다.
     * Authorization 헤더용 응답과 함께 HttpOnly 쿠키로도 토큰을 제공합니다.
     * </p>
     *
     * @param signupRequest 회원가입 요청 정보 (이메일, 비밀번호, 닉네임)
     * @param response      HTTP 응답 객체 (쿠키 설정용)
     * @return JWT 토큰과 사용자 정보, 이메일 인증 필요 상태
     */
    @PostMapping("/signup")
    public ResponseEntity<?> signup(@Valid @RequestBody SignupRequest signupRequest,
                                    HttpServletResponse response) {
        log.info("로컬 계정 회원가입 요청 - 이메일: {}, 닉네임: {}",
                signupRequest.getEmail(), signupRequest.getNickname());

        try {
            // 로컬 사용자 생성 (emailVerified=false)
            User user = userService.createLocalUser(
                    signupRequest.getEmail(),
                    signupRequest.getPassword(),
                    signupRequest.getNickname()
            );

            // 이메일 인증 토큰 발급 및 발송
            String emailVerifyToken = jwtTokenProvider.createEmailVerifyToken(user.getId(), user.getEmail());
            String verifyUrl = backendBaseUrl + "/api/auth/verify?token=" + emailVerifyToken;
            emailService.sendVerificationMail(user.getEmail(), verifyUrl);

            // JWT 토큰 생성
            String accessToken = jwtTokenProvider.createAccessToken(user.getId(), user.getEmail());
            String refreshToken = jwtTokenProvider.createRefreshToken(user.getId());

            // Access Token 만료 시간 계산
            Date expirationDate = jwtTokenProvider.getExpirationFromToken(accessToken);
            LocalDateTime expiresAt = expirationDate.toInstant()
                    .atZone(ZoneId.systemDefault())
                    .toLocalDateTime();

            // 쿠키로 토큰 설정 (헤더와 병행)
            int accessMaxAge = jwtTokenProvider.getAccessMaxAge();
            int refreshMaxAge = jwtTokenProvider.getRefreshMaxAge();

            response.addCookie(CookieUtil.build("ACCESS_TOKEN", accessToken, accessMaxAge));
            response.addCookie(CookieUtil.build("REFRESH_TOKEN", refreshToken, refreshMaxAge));

            // 응답 생성 (기존 헤더 방식도 유지)
            TokenResponse.TokenResponseWithUser tokenResponse = TokenResponse.withUser(
                    accessToken, refreshToken, expiresAt, UserResponse.from(user)
            );

            log.info("로컬 계정 회원가입 성공 (쿠키+헤더) - 사용자 ID: {}, 이메일 인증 메일 발송", user.getId());

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "회원가입이 완료되었습니다. 이메일 인증을 진행해주세요.",
                    "data", tokenResponse,
                    "nextStep", "email_verification"
            ));

        } catch (IllegalArgumentException e) {
            log.warn("로컬 계정 회원가입 실패 - 이메일: {}, 사유: {}",
                    signupRequest.getEmail(), e.getMessage());

            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage(),
                    "errorCode", "SIGNUP_FAILED"
            ));
        }
    }

    /**
     * 로컬 계정 로그인 (이메일 인증 확인)
     *
     * <p>
     * 이메일과 비밀번호를 검증하여 JWT 토큰을 발급합니다.
     * 이메일 인증이 완료되지 않은 계정은 403으로 차단합니다.
     * 로그인 성공 시 사용자 정보와 온보딩 완료 상태를 함께 반환하며,
     * Authorization 헤더용 응답과 함께 HttpOnly 쿠키로도 토큰을 제공합니다.
     * </p>
     *
     * @param loginRequest 로그인 요청 정보 (이메일, 비밀번호)
     * @param response     HTTP 응답 객체 (쿠키 설정용)
     * @return JWT 토큰과 사용자 정보 및 인증 상태
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest loginRequest,
                                   HttpServletResponse response) {
        log.info("로컬 계정 로그인 요청 - 이메일: {}", loginRequest.getEmail());

        try {
            // 사용자 조회
            User user = userService.findActiveUserByEmail(loginRequest.getEmail())
                    .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 이메일입니다"));

            // 로컬 계정 확인
            if (!user.isLocalAccount()) {
                log.warn("소셜 계정으로 로컬 로그인 시도 - 이메일: {}", loginRequest.getEmail());
                throw new IllegalArgumentException("소셜 계정입니다. 해당 소셜 로그인을 이용해주세요.");
            }

            // 비밀번호 검증
            if (!userService.verifyPassword(user, loginRequest.getPassword())) {
                log.warn("로컬 계정 비밀번호 불일치 - 이메일: {}", loginRequest.getEmail());
                throw new IllegalArgumentException("비밀번호가 일치하지 않습니다");
            }

            // 이메일 인증 확인 (핵심 추가 부분)
            if (!user.isEmailVerified()) {
                log.warn("이메일 미인증 사용자 로그인 시도 - 이메일: {}", loginRequest.getEmail());
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                        "success", false,
                        "message", "이메일 인증이 필요합니다",
                        "errorCode", "EMAIL_NOT_VERIFIED",
                        "nextStep", "email_verification"
                ));
            }

            // JWT 토큰 생성
            String accessToken = jwtTokenProvider.createAccessToken(user.getId(), user.getEmail());
            String refreshToken = jwtTokenProvider.createRefreshToken(user.getId());

            // Access Token 만료 시간 계산
            Date expirationDate = jwtTokenProvider.getExpirationFromToken(accessToken);
            LocalDateTime expiresAt = expirationDate.toInstant()
                    .atZone(ZoneId.systemDefault())
                    .toLocalDateTime();

            // 쿠키로 토큰 설정 (헤더와 병행)
            int accessMaxAge = jwtTokenProvider.getAccessMaxAge();
            int refreshMaxAge = jwtTokenProvider.getRefreshMaxAge();

            response.addCookie(CookieUtil.build("ACCESS_TOKEN", accessToken, accessMaxAge));
            response.addCookie(CookieUtil.build("REFRESH_TOKEN", refreshToken, refreshMaxAge));

            // 마지막 로그인 시간 업데이트
            userService.updateLastLoginTime(user.getId());

            // 온보딩 완료 여부 확인
            String nextStep = user.isOnboardingCompleted() ? "dashboard" : "onboarding";

            // 응답 생성 (기존 헤더 방식도 유지)
            TokenResponse.TokenResponseWithUser tokenResponse = TokenResponse.withUser(
                    accessToken, refreshToken, expiresAt, UserResponse.from(user)
            );

            log.info("로컬 계정 로그인 성공 (쿠키+헤더) - 사용자 ID: {}, 다음 단계: {}", user.getId(), nextStep);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "로그인이 완료되었습니다",
                    "data", tokenResponse,
                    "nextStep", nextStep
            ));

        } catch (IllegalArgumentException e) {
            log.warn("로컬 계정 로그인 실패 - 이메일: {}, 사유: {}", loginRequest.getEmail(), e.getMessage());

            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage(),
                    "errorCode", "LOGIN_FAILED"
            ));
        }
    }

    /**
     * 이메일 인증 처리
     *
     * <p>
     * 이메일로 발송된 인증 링크를 통해 이메일 인증을 완료합니다.
     * 토큰을 검증한 후 사용자의 emailVerified 플래그를 true로 설정하고
     * 인증 완료 시간을 기록합니다.
     * </p>
     *
     * @param token    이메일 인증 토큰
     * @param response HTTP 응답 객체 (리다이렉트용)
     * @throws IOException 리다이렉트 실패 시
     */
    @GetMapping("/verify")
    public void verifyEmail(@RequestParam("token") String token,
                            HttpServletResponse response) throws IOException {
        try {
            Claims claims = jwtTokenProvider.parseClaims(token);

            // 토큰 타입 확인
            if (!"EMAIL_VERIFY".equals(String.valueOf(claims.get("tokenType")))) {
                log.warn("잘못된 토큰 타입으로 이메일 인증 시도");
                response.sendRedirect(frontendBaseUrl + "/verify-failed");
                return;
            }

            Long userId = Long.valueOf(claims.getSubject());
            String email = claims.get("email", String.class);

            // 이메일 인증 처리
            userService.markEmailVerified(userId, email);

            log.info("이메일 인증 완료 - 사용자 ID: {}, 이메일: {}", userId, email);
            response.sendRedirect(frontendBaseUrl + "/verified");

        } catch (Exception e) {
            log.warn("이메일 인증 실패 - 토큰: {}, 사유: {}", token, e.getMessage());
            response.sendRedirect(frontendBaseUrl + "/verify-failed");
        }
    }

    /**
     * 이메일 인증 메일 재발송
     *
     * <p>
     * 현재 로그인된 사용자에게 이메일 인증 메일을 재발송합니다.
     * 이미 인증된 사용자나 소셜 계정 사용자는 재발송할 수 없습니다.
     * </p>
     *
     * @return 재발송 결과
     */
    @PostMapping("/verify/resend")
    public ResponseEntity<?> resendVerificationEmail() {
        Long userId = JwtAuthenticationFilter.getCurrentUserId();
        if (userId == null) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "인증 정보를 찾을 수 없습니다",
                    "errorCode", "AUTHENTICATION_REQUIRED"
            ));
        }

        log.info("이메일 인증 메일 재발송 요청 - 사용자 ID: {}", userId);

        try {
            User user = userService.findActiveUserById(userId)
                    .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다"));

            // 이미 인증된 경우
            if (user.isEmailVerified()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "이미 이메일 인증이 완료되었습니다",
                        "errorCode", "ALREADY_VERIFIED"
                ));
            }

            // 소셜 계정인 경우
            if (user.isSocialAccount()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "소셜 계정은 이메일 인증이 필요하지 않습니다",
                        "errorCode", "SOCIAL_ACCOUNT"
                ));
            }

            // 인증 메일 재발송
            String emailVerifyToken = jwtTokenProvider.createEmailVerifyToken(user.getId(), user.getEmail());
            String verifyUrl = backendBaseUrl + "/api/auth/verify?token=" + emailVerifyToken;
            emailService.sendVerificationMail(user.getEmail(), verifyUrl);

            log.info("이메일 인증 메일 재발송 완료 - 사용자 ID: {}", userId);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "인증 메일이 재발송되었습니다"
            ));

        } catch (IllegalArgumentException e) {
            log.warn("이메일 인증 메일 재발송 실패 - 사용자 ID: {}, 사유: {}", userId, e.getMessage());

            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage(),
                    "errorCode", "RESEND_FAILED"
            ));
        }
    }

    /**
     * 온보딩 완료 처리 (JWT 인증 필요)
     *
     * <p>
     * JWT 토큰으로 인증된 사용자의 필수 동의와 선택 동의를 처리하여 온보딩을 완료합니다.
     * SecurityContext에서 현재 인증된 사용자 정보를 추출합니다.
     * </p>
     *
     * @param onboardingRequest 동의 정보 (필수 동의, 선택 동의)
     * @return 온보딩 완료 결과
     */
    @PostMapping("/onboarding")
    public ResponseEntity<?> completeOnboarding(@Valid @RequestBody OnboardingRequest onboardingRequest) {

        // JWT 필터에서 설정한 SecurityContext에서 현재 사용자 ID 추출
        Long userId = JwtAuthenticationFilter.getCurrentUserId();
        if (userId == null) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "인증 정보를 찾을 수 없습니다",
                    "errorCode", "AUTHENTICATION_REQUIRED"
            ));
        }

        log.info("온보딩 완료 요청 - 사용자 ID: {}", userId);

        try {
            // 동의 정보 처리
            boolean consentResult = userConsentService.processOnboardingConsents(
                    userId,
                    onboardingRequest.getAllConsents()
            );

            if (!consentResult) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "필수 동의 항목이 누락되었습니다",
                        "errorCode", "REQUIRED_CONSENT_MISSING"
                ));
            }

            // 온보딩 완료 처리
            boolean onboardingResult = userService.completeOnboarding(userId);

            if (!onboardingResult) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "온보딩 완료 처리 중 오류가 발생했습니다",
                        "errorCode", "ONBOARDING_FAILED"
                ));
            }

            // 완료된 사용자 정보 조회
            User user = userService.findActiveUserById(userId)
                    .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다"));

            log.info("온보딩 완료 성공 - 사용자 ID: {}", userId);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "온보딩이 완료되었습니다",
                    "user", UserResponse.from(user),
                    "nextStep", "dashboard"
            ));

        } catch (IllegalArgumentException e) {
            log.warn("온보딩 완료 실패 - 사용자 ID: {}, 사유: {}", userId, e.getMessage());

            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage(),
                    "errorCode", "ONBOARDING_FAILED"
            ));
        }
    }

    /**
     * 사용자 인증 상태 확인 (JWT 인증 필요)
     *
     * <p>
     * 현재 JWT 토큰으로 인증된 사용자의 상태와 온보딩 완료 여부를 확인합니다.
     * SecurityContext에서 인증 정보를 추출하여 사용자 상태를 반환합니다.
     * emailVerified 정보도 포함하여 프론트엔드에서 다음 단계를 결정할 수 있습니다.
     * </p>
     *
     * @return 사용자 인증 상태 정보
     */
    @GetMapping("/status")
    public ResponseEntity<?> getAuthStatus() {

        // JWT 필터에서 설정한 SecurityContext에서 현재 사용자 ID 추출
        Long userId = JwtAuthenticationFilter.getCurrentUserId();
        if (userId == null) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "인증 정보를 찾을 수 없습니다",
                    "authenticated", false
            ));
        }

        log.debug("사용자 인증 상태 확인 - 사용자 ID: {}", userId);

        try {
            User user = userService.findActiveUserById(userId)
                    .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다"));

            String nextStep;
            if (!user.isEmailVerified()) {
                nextStep = "email_verification";
            } else if (!user.isOnboardingCompleted()) {
                nextStep = "onboarding";
            } else {
                nextStep = "dashboard";
            }

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "user", UserResponse.from(user),
                    "nextStep", nextStep,
                    "authenticated", true,
                    "emailVerified", user.isEmailVerified()  // 핵심 추가 부분
            ));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage(),
                    "authenticated", false
            ));
        }
    }

    /**
     * 로그아웃 처리 (쿠키 삭제 포함)
     *
     * <p>
     * JWT 토큰 쿠키를 만료시키고 SecurityContext를 클리어합니다.
     * 클라이언트에서도 localStorage의 토큰을 삭제해야 완전한 로그아웃이 됩니다.
     * </p>
     *
     * @param response HTTP 응답 객체 (쿠키 삭제용)
     * @return 로그아웃 완료 메시지
     */
    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletResponse response) {

        Long userId = JwtAuthenticationFilter.getCurrentUserId();
        log.info("로그아웃 요청 - 사용자 ID: {}", userId);

        // JWT 토큰 쿠키 삭제
        response.addCookie(CookieUtil.expire("ACCESS_TOKEN"));
        response.addCookie(CookieUtil.expire("REFRESH_TOKEN"));

        // SecurityContext 클리어
        JwtAuthenticationFilter.clearSecurityContext();

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "로그아웃이 완료되었습니다",
                "instruction", "클라이언트에서 저장된 토큰을 삭제해주세요"
        ));
    }

    /**
     * Access Token 갱신 (쿠키 업데이트 포함)
     *
     * <p>
     * 만료된 Access Token을 Refresh Token을 사용하여 갱신합니다.
     * Refresh Token의 유효성을 검증한 후 새로운 Access Token을 발급하며,
     * 응답과 함께 쿠키도 업데이트합니다.
     * </p>
     *
     * @param refreshTokenRequest Refresh Token 정보
     * @param response            HTTP 응답 객체 (쿠키 업데이트용)
     * @return 새로운 Access Token
     */
    @PostMapping("/token/refresh")
    public ResponseEntity<?> refreshToken(@RequestBody Map<String, String> refreshTokenRequest,
                                          HttpServletResponse response) {

        String refreshToken = refreshTokenRequest.get("refreshToken");

        if (refreshToken == null || refreshToken.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Refresh Token이 필요합니다",
                    "errorCode", "REFRESH_TOKEN_REQUIRED"
            ));
        }

        log.info("토큰 갱신 요청");

        try {
            // Refresh Token 검증
            if (!jwtTokenProvider.validateToken(refreshToken)) {
                log.warn("유효하지 않은 Refresh Token으로 갱신 시도");
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "유효하지 않은 Refresh Token입니다",
                        "errorCode", "INVALID_REFRESH_TOKEN"
                ));
            }

            // Refresh Token 타입 확인
            if (!jwtTokenProvider.isRefreshToken(refreshToken)) {
                log.warn("Refresh Token이 아닌 토큰으로 갱신 시도");
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "Refresh Token이 아닙니다",
                        "errorCode", "INVALID_TOKEN_TYPE"
                ));
            }

            // 사용자 정보 추출 및 검증
            Long userId = jwtTokenProvider.getUserIdFromToken(refreshToken);
            User user = userService.findActiveUserById(userId)
                    .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다"));

            // 새로운 Access Token 생성
            String newAccessToken = jwtTokenProvider.createAccessToken(user.getId(), user.getEmail());

            // Access Token 만료 시간 계산
            Date expirationDate = jwtTokenProvider.getExpirationFromToken(newAccessToken);
            LocalDateTime expiresAt = expirationDate.toInstant()
                    .atZone(ZoneId.systemDefault())
                    .toLocalDateTime();

            // 새로운 Access Token 쿠키 설정
            int accessMaxAge = jwtTokenProvider.getAccessMaxAge();
            response.addCookie(CookieUtil.build("ACCESS_TOKEN", newAccessToken, accessMaxAge));

            // 토큰 응답 생성
            TokenResponse tokenResponse = TokenResponse.of(newAccessToken, refreshToken, expiresAt);

            log.info("토큰 갱신 성공 (쿠키+헤더) - 사용자 ID: {}", userId);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "토큰이 갱신되었습니다",
                    "data", tokenResponse
            ));

        } catch (Exception e) {
            log.warn("토큰 갱신 실패: {}", e.getMessage());

            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "토큰 갱신에 실패했습니다",
                    "errorCode", "TOKEN_REFRESH_FAILED"
            ));
        }
    }
}