package com.tropical.backend.auth.controller;

import com.tropical.backend.auth.dto.request.LoginRequest;
import com.tropical.backend.auth.dto.request.OnboardingRequest;
import com.tropical.backend.auth.dto.request.SignupRequest;
import com.tropical.backend.auth.dto.response.UserResponse;
import com.tropical.backend.auth.entity.User;
import com.tropical.backend.auth.service.UserConsentService;
import com.tropical.backend.auth.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 인증 관련 API 컨트롤러
 *
 * <p>
 * 로컬 계정 회원가입, 로그인, 온보딩 등 사용자 인증과 관련된
 * REST API 엔드포인트를 제공합니다.
 * </p>
 *
 * <p>주요 기능:</p>
 * <ul>
 *   <li>로컬 계정 회원가입 및 이메일 인증</li>
 *   <li>로컬 계정 로그인 및 인증</li>
 *   <li>온보딩 프로세스 (필수/선택 동의 처리)</li>
 *   <li>사용자 인증 상태 확인</li>
 * </ul>
 *
 * @author 왕택준
 * @version 0.1
 * @since 2025.09.13
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final UserService userService;
    private final UserConsentService userConsentService;

    /**
     * 로컬 계정 회원가입
     *
     * <p>
     * 이메일과 비밀번호를 사용하는 로컬 계정을 생성합니다.
     * 회원가입 성공 후 이메일 인증이 필요하며, 온보딩 페이지로 리다이렉트됩니다.
     * </p>
     *
     * @param signupRequest 회원가입 요청 정보 (이메일, 비밀번호, 닉네임)
     * @return 생성된 사용자 정보와 온보딩 필요 상태
     */
    @PostMapping("/signup")
    public ResponseEntity<?> signup(@Valid @RequestBody SignupRequest signupRequest) {
        log.info("로컬 계정 회원가입 요청 - 이메일: {}, 닉네임: {}",
                signupRequest.getEmail(), signupRequest.getNickname());

        try {
            // 로컬 사용자 생성
            User user = userService.createLocalUser(
                    signupRequest.getEmail(),
                    signupRequest.getPassword(),
                    signupRequest.getNickname()
            );

            log.info("로컬 계정 회원가입 성공 - 사용자 ID: {}", user.getId());

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "회원가입이 완료되었습니다. 온보딩을 진행해주세요.",
                    "user", UserResponse.from(user),
                    "nextStep", "onboarding"
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
     * 로컬 계정 로그인
     *
     * <p>
     * 이메일과 비밀번호를 검증하여 로컬 계정 로그인을 처리합니다.
     * 로그인 성공 시 사용자 정보와 온보딩 완료 상태를 반환합니다.
     * </p>
     *
     * @param loginRequest 로그인 요청 정보 (이메일, 비밀번호)
     * @return 로그인 사용자 정보 및 인증 상태
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest loginRequest) {
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

            // 이메일 인증 확인
            if (!user.isEmailVerified()) {
                log.warn("이메일 미인증 사용자 로그인 시도 - 이메일: {}", loginRequest.getEmail());
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "이메일 인증이 필요합니다",
                        "errorCode", "EMAIL_NOT_VERIFIED",
                        "nextStep", "email_verification"
                ));
            }

            // 마지막 로그인 시간 업데이트
            userService.updateLastLoginTime(user.getId());

            // 온보딩 완료 여부 확인
            String nextStep = user.isOnboardingCompleted() ? "dashboard" : "onboarding";

            log.info("로컬 계정 로그인 성공 - 사용자 ID: {}, 다음 단계: {}", user.getId(), nextStep);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "로그인이 완료되었습니다",
                    "user", UserResponse.from(user),
                    "nextStep", nextStep
            ));

        } catch (IllegalArgumentException e) {
            log.warn("로컬 계정 로그인 실패 - 이메일: {}, 사유: {}",
                    loginRequest.getEmail(), e.getMessage());

            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage(),
                    "errorCode", "LOGIN_FAILED"
            ));
        }
    }

    /**
     * 온보딩 완료 처리
     *
     * <p>
     * 회원가입 후 필수 동의와 선택 동의를 처리하여 온보딩을 완료합니다.
     * 필수 동의가 모두 완료되어야 온보딩이 성공적으로 처리됩니다.
     * </p>
     *
     * @param userId            온보딩 완료할 사용자 ID (임시로 RequestParam 사용)
     * @param onboardingRequest 동의 정보 (필수 동의, 선택 동의)
     * @return 온보딩 완료 결과
     */
    @PostMapping("/onboarding")
    public ResponseEntity<?> completeOnboarding(
            @RequestParam Long userId,
            @Valid @RequestBody OnboardingRequest onboardingRequest) {

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
     * 사용자 인증 상태 확인
     *
     * <p>
     * 현재 사용자의 인증 상태와 온보딩 완료 여부를 확인합니다.
     * JWT 토큰 구현 전까지는 사용자 ID로 임시 구현합니다.
     * </p>
     *
     * @param userId 확인할 사용자 ID (임시로 RequestParam 사용)
     * @return 사용자 인증 상태 정보
     */
    @GetMapping("/status")
    public ResponseEntity<?> getAuthStatus(@RequestParam Long userId) {
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
                    "authenticated", true
            ));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage(),
                    "authenticated", false
            ));
        }
    }
}