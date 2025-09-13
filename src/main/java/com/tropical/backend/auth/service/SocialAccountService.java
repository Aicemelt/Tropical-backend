package com.tropical.backend.auth.service;

import com.tropical.backend.auth.entity.SocialAccount;
import com.tropical.backend.auth.entity.User;
import com.tropical.backend.auth.repository.SocialAccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 소셜 계정 연동 관리 서비스
 *
 * <p>
 * 카카오, 구글, 네이버 등의 소셜 로그인 계정 정보를 관리하는 비즈니스 로직을 처리합니다.
 * OAuth2 인증 프로세스와 연동하여 소셜 계정 생성, 조회, 연결/해제 등의
 * 핵심 기능을 제공합니다.
 * </p>
 *
 * <p>주요 기능:</p>
 * <ul>
 *   <li>OAuth2 콜백 시 소셜 계정 생성 및 연동</li>
 *   <li>기존 소셜 계정 조회 및 중복 연결 방지</li>
 *   <li>사용자별 다중 소셜 계정 관리</li>
 *   <li>소셜 계정 연동 해제 및 정리</li>
 *   <li>소셜 로그인 통계 및 분석 데이터 제공</li>
 *   <li>탈퇴 회원의 소셜 계정 정보 보안 처리</li>
 * </ul>
 *
 * @author 왕택준
 * @version 0.1
 * @since 2025.09.13
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class SocialAccountService {

    private final SocialAccountRepository socialAccountRepository;

    /**
     * 소셜 계정 생성 및 연동
     *
     * <p>
     * OAuth2 인증 성공 후 사용자 계정과 소셜 제공자 정보를 연결합니다.
     * 이미 연동된 소셜 계정인 경우 중복 생성하지 않고 기존 정보를 반환합니다.
     * </p>
     *
     * @param user           연결할 사용자 엔터티
     * @param provider       소셜 제공자 (KAKAO, GOOGLE, NAVER)
     * @param providerUserId 소셜 제공자의 사용자 고유 ID
     * @return 생성되거나 기존에 존재하는 소셜 계정 엔터티
     * @throws IllegalArgumentException 이미 다른 사용자에게 연결된 소셜 계정인 경우
     */
    @Transactional
    public SocialAccount createOrGetSocialAccount(User user, SocialAccount.SocialProvider provider, String providerUserId) {
        log.info("소셜 계정 생성/조회 시작 - 사용자 ID: {}, 제공자: {}, 제공자 사용자 ID: {}",
                user.getId(), provider, providerUserId);

        // 기존 소셜 계정 조회
        Optional<SocialAccount> existingSocialAccount = socialAccountRepository
                .findByProviderAndProviderUserIdWithActiveUser(provider, providerUserId);

        if (existingSocialAccount.isPresent()) {
            SocialAccount socialAccount = existingSocialAccount.get();

            // 동일한 사용자의 기존 연동인 경우
            if (socialAccount.getUser().getId().equals(user.getId())) {
                log.info("기존 소셜 계정 반환 - 소셜 계정 ID: {}, 사용자 ID: {}",
                        socialAccount.getId(), user.getId());
                return socialAccount;
            } else {
                // 다른 사용자에게 이미 연결된 소셜 계정
                log.warn("소셜 계정 중복 연결 시도 - 제공자: {}, 제공자 사용자 ID: {}, 기존 사용자 ID: {}, 시도 사용자 ID: {}",
                        provider, providerUserId, socialAccount.getUser().getId(), user.getId());
                throw new IllegalArgumentException("이미 다른 계정에 연결된 소셜 계정입니다: " + provider.name());
            }
        }

        // 새로운 소셜 계정 생성
        SocialAccount newSocialAccount = SocialAccount.createSocialAccount(user, provider, providerUserId);
        SocialAccount savedSocialAccount = socialAccountRepository.save(newSocialAccount);

        log.info("새 소셜 계정 생성 완료 - 소셜 계정 ID: {}, 사용자 ID: {}, 제공자: {}",
                savedSocialAccount.getId(), user.getId(), provider);
        return savedSocialAccount;
    }

    /**
     * 제공자와 제공자 사용자 ID로 활성 소셜 계정 조회
     *
     * <p>
     * OAuth2 로그인 시 기존 계정 존재 여부를 확인하는 핵심 메서드입니다.
     * 탈퇴한 사용자의 소셜 계정은 제외하고 조회합니다.
     * </p>
     *
     * @param provider       소셜 제공자
     * @param providerUserId 소셜 제공자의 사용자 ID
     * @return 활성 사용자에 연결된 소셜 계정 Optional
     */
    public Optional<SocialAccount> findActiveSocialAccount(SocialAccount.SocialProvider provider, String providerUserId) {
        log.debug("활성 소셜 계정 조회 - 제공자: {}, 제공자 사용자 ID: {}", provider, providerUserId);

        return socialAccountRepository.findByProviderAndProviderUserIdWithActiveUser(provider, providerUserId);
    }

    /**
     * 사용자 ID로 연동된 모든 소셜 계정 조회
     *
     * <p>
     * 마이페이지에서 사용자가 연동한 소셜 계정 목록을 표시할 때 사용됩니다.
     * 각 소셜 제공자별 연동 상태를 확인할 수 있습니다.
     * </p>
     *
     * @param userId 조회할 사용자 ID
     * @return 해당 사용자의 소셜 계정 목록
     */
    public List<SocialAccount> getUserSocialAccounts(Long userId) {
        log.debug("사용자 소셜 계정 목록 조회 - 사용자 ID: {}", userId);

        List<SocialAccount> socialAccounts = socialAccountRepository.findByUserId(userId);
        log.debug("사용자 소셜 계정 조회 완료 - 사용자 ID: {}, 연동 계정 수: {}", userId, socialAccounts.size());

        return socialAccounts;
    }

    /**
     * 사용자별 소셜 제공자 연동 상태 맵 조회
     *
     * <p>
     * UI에서 각 소셜 제공자별 연동 여부를 쉽게 확인할 수 있도록 맵 형태로 반환합니다.
     * 예: {GOOGLE: true, KAKAO: false, NAVER: true}
     * </p>
     *
     * @param userId 조회할 사용자 ID
     * @return 소셜 제공자별 연동 여부 맵
     */
    public Map<SocialAccount.SocialProvider, Boolean> getUserSocialProviderStatus(Long userId) {
        log.debug("사용자 소셜 제공자 연동 상태 조회 - 사용자 ID: {}", userId);

        List<SocialAccount> socialAccounts = socialAccountRepository.findByUserId(userId);

        Map<SocialAccount.SocialProvider, Boolean> providerStatus = java.util.Arrays.stream(SocialAccount.SocialProvider.values())
                .collect(Collectors.toMap(
                        provider -> provider,
                        provider -> socialAccounts.stream()
                                .anyMatch(account -> account.getProvider().equals(provider))
                ));

        log.debug("소셜 제공자 연동 상태 조회 완료 - 사용자 ID: {}, 연동 상태: {}", userId, providerStatus);
        return providerStatus;
    }

    /**
     * 특정 소셜 제공자 계정 조회
     *
     * <p>
     * 사용자가 특정 소셜 제공자(예: 구글)와 연동한 계정 정보를 조회합니다.
     * 소셜 계정별 상세 정보 확인이나 연동 해제 시 사용됩니다.
     * </p>
     *
     * @param userId   조회할 사용자 ID
     * @param provider 조회할 소셜 제공자
     * @return 해당 제공자와 연동된 소셜 계정 Optional
     */
    public Optional<SocialAccount> getUserSocialAccountByProvider(Long userId, SocialAccount.SocialProvider provider) {
        log.debug("특정 소셜 제공자 계정 조회 - 사용자 ID: {}, 제공자: {}", userId, provider);

        return socialAccountRepository.findByUserIdAndProvider(userId, provider);
    }

    /**
     * 소셜 계정 연동 해제
     *
     * <p>
     * 사용자가 특정 소셜 제공자와의 연동을 해제할 때 사용됩니다.
     * 마이페이지에서 소셜 계정 관리 기능에 활용됩니다.
     * </p>
     *
     * @param userId   연동 해제할 사용자 ID
     * @param provider 연동 해제할 소셜 제공자
     * @return 연동 해제 성공 여부
     */
    @Transactional
    public boolean unlinkSocialAccount(Long userId, SocialAccount.SocialProvider provider) {
        log.info("소셜 계정 연동 해제 시작 - 사용자 ID: {}, 제공자: {}", userId, provider);

        Optional<SocialAccount> socialAccountOpt = socialAccountRepository.findByUserIdAndProvider(userId, provider);
        if (socialAccountOpt.isEmpty()) {
            log.warn("연동 해제 실패 - 연동되지 않은 소셜 계정: 사용자 ID {}, 제공자 {}", userId, provider);
            return false;
        }

        SocialAccount socialAccount = socialAccountOpt.get();
        socialAccount.unlinkFromUser();
        socialAccountRepository.delete(socialAccount);

        log.info("소셜 계정 연동 해제 완료 - 사용자 ID: {}, 제공자: {}", userId, provider);
        return true;
    }

    /**
     * 소셜 계정 존재 여부 확인
     *
     * <p>
     * OAuth2 인증 전 해당 소셜 계정이 이미 시스템에 등록되어 있는지 빠르게 확인합니다.
     * 성능상 count 쿼리보다 exists 쿼리가 더 효율적입니다.
     * </p>
     *
     * @param provider       소셜 제공자
     * @param providerUserId 소셜 제공자의 사용자 ID
     * @return 해당 소셜 계정이 이미 등록되어 있으면 true
     */
    public boolean existsSocialAccount(SocialAccount.SocialProvider provider, String providerUserId) {
        boolean exists = socialAccountRepository.existsByProviderAndProviderUserId(provider, providerUserId);
        log.debug("소셜 계정 존재 여부 확인 - 제공자: {}, 제공자 사용자 ID: {}, 존재 여부: {}",
                provider, providerUserId, exists);
        return exists;
    }

    /**
     * 사용자의 모든 소셜 계정 삭제
     *
     * <p>
     * 회원 탈퇴 시 해당 사용자와 연결된 모든 소셜 계정 정보를 삭제합니다.
     * 개인정보보호 및 GDPR 준수를 위해 사용됩니다.
     * </p>
     *
     * @param userId 삭제할 사용자 ID
     * @return 삭제된 소셜 계정 수
     */
    @Transactional
    public int deleteAllUserSocialAccounts(Long userId) {
        log.info("사용자 모든 소셜 계정 삭제 시작 - 사용자 ID: {}", userId);

        List<SocialAccount> socialAccounts = socialAccountRepository.findByUserId(userId);
        int deletedCount = socialAccounts.size();

        if (deletedCount > 0) {
            socialAccountRepository.deleteByUserId(userId);
            log.info("사용자 모든 소셜 계정 삭제 완료 - 사용자 ID: {}, 삭제된 계정 수: {}", userId, deletedCount);
        } else {
            log.debug("삭제할 소셜 계정 없음 - 사용자 ID: {}", userId);
        }

        return deletedCount;
    }

    /**
     * 복수 소셜 계정 연동 사용자 조회
     *
     * <p>
     * 2개 이상의 소셜 계정을 연동한 사용자들을 조회합니다.
     * 사용자 행동 분석이나 UX 개선을 위한 데이터로 활용할 수 있습니다.
     * </p>
     *
     * @return 복수 소셜 계정을 연동한 사용자 ID 목록
     */
    public List<Long> getUsersWithMultipleSocialAccounts() {
        List<Long> multipleAccountUsers = socialAccountRepository.findUserIdsWithMultipleSocialAccounts();
        log.info("복수 소셜 계정 사용자 조회 완료 - 사용자 수: {}", multipleAccountUsers.size());
        return multipleAccountUsers;
    }

    /**
     * 소셜 제공자별 활성 계정 수 조회
     *
     * <p>
     * 각 소셜 제공자별로 활성 사용자에게 연동된 계정 수를 조회합니다.
     * 서비스 통계나 소셜 로그인 사용 현황 분석에 활용할 수 있습니다.
     * </p>
     *
     * @param provider 조회할 소셜 제공자
     * @return 해당 제공자로 연동된 활성 계정 수
     */
    public long getActiveAccountCountByProvider(SocialAccount.SocialProvider provider) {
        long count = socialAccountRepository.countActiveAccountsByProvider(provider);
        log.debug("소셜 제공자별 활성 계정 수 조회 - 제공자: {}, 계정 수: {}", provider, count);
        return count;
    }

    /**
     * 모든 소셜 제공자별 활성 계정 수 통계 조회
     *
     * <p>
     * 카카오, 구글, 네이버 각각의 활성 계정 수를 한 번에 조회합니다.
     * 관리자 대시보드나 서비스 현황 보고서에 활용할 수 있습니다.
     * </p>
     *
     * @return 소셜 제공자별 활성 계정 수 맵
     */
    public Map<SocialAccount.SocialProvider, Long> getAllProviderAccountStatistics() {
        Map<SocialAccount.SocialProvider, Long> statistics = java.util.Arrays.stream(SocialAccount.SocialProvider.values())
                .collect(Collectors.toMap(
                        provider -> provider,
                        this::getActiveAccountCountByProvider
                ));

        log.info("모든 소셜 제공자 계정 통계 조회 완료 - 통계: {}", statistics);
        return statistics;
    }

    /**
     * 최근 연동된 소셜 계정 조회
     *
     * <p>
     * 특정 기간 이후 연동된 소셜 계정들을 조회합니다.
     * 최근 소셜 로그인 가입 동향을 파악하기 위해 사용됩니다.
     * </p>
     *
     * @param afterDate 조회 기준 날짜
     * @return 해당 날짜 이후 연동된 소셜 계정 목록
     */
    public List<SocialAccount> getRecentLinkedAccounts(LocalDateTime afterDate) {
        List<SocialAccount> recentAccounts = socialAccountRepository.findAccountsLinkedAfter(afterDate);
        log.info("최근 연동 소셜 계정 조회 완료 - 기준 날짜: {}, 연동 계정 수: {}",
                afterDate, recentAccounts.size());
        return recentAccounts;
    }

    /**
     * 고아 소셜 계정 조회 및 정리
     *
     * <p>
     * 삭제된 사용자와 연결된 소셜 계정 정보를 조회합니다.
     * 데이터 정리나 무결성 검증을 위한 배치 작업에서 활용할 수 있습니다.
     * </p>
     *
     * @return 삭제된 사용자와 연결된 소셜 계정 목록
     */
    public List<SocialAccount> getOrphanedSocialAccounts() {
        List<SocialAccount> orphanedAccounts = socialAccountRepository.findOrphanedAccounts();
        log.info("고아 소셜 계정 조회 완료 - 고아 계정 수: {}", orphanedAccounts.size());

        if (orphanedAccounts.size() > 0) {
            log.warn("고아 소셜 계정 발견 - 데이터 정리가 필요할 수 있습니다. 계정 수: {}", orphanedAccounts.size());
        }

        return orphanedAccounts;
    }

    /**
     * 고아 소셜 계정 일괄 삭제
     *
     * <p>
     * 삭제된 사용자와 연결된 불필요한 소셜 계정 정보를 일괄 삭제합니다.
     * 배치 작업이나 데이터 정리 작업에서 사용됩니다.
     * </p>
     *
     * @return 삭제된 고아 소셜 계정 수
     */
    @Transactional
    public int cleanupOrphanedSocialAccounts() {
        log.info("고아 소셜 계정 정리 작업 시작");

        List<SocialAccount> orphanedAccounts = socialAccountRepository.findOrphanedAccounts();
        int deletedCount = orphanedAccounts.size();

        if (deletedCount > 0) {
            for (SocialAccount account : orphanedAccounts) {
                socialAccountRepository.delete(account);
            }
            log.info("고아 소셜 계정 정리 완료 - 삭제된 계정 수: {}", deletedCount);
        } else {
            log.info("정리할 고아 소셜 계정 없음");
        }

        return deletedCount;
    }

    /**
     * 소셜 계정 연동 이력 조회
     *
     * <p>
     * 특정 사용자의 소셜 계정 연동 이력을 조회합니다.
     * 사용자 지원이나 계정 문제 해결 시 활용할 수 있습니다.
     * </p>
     *
     * @param userId 조회할 사용자 ID
     * @return 해당 사용자의 소셜 계정 연동 이력 (연동 시간순 정렬)
     */
    public List<SocialAccount> getUserSocialAccountHistory(Long userId) {
        log.debug("사용자 소셜 계정 연동 이력 조회 - 사용자 ID: {}", userId);

        List<SocialAccount> history = socialAccountRepository.findByUserId(userId);

        // 연동 시간순으로 정렬 (최신순)
        history.sort((a, b) -> b.getLinkedAt().compareTo(a.getLinkedAt()));

        log.debug("소셜 계정 연동 이력 조회 완료 - 사용자 ID: {}, 이력 수: {}", userId, history.size());
        return history;
    }

    /**
     * 소셜 제공자별 신규 가입자 동향 분석
     *
     * <p>
     * 특정 기간 동안 각 소셜 제공자를 통해 가입한 신규 사용자 수를 분석합니다.
     * 마케팅 분석이나 성장 지표 측정에 활용할 수 있습니다.
     * </p>
     *
     * @param startDate 분석 시작 날짜
     * @param endDate   분석 종료 날짜
     * @return 기간 내 소셜 제공자별 신규 연동 계정 수 맵
     */
    public Map<SocialAccount.SocialProvider, Long> getNewAccountTrendsByProvider(LocalDateTime startDate, LocalDateTime endDate) {
        log.info("소셜 제공자별 신규 가입 동향 분석 - 기간: {} ~ {}", startDate, endDate);

        List<SocialAccount> accountsInPeriod = socialAccountRepository.findAccountsLinkedAfter(startDate)
                .stream()
                .filter(account -> account.getLinkedAt().isBefore(endDate))
                .collect(Collectors.toList());

        Map<SocialAccount.SocialProvider, Long> trends = accountsInPeriod.stream()
                .collect(Collectors.groupingBy(
                        SocialAccount::getProvider,
                        Collectors.counting()
                ));

        // 연동이 없는 제공자도 0으로 포함
        for (SocialAccount.SocialProvider provider : SocialAccount.SocialProvider.values()) {
            trends.putIfAbsent(provider, 0L);
        }

        log.info("신규 가입 동향 분석 완료 - 총 신규 연동: {}, 제공자별 현황: {}",
                accountsInPeriod.size(), trends);
        return trends;
    }

    /**
     * registrationId로 소셜 제공자 변환
     *
     * <p>
     * Spring Security OAuth2에서 사용하는 registrationId를
     * 시스템의 SocialProvider enum으로 변환합니다.
     * OAuth2 콜백 처리 시 사용됩니다.
     * </p>
     *
     * @param registrationId Spring Security OAuth2의 registrationId
     * @return 매칭되는 SocialProvider
     * @throws IllegalArgumentException 지원하지 않는 registrationId인 경우
     */
    public SocialAccount.SocialProvider getSocialProviderFromRegistrationId(String registrationId) {
        try {
            SocialAccount.SocialProvider provider = SocialAccount.SocialProvider.fromRegistrationId(registrationId);
            log.debug("registrationId 변환 성공 - registrationId: {}, provider: {}", registrationId, provider);
            return provider;
        } catch (IllegalArgumentException e) {
            log.warn("지원하지 않는 registrationId - registrationId: {}", registrationId);
            throw e;
        }
    }
}