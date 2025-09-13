package com.tropical.backend.auth.service;

import com.tropical.backend.auth.entity.User;
import com.tropical.backend.auth.entity.UserConsent;
import com.tropical.backend.auth.repository.UserConsentRepository;
import com.tropical.backend.auth.repository.UserRepository;
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
 * 사용자 동의 관리 서비스
 *
 * <p>
 * 사용자의 필수 동의와 선택 동의를 관리하는 비즈니스 로직을 처리합니다.
 * 온보딩 프로세스, AI 개인화 서비스 권한 제어, GDPR 준수 등의
 * 동의 관련 핵심 기능을 제공합니다.
 * </p>
 *
 * <p>주요 기능:</p>
 * <ul>
 *   <li>온보딩 시 필수/선택 동의 처리</li>
 *   <li>마이페이지에서 동의 상태 변경</li>
 *   <li>AI 추천 시스템 접근 권한 제어</li>
 *   <li>동의 현황 조회 및 통계</li>
 *   <li>동의 철회 및 재동의 처리</li>
 *   <li>GDPR 및 개인정보보호법 준수 지원</li>
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
public class UserConsentService {

    private final UserConsentRepository userConsentRepository;
    private final UserRepository userRepository;

    /**
     * 온보딩 시 사용자 동의 정보 일괄 저장
     *
     * <p>
     * 회원가입 후 온보딩 페이지에서 입력받은 필수 동의와 선택 동의를
     * 일괄적으로 처리합니다. 필수 동의가 모두 완료된 경우에만 온보딩이 완료됩니다.
     * </p>
     *
     * @param userId      동의 처리할 사용자 ID
     * @param consentData 동의 타입별 동의 여부 맵
     * @return 온보딩 완료 가능 여부 (필수 동의 모두 완료 시 true)
     * @throws IllegalArgumentException 존재하지 않는 사용자이거나 필수 동의가 누락된 경우
     */
    @Transactional
    public boolean processOnboardingConsents(Long userId, Map<UserConsent.ConsentType, Boolean> consentData) {
        log.info("온보딩 동의 처리 시작 - 사용자 ID: {}, 동의 항목 수: {}", userId, consentData.size());

        // 사용자 존재 확인
        Optional<User> userOpt = userRepository.findByIdAndActive(userId);
        if (userOpt.isEmpty()) {
            log.warn("온보딩 동의 처리 실패 - 사용자 없음: {}", userId);
            throw new IllegalArgumentException("존재하지 않는 사용자입니다: " + userId);
        }

        User user = userOpt.get();

        // 필수 동의 체크
        UserConsent.ConsentType[] requiredConsents = UserConsent.ConsentType.getRequiredConsents();
        for (UserConsent.ConsentType requiredType : requiredConsents) {
            Boolean agreed = consentData.get(requiredType);
            if (agreed == null || !agreed) {
                log.warn("필수 동의 누락 - 사용자 ID: {}, 동의 타입: {}", userId, requiredType);
                throw new IllegalArgumentException("필수 동의가 누락되었습니다: " + requiredType.getDescription());
            }
        }

        // 동의 정보 저장
        for (Map.Entry<UserConsent.ConsentType, Boolean> entry : consentData.entrySet()) {
            UserConsent consent = UserConsent.createUserConsent(user, entry.getKey(), entry.getValue());
            userConsentRepository.save(consent);

            log.debug("동의 정보 저장 - 사용자 ID: {}, 동의 타입: {}, 동의 여부: {}",
                    userId, entry.getKey(), entry.getValue());
        }

        log.info("온보딩 동의 처리 완료 - 사용자 ID: {}", userId);
        return true;
    }

    /**
     * 사용자의 모든 동의 현황 조회
     *
     * <p>
     * 마이페이지에서 현재 동의 상태를 표시하기 위해 사용됩니다.
     * 필수 동의와 선택 동의를 구분하여 반환합니다.
     * </p>
     *
     * @param userId 조회할 사용자 ID
     * @return 동의 타입별 동의 현황 맵
     */
    public Map<UserConsent.ConsentType, Boolean> getUserConsentStatus(Long userId) {
        log.debug("사용자 동의 현황 조회 - 사용자 ID: {}", userId);

        List<UserConsent> consents = userConsentRepository.findByUserId(userId);

        return consents.stream()
                .collect(Collectors.toMap(
                        UserConsent::getConsentType,
                        UserConsent::isAgreed
                ));
    }

    /**
     * 특정 동의 항목의 동의 여부 확인
     *
     * <p>
     * AI 추천 서비스 이용 전 해당 동의가 완료되었는지 확인하는 용도로 사용됩니다.
     * 예: 일기 기반 추천 기능 사용 전 DIARY_PERSONALIZATION 동의 확인
     * </p>
     *
     * @param userId      확인할 사용자 ID
     * @param consentType 확인할 동의 타입
     * @return 해당 동의를 했으면 true, 안했거나 철회했으면 false
     */
    public boolean isConsentAgreed(Long userId, UserConsent.ConsentType consentType) {
        return userConsentRepository.isConsentAgreed(userId, consentType);
    }

    /**
     * 필수 동의 완료 여부 확인
     *
     * <p>
     * 온보딩 완료 조건 검증에 사용됩니다.
     * 서비스 이용약관과 일정 기반 추천 동의가 모두 완료되어야 true를 반환합니다.
     * </p>
     *
     * @param userId 확인할 사용자 ID
     * @return 필수 동의 2개가 모두 완료되었으면 true
     */
    public boolean hasAllRequiredConsents(Long userId) {
        boolean hasRequired = userConsentRepository.hasAllRequiredConsents(userId);
        log.debug("필수 동의 완료 여부 확인 - 사용자 ID: {}, 완료 여부: {}", userId, hasRequired);
        return hasRequired;
    }

    /**
     * 선택 동의 상태 변경
     *
     * <p>
     * 마이페이지에서 사용자가 선택 동의를 변경할 때 사용됩니다.
     * 필수 동의는 변경할 수 없으며, 선택 동의만 on/off 토글이 가능합니다.
     * </p>
     *
     * @param userId      동의 상태를 변경할 사용자 ID
     * @param consentType 변경할 동의 타입 (선택 동의만 허용)
     * @param agreed      새로운 동의 상태
     * @return 동의 상태 변경 성공 여부
     * @throws IllegalArgumentException 필수 동의를 변경하려고 하는 경우
     */
    @Transactional
    public boolean updateOptionalConsent(Long userId, UserConsent.ConsentType consentType, boolean agreed) {
        log.info("선택 동의 상태 변경 시작 - 사용자 ID: {}, 동의 타입: {}, 동의 여부: {}",
                userId, consentType, agreed);

        // 필수 동의 변경 시도 차단
        if (consentType.isRequired()) {
            log.warn("필수 동의 변경 시도 차단 - 사용자 ID: {}, 동의 타입: {}", userId, consentType);
            throw new IllegalArgumentException("필수 동의는 변경할 수 없습니다: " + consentType.getDescription());
        }

        // 사용자 존재 확인
        Optional<User> userOpt = userRepository.findByIdAndActive(userId);
        if (userOpt.isEmpty()) {
            log.warn("선택 동의 변경 실패 - 사용자 없음: {}", userId);
            return false;
        }

        User user = userOpt.get();

        // 기존 동의 정보 조회 또는 생성
        Optional<UserConsent> consentOpt = userConsentRepository.findByUserIdAndConsentType(userId, consentType);

        UserConsent consent;
        if (consentOpt.isPresent()) {
            // 기존 동의 정보 업데이트
            consent = consentOpt.get();
            consent.updateAgreement(agreed);
            log.debug("기존 동의 정보 업데이트 - 사용자 ID: {}, 동의 타입: {}", userId, consentType);
        } else {
            // 새로운 동의 정보 생성
            consent = UserConsent.createUserConsent(user, consentType, agreed);
            log.debug("새로운 동의 정보 생성 - 사용자 ID: {}, 동의 타입: {}", userId, consentType);
        }

        userConsentRepository.save(consent);
        log.info("선택 동의 상태 변경 완료 - 사용자 ID: {}, 동의 타입: {}, 동의 여부: {}",
                userId, consentType, agreed);
        return true;
    }

    /**
     * 사용자의 동의한 선택 동의 목록 조회
     *
     * <p>
     * AI 추천 시스템에서 어떤 개인화 데이터를 활용할 수 있는지 확인하기 위해 사용됩니다.
     * 동의한 항목에 따라 AI 추천의 개인화 수준이 결정됩니다.
     * </p>
     *
     * @param userId 조회할 사용자 ID
     * @return 동의한 선택 동의 타입 목록 (DIARY, TODO, BUCKET 중 동의한 것들)
     */
    public List<UserConsent.ConsentType> getAgreedOptionalConsents(Long userId) {
        List<UserConsent.ConsentType> agreedConsents = userConsentRepository.findAgreedOptionalConsentTypes(userId);
        log.debug("동의한 선택 동의 목록 조회 - 사용자 ID: {}, 동의 항목 수: {}", userId, agreedConsents.size());
        return agreedConsents;
    }

    /**
     * 동의 철회 처리
     *
     * <p>
     * 선택 동의에 대해서만 철회가 가능합니다.
     * 철회 시 해당 데이터를 활용한 AI 개인화 서비스 이용이 제한됩니다.
     * </p>
     *
     * @param userId      철회할 사용자 ID
     * @param consentType 철회할 동의 타입 (선택 동의만 가능)
     * @return 동의 철회 성공 여부
     * @throws IllegalArgumentException 필수 동의를 철회하려고 하는 경우
     */
    @Transactional
    public boolean withdrawConsent(Long userId, UserConsent.ConsentType consentType) {
        log.info("동의 철회 처리 시작 - 사용자 ID: {}, 동의 타입: {}", userId, consentType);

        Optional<UserConsent> consentOpt = userConsentRepository.findByUserIdAndConsentType(userId, consentType);
        if (consentOpt.isEmpty()) {
            log.warn("동의 철회 실패 - 동의 정보 없음: 사용자 ID {}, 동의 타입 {}", userId, consentType);
            return false;
        }

        UserConsent consent = consentOpt.get();
        try {
            consent.withdraw();
            userConsentRepository.save(consent);
            log.info("동의 철회 완료 - 사용자 ID: {}, 동의 타입: {}", userId, consentType);
            return true;
        } catch (IllegalStateException e) {
            log.warn("동의 철회 실패 - {}: 사용자 ID {}, 동의 타입 {}", e.getMessage(), userId, consentType);
            throw new IllegalArgumentException(e.getMessage());
        }
    }

    /**
     * 동의 재동의 처리
     *
     * <p>
     * 이전에 철회했던 선택 동의에 대해 다시 동의할 때 사용됩니다.
     * AI 개인화 서비스 이용을 재개하고 싶을 때 활용할 수 있습니다.
     * </p>
     *
     * @param userId      재동의할 사용자 ID
     * @param consentType 재동의할 동의 타입
     * @return 재동의 성공 여부
     */
    @Transactional
    public boolean reConsent(Long userId, UserConsent.ConsentType consentType) {
        log.info("재동의 처리 시작 - 사용자 ID: {}, 동의 타입: {}", userId, consentType);

        Optional<UserConsent> consentOpt = userConsentRepository.findByUserIdAndConsentType(userId, consentType);
        if (consentOpt.isEmpty()) {
            log.warn("재동의 실패 - 동의 정보 없음: 사용자 ID {}, 동의 타입 {}", userId, consentType);
            return false;
        }

        UserConsent consent = consentOpt.get();
        consent.reConsent();
        userConsentRepository.save(consent);

        log.info("재동의 완료 - 사용자 ID: {}, 동의 타입: {}", userId, consentType);
        return true;
    }

    /**
     * AI 개인화 서비스 이용 가능 여부 확인
     *
     * <p>
     * 특정 AI 개인화 기능 이용 전 해당 동의가 완료되었는지 확인합니다.
     * 동의하지 않은 사용자에게는 동의 요청 모달을 표시해야 합니다.
     * </p>
     *
     * @param userId               확인할 사용자 ID
     * @param requiredConsentTypes AI 서비스 이용에 필요한 동의 타입 목록
     * @return 모든 필요한 동의가 완료되었으면 true
     */
    public boolean canUsePersonalizedService(Long userId, List<UserConsent.ConsentType> requiredConsentTypes) {
        log.debug("AI 개인화 서비스 이용 가능 여부 확인 - 사용자 ID: {}, 필요 동의 수: {}",
                userId, requiredConsentTypes.size());

        for (UserConsent.ConsentType consentType : requiredConsentTypes) {
            if (!isConsentAgreed(userId, consentType)) {
                log.debug("AI 서비스 이용 불가 - 사용자 ID: {}, 미동의 타입: {}", userId, consentType);
                return false;
            }
        }

        log.debug("AI 개인화 서비스 이용 가능 - 사용자 ID: {}", userId);
        return true;
    }

    /**
     * 온보딩 미완료 사용자 목록 조회
     *
     * <p>
     * 필수 동의를 완료하지 않은 사용자들을 조회합니다.
     * 온보딩 독려 알림이나 사용자 현황 분석에 활용할 수 있습니다.
     * </p>
     *
     * @return 필수 동의가 미완료된 사용자 ID 목록
     */
    public List<Long> getIncompleteOnboardingUsers() {
        List<Long> incompleteUsers = userConsentRepository.findUsersWithIncompleteRequiredConsents();
        log.info("온보딩 미완료 사용자 조회 완료 - 미완료 사용자 수: {}", incompleteUsers.size());
        return incompleteUsers;
    }

    /**
     * 동의 타입별 사용자 수 통계 조회
     *
     * <p>
     * 각 동의 항목별로 얼마나 많은 사용자가 동의했는지 통계를 제공합니다.
     * 서비스 대시보드나 기능 사용률 분석에 활용할 수 있습니다.
     * </p>
     *
     * @param consentType 통계를 조회할 동의 타입
     * @return 해당 동의를 한 사용자 수
     */
    public long getUserCountByConsentType(UserConsent.ConsentType consentType) {
        long count = userConsentRepository.countUsersWithConsent(consentType);
        log.debug("동의 타입별 사용자 수 조회 - 동의 타입: {}, 사용자 수: {}", consentType, count);
        return count;
    }

    /**
     * 동의 철회한 사용자 목록 조회
     *
     * <p>
     * 특정 선택 동의를 철회한 사용자들을 조회합니다.
     * 서비스 개선이나 사용자 피드백 수집에 활용할 수 있습니다.
     * </p>
     *
     * @param consentType 조회할 동의 타입
     * @return 해당 동의를 철회한 사용자 ID 목록
     */
    public List<Long> getUsersWhoWithdrewConsent(UserConsent.ConsentType consentType) {
        List<Long> withdrawnUsers = userConsentRepository.findUsersWhoWithdrewConsent(consentType);
        log.info("동의 철회 사용자 조회 완료 - 동의 타입: {}, 철회 사용자 수: {}",
                consentType, withdrawnUsers.size());
        return withdrawnUsers;
    }

    /**
     * 최근 동의 변경 이력 조회
     *
     * <p>
     * 특정 기간 내 동의 상태를 변경한 사용자들을 조회합니다.
     * 개인정보처리방침 변경 등으로 인한 재동의 현황 파악에 사용됩니다.
     * </p>
     *
     * @param afterDate 조회 기준 날짜
     * @return 해당 기간 이후 동의 상태를 변경한 동의 정보 목록
     */
    public List<UserConsent> getRecentConsentChanges(LocalDateTime afterDate) {
        List<UserConsent> recentChanges = userConsentRepository.findConsentsUpdatedAfter(afterDate);
        log.info("최근 동의 변경 이력 조회 완료 - 기준 날짜: {}, 변경 건수: {}",
                afterDate, recentChanges.size());
        return recentChanges;
    }

    /**
     * 사용자별 동의 완성도 조회
     *
     * <p>
     * 각 사용자가 총 몇 개의 동의 항목에 동의했는지 조회합니다.
     * 사용자 참여도나 개인화 서비스 활용도 측정에 사용할 수 있습니다.
     * </p>
     *
     * @param userId 사용자 ID
     * @return 해당 사용자가 동의한 총 항목 수
     */
    public long getUserConsentCompleteness(Long userId) {
        long consentCount = userConsentRepository.countAgreedConsentsByUser(userId);
        log.debug("사용자 동의 완성도 조회 - 사용자 ID: {}, 동의 항목 수: {}", userId, consentCount);
        return consentCount;
    }

    /**
     * 최대 개인화 동의 완료 사용자 조회
     *
     * <p>
     * 모든 선택 동의(일기, 할일, 버킷리스트)에 동의한 사용자들을 조회합니다.
     * 최고 수준의 개인화 서비스를 받을 수 있는 프리미엄 사용자 집단입니다.
     * </p>
     *
     * @return 모든 선택 동의를 완료한 사용자 ID 목록
     */
    public List<Long> getFullPersonalizationUsers() {
        List<Long> fullPersonalizationUsers = userConsentRepository.findUsersWithFullPersonalizationConsent();
        log.info("최대 개인화 동의 사용자 조회 완료 - 사용자 수: {}", fullPersonalizationUsers.size());
        return fullPersonalizationUsers;
    }

    /**
     * 전체 동의 현황 통계 조회
     *
     * <p>
     * 모든 동의 타입별 동의율을 한 번에 조회합니다.
     * 관리자 대시보드나 서비스 현황 보고서 작성에 활용할 수 있습니다.
     * </p>
     *
     * @return 동의 타입별 동의 사용자 수 맵
     */
    public Map<UserConsent.ConsentType, Long> getAllConsentStatistics() {
        List<Object[]> statistics = userConsentRepository.getConsentStatistics();

        Map<UserConsent.ConsentType, Long> result = statistics.stream()
                .collect(Collectors.toMap(
                        row -> (UserConsent.ConsentType) row[0],
                        row -> (Long) row[1]
                ));

        log.info("전체 동의 현황 통계 조회 완료 - 동의 타입 수: {}", result.size());
        return result;
    }

    /**
     * 동의 항목별 철회율 통계 조회
     *
     * <p>
     * 각 동의 항목이 얼마나 자주 철회되는지 분석하기 위해 사용됩니다.
     * 사용자가 부담스러워하는 동의 항목을 파악하여 서비스 개선에 활용할 수 있습니다.
     * </p>
     *
     * @return 동의 타입별 철회율 통계 맵
     */
    public Map<UserConsent.ConsentType, Double> getWithdrawalRateStatistics() {
        List<Object[]> statistics = userConsentRepository.getWithdrawalRateStatistics();

        Map<UserConsent.ConsentType, Double> result = statistics.stream()
                .collect(Collectors.toMap(
                        row -> (UserConsent.ConsentType) row[0],
                        row -> {
                            Long totalCount = (Long) row[1];
                            Long withdrawnCount = (Long) row[2];
                            return totalCount > 0 ? (double) withdrawnCount / totalCount * 100.0 : 0.0;
                        }
                ));

        log.info("동의 철회율 통계 조회 완료 - 동의 타입 수: {}", result.size());
        return result;
    }
}