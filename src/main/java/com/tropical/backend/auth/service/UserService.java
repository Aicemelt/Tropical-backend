package com.tropical.backend.auth.service;

import com.tropical.backend.auth.entity.User;
import com.tropical.backend.auth.repository.SocialAccountRepository;
import com.tropical.backend.auth.repository.UserConsentRepository;
import com.tropical.backend.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 사용자 관리 서비스
 *
 * <p>
 * 로컬 계정과 소셜 계정을 포함한 사용자 생성, 인증, 프로필 관리 등의
 * 핵심 비즈니스 로직을 처리하는 서비스 계층입니다.
 * 사용자의 전체 라이프사이클을 관리하며, 보안과 데이터 무결성을 보장합니다.
 * </p>
 *
 * <p>주요 기능:</p>
 * <ul>
 *   <li>로컬 계정 생성 및 이메일 인증</li>
 *   <li>소셜 계정 생성 및 닉네임 중복 처리</li>
 *   <li>사용자 프로필 조회 및 수정</li>
 *   <li>온보딩 상태 관리</li>
 *   <li>계정 논리적 삭제 및 복구</li>
 *   <li>사용자 활동 추적 (로그인 시간 등)</li>
 * </ul>
 *
 * @author 왕택준
 * @version 0.2
 * @since 2025.09.13
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final UserConsentRepository userConsentRepository;
    private final SocialAccountRepository socialAccountRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * 로컬 계정 사용자 생성
     *
     * <p>
     * 이메일과 비밀번호를 사용하는 로컬 계정을 생성합니다.
     * 비밀번호는 자동으로 해시화되며, 이메일 인증이 필요한 상태로 생성됩니다.
     * 닉네임 중복은 허용됩니다.
     * </p>
     *
     * @param email    사용자 이메일 (로그인 ID)
     * @param password 평문 비밀번호 (자동 해시화됨)
     * @param nickname 사용자 닉네임
     * @return 생성된 사용자 엔터티
     * @throws IllegalArgumentException 이메일이 이미 존재하는 경우
     */
    @Transactional
    public User createLocalUser(String email, String password, String nickname) {
        log.info("로컬 계정 생성 시작 - 이메일: {}, 닉네임: {}", email, nickname);

        // 이메일 중복 체크 (활성 계정만)
        // if (userRepository.existsByEmailAndActive(email)) {
        //     log.warn("이메일 중복 - 이미 존재하는 이메일: {}", email);
        //     throw new IllegalArgumentException("이미 사용 중인 이메일입니다: " + email);
        // }

        // 이메일 중복 체크 (활성화 된 로컬 계정만)
        if (userRepository.existsByEmailAndActiveAndLocal(email)) {
            log.warn("로컬 계정 이메일 중복 - 이미 존재하는 로컬 이메일: {}", email);
            throw new IllegalArgumentException("이미 사용 중인 로컬 계정 이메일입니다: " + email);
        }

        // 닉네임 중복 체크 제거 (중복 허용)

        // 비밀번호 해시화
        String hashedPassword = passwordEncoder.encode(password);

        // 사용자 생성
        User user = User.createLocalUser(email, hashedPassword, nickname);
        User savedUser = userRepository.save(user);

        log.info("로컬 계정 생성 완료 - 사용자 ID: {}, 이메일: {}", savedUser.getId(), savedUser.getEmail());
        return savedUser;
    }

    /**
     * 소셜 계정 사용자 생성
     *
     * <p>
     * OAuth2를 통한 소셜 로그인으로 가입하는 사용자를 생성합니다.
     * 닉네임 중복이 허용되므로 suffix 추가 로직을 제거했습니다.
     * </p>
     *
     * @param email    소셜 플랫폼에서 제공받은 이메일
     * @param nickname 소셜 플랫폼에서 제공받은 닉네임
     * @return 생성된 사용자 엔터티
     */
    @Transactional
    public User createSocialUser(String email, String nickname) {
        log.info("소셜 계정 생성 시작 - 이메일: {}, 닉네임: {}", email, nickname);

        // 닉네임 중복 처리 로직 제거 (중복 허용)

        // 소셜 사용자 생성 (이메일 인증 완료 상태)
        User user = User.createSocialUser(email, nickname);
        User savedUser = userRepository.save(user);

        log.info("소셜 계정 생성 완료 - 사용자 ID: {}, 이메일: {}, 닉네임: {}",
                savedUser.getId(), savedUser.getEmail(), savedUser.getNickname());
        return savedUser;
    }

    /**
     * 중복되지 않는 고유한 닉네임 생성
     *
     * <p>
     * 소셜 로그인 시 닉네임 중복이 발생할 때 자동으로 숫자 suffix를 추가합니다.
     * 예: "홍길동" → "홍길동1" → "홍길동2"
     * </p>
     *
     * @param baseNickname 기본 닉네임
     * @return 중복되지 않는 고유한 닉네임
     */
    private String generateUniqueNickname(String baseNickname) {
        String uniqueNickname = baseNickname;
        int suffix = 1;

        while (userRepository.existsByNickname(uniqueNickname)) {
            uniqueNickname = baseNickname + suffix;
            suffix++;

            // 무한루프 방지 (1000번 시도 후 중단)
            if (suffix > 1000) {
                uniqueNickname = baseNickname + "_" + System.currentTimeMillis();
                break;
            }
        }

        return uniqueNickname;
    }

    /**
     * 이메일로 활성 사용자 조회
     *
     * <p>
     * 로그인 시 사용자 인증에 사용됩니다.
     * 논리적으로 삭제된 사용자(DELETED 상태)는 제외하고 조회합니다.
     * </p>
     *
     * @param email 사용자 이메일
     * @return 활성 상태의 사용자 Optional
     */
    public Optional<User> findActiveUserByEmail(String email) {
        return userRepository.findByEmailAndActive(email);
    }

    /**
     * 사용자 ID로 활성 사용자 조회
     *
     * @param userId 사용자 ID
     * @return 활성 상태의 사용자 Optional
     */
    public Optional<User> findActiveUserById(Long userId) {
        return userRepository.findByIdAndActive(userId);
    }

    /**
     * JWT 토큰 검증을 위한 사용자 조회
     *
     * <p>
     * JWT 토큰에 포함된 사용자 ID와 이메일이 모두 일치하는지 확인하여
     * 토큰의 유효성을 검증합니다.
     * </p>
     *
     * @param userId 사용자 ID (JWT subject)
     * @param email  사용자 이메일 (JWT claim)
     * @return ID와 이메일이 모두 일치하는 활성 사용자 Optional
     */
    public Optional<User> findUserForTokenValidation(Long userId, String email) {
        return userRepository.findByIdAndEmailAndActive(userId, email);
    }

    /**
     * 사용자 ID로 완전한 User 엔티티 조회
     *
     * <p>
     * OAuth2 소셜 로그인 시 LazyInitializationException 방지를 위해 사용됩니다.
     * Hibernate 프록시가 아닌 실제 엔티티를 반환하여 트랜잭션 밖에서도
     * 안전하게 필드에 접근할 수 있습니다. 조회 시 모든 필드를 강제 초기화하여
     * 'no session' 에러를 방지합니다.
     * </p>
     *
     * @param userId 조회할 사용자 ID
     * @return 완전히 초기화된 User 엔티티
     * @throws IllegalArgumentException 사용자를 찾을 수 없는 경우
     */
    @Transactional(readOnly = true)
    public User getById(Long userId) {
        log.debug("User 엔티티 조회 시작 - 사용자 ID: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + userId));

        // Hibernate 프록시 강제 초기화
        // OAuth2 핸들러에서 isOnboardingCompleted() 등의 필드 접근 시 안전성 보장
        boolean initialized = user.isOnboardingCompleted();

        log.debug("User 엔티티 초기화 완료 - 사용자 ID: {}, 온보딩 완료: {}", userId, initialized);
        return user;
    }

    /**
     * 이메일 인증 완료 처리
     *
     * <p>
     * 사용자의 이메일 인증을 완료하고 인증 시간을 기록합니다.
     * 보안을 위해 사용자 ID와 이메일이 모두 일치하는지 확인합니다.
     * </p>
     *
     * @param userId 사용자 ID
     * @param email  인증할 이메일 주소
     * @throws IllegalArgumentException 사용자를 찾을 수 없거나 이메일이 일치하지 않는 경우
     */
    @Transactional
    public void markEmailVerified(Long userId, String email) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        if (!email.equals(user.getEmail())) {
            throw new IllegalArgumentException("이메일이 일치하지 않습니다.");
        }

        user.setEmailVerified(true);
        user.setEmailVerifiedAt(LocalDateTime.now());
        userRepository.save(user);

        log.info("이메일 인증 완료 처리 - 사용자 ID: {}, 이메일: {}", userId, email);
    }

    /**
     * 이메일 인증 처리
     *
     * <p>
     * 로컬 계정의 이메일 인증을 완료 처리합니다.
     * 이미 인증된 계정에 대한 중복 요청도 적절히 처리합니다.
     * </p>
     *
     * @param email 인증할 이메일
     * @return 인증 처리 성공 여부
     */
    @Transactional
    public boolean verifyUserEmail(String email) {
        log.info("이메일 인증 처리 시작 - 이메일: {}", email);

        // 이미 인증된 사용자 체크
        if (userRepository.existsByEmailAndVerified(email)) {
            log.info("이미 인증 완료된 이메일 - 중복 요청 처리: {}", email);
            return true; // 이미 인증되었으므로 성공으로 처리
        }

        // 인증 대기 중인 사용자 처리
        Optional<User> userOpt = userRepository.findByEmailAndUnverified(email);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            user.verifyEmail();
            userRepository.save(user);

            log.info("이메일 인증 완료 - 사용자 ID: {}, 이메일: {}", user.getId(), email);
            return true;
        }

        log.warn("이메일 인증 실패 - 존재하지 않는 이메일: {}", email);
        return false;
    }

    /**
     * 온보딩 완료 처리
     *
     * <p>
     * 필수 동의를 모두 완료한 사용자의 온보딩 상태를 완료로 변경합니다.
     * 이후 홈 화면에 정상 접근이 가능해집니다.
     * </p>
     *
     * @param userId 온보딩 완료 처리할 사용자 ID
     * @return 온보딩 완료 처리 성공 여부
     */
    @Transactional
    public boolean completeOnboarding(Long userId) {
        log.info("온보딩 완료 처리 시작 - 사용자 ID: {}", userId);

        Optional<User> userOpt = userRepository.findByIdAndActive(userId);
        if (userOpt.isPresent()) {
            User user = userOpt.get();

            // 필수 동의 완료 여부 확인
            if (!userConsentRepository.hasAllRequiredConsents(userId)) {
                log.warn("필수 동의 미완료로 온보딩 완료 불가 - 사용자 ID: {}", userId);
                return false;
            }

            user.completeOnboarding();
            userRepository.save(user);

            log.info("온보딩 완료 - 사용자 ID: {}", userId);
            return true;
        }

        log.warn("온보딩 완료 실패 - 사용자 없음: {}", userId);
        return false;
    }

    /**
     * 마지막 로그인 시간 업데이트
     *
     * <p>
     * 로그인 성공 시 사용자의 마지막 로그인 시간을 현재 시간으로 업데이트합니다.
     * 사용자 활동 추적 및 휴면 계정 관리에 활용됩니다.
     * </p>
     *
     * @param userId 로그인한 사용자 ID
     */
    @Transactional
    public void updateLastLoginTime(Long userId) {
        Optional<User> userOpt = userRepository.findByIdAndActive(userId);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            user.updateLastLogin();
            userRepository.save(user);

            log.debug("마지막 로그인 시간 업데이트 - 사용자 ID: {}, 시간: {}", userId, user.getLastLoginAt());
        }
    }

    /**
     * 사용자 프로필 수정
     *
     * <p>
     * 닉네임, 캘린더 설정, 알림 설정 등 사용자가 변경 가능한 프로필 정보를 수정합니다.
     * 닉네임 중복은 허용됩니다.
     * </p>
     *
     * @param userId      수정할 사용자 ID
     * @param newNickname 새로운 닉네임 (null인 경우 변경하지 않음)
     * @return 프로필 수정 성공 여부
     */
    @Transactional
    public boolean updateUserProfile(Long userId, String newNickname) {
        log.info("사용자 프로필 수정 시작 - 사용자 ID: {}", userId);

        Optional<User> userOpt = userRepository.findByIdAndActive(userId);
        if (userOpt.isEmpty()) {
            log.warn("프로필 수정 실패 - 사용자 없음: {}", userId);
            return false;
        }

        User user = userOpt.get();

        // 닉네임 변경 (중복 체크 제거)
        if (newNickname != null && !newNickname.equals(user.getNickname())) {
            user.changeNickname(newNickname);
            log.info("닉네임 변경 - 사용자 ID: {}, 기존: {}, 신규: {}",
                    userId, user.getNickname(), newNickname);
        }

        userRepository.save(user);
        log.info("사용자 프로필 수정 완료 - 사용자 ID: {}", userId);
        return true;
    }

    /**
     * 계정 논리적 삭제 (회원 탈퇴)
     *
     * <p>
     * 물리적 삭제 대신 논리적 삭제를 수행합니다.
     * 사용자 상태를 DELETED로 변경하고 개인정보를 마스킹 처리합니다.
     * 관련된 소셜 계정과 동의 정보도 함께 삭제됩니다.
     * </p>
     *
     * @param userId 탈퇴 처리할 사용자 ID
     * @return 탈퇴 처리 성공 여부
     */
    @Transactional
    public boolean deleteUser(Long userId) {
        log.info("회원 탈퇴 처리 시작 - 사용자 ID: {}", userId);

        Optional<User> userOpt = userRepository.findByIdAndActive(userId);
        if (userOpt.isEmpty()) {
            log.warn("회원 탈퇴 실패 - 사용자 없음: {}", userId);
            return false;
        }

        User user = userOpt.get();
        String originalEmail = user.getEmail();
        String originalNickname = user.getNickname();

        // 논리적 삭제 및 개인정보 마스킹
        user.softDelete();
        userRepository.save(user);

        // 연관된 소셜 계정 정보 삭제
        socialAccountRepository.deleteByUserId(userId);

        // 연관된 동의 정보 삭제
        userConsentRepository.deleteByUserId(userId);

        log.info("회원 탈퇴 완료 - 사용자 ID: {}, 원본 이메일: {}, 원본 닉네임: {}",
                userId, originalEmail, originalNickname);
        return true;
    }

    /**
     * 온보딩 미완료 사용자 확인
     *
     * <p>
     * 해당 사용자가 온보딩을 완료했는지 확인합니다.
     * 미완료 사용자는 홈 화면 대신 온보딩 페이지로 리다이렉트되어야 합니다.
     * </p>
     *
     * @param userId 확인할 사용자 ID
     * @return 온보딩 미완료 사용자인 경우 true
     */
    public boolean isOnboardingIncomplete(Long userId) {
        return userRepository.findByIdAndOnboardingIncomplete(userId).isPresent();
    }

    /**
     * 비밀번호 검증
     *
     * <p>
     * 로컬 계정 로그인 시 입력된 평문 비밀번호와 저장된 해시를 비교합니다.
     * </p>
     *
     * @param user          검증할 사용자 엔터티
     * @param plainPassword 평문 비밀번호
     * @return 비밀번호 일치 여부
     */
    public boolean verifyPassword(User user, String plainPassword) {
        if (user.getPasswordHash() == null) {
            log.warn("비밀번호 검증 실패 - 소셜 계정 (비밀번호 없음): 사용자 ID {}", user.getId());
            return false;
        }

        return passwordEncoder.matches(plainPassword, user.getPasswordHash());
    }

    /**
     * 계정 타입별 활성 사용자 수 조회
     *
     * <p>
     * 서비스 통계 및 대시보드용 데이터를 제공합니다.
     * 로컬 계정과 소셜 계정의 비율을 파악할 수 있습니다.
     * </p>
     *
     * @param accountType 조회할 계정 타입
     * @return 해당 계정 타입의 활성 사용자 수
     */
    public long getActiveUserCountByType(User.AccountType accountType) {
        return userRepository.countActiveUsersByAccountType(accountType);
    }

    /**
     * 최근 활동 사용자 목록 조회
     *
     * <p>
     * 특정 기간 이후 로그인한 활성 사용자 목록을 조회합니다.
     * 사용자 활동 분석이나 알림 발송 대상 선별에 활용할 수 있습니다.
     * </p>
     *
     * @param afterDate 기준 날짜
     * @return 해당 기간 이후 로그인한 사용자 목록
     */
    public List<User> getRecentActiveUsers(LocalDateTime afterDate) {
        return userRepository.findActiveUsersLoggedInAfter(afterDate);
    }

    /**
     * 닉네임 패턴별 사용자 수 조회
     *
     * <p>
     * 특정 닉네임 패턴과 일치하는 사용자 수를 조회합니다.
     * 닉네임 자동 생성 시 기존 사용량 파악에 활용됩니다.
     * </p>
     *
     * @param nicknamePattern 조회할 닉네임 패턴 (LIKE 연산자 사용)
     * @return 해당 패턴과 일치하는 사용자 수
     */
    public long getUserCountByNicknamePattern(String nicknamePattern) {
        return userRepository.countByNicknamePattern(nicknamePattern);
    }
}