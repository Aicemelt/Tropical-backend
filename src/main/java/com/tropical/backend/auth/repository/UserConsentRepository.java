package com.tropical.backend.auth.repository;

import com.tropical.backend.auth.entity.UserConsent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 사용자 동의 정보 데이터 접근 Repository
 *
 * <p>
 * 사용자의 필수 동의와 선택 동의 정보를 관리하는 데이터 접근 계층입니다.
 * 온보딩 프로세스, AI 개인화 서비스, GDPR 준수 등을 위한 동의 관리 기능을 제공하며,
 * 약관 버전 추적을 통한 법적 증빙도 지원합니다.
 * </p>
 *
 * <p>주요 기능:</p>
 * <ul>
 *   <li>온보딩 완료 조건 검증 (필수 동의 확인)</li>
 *   <li>AI 추천 시스템 연동 (선택 동의 기반 개인화)</li>
 *   <li>동의 철회 및 변경 이력 관리</li>
 *   <li>약관 버전별 동의 추적 및 재동의 관리</li>
 *   <li>개인정보보호법 및 GDPR 준수 지원</li>
 *   <li>동의 현황 통계 및 분석 데이터 제공</li>
 * </ul>
 *
 * @author 왕택준
 * @version 0.2
 * @since 2025.09.18
 */
@Repository
public interface UserConsentRepository extends JpaRepository<UserConsent, Long> {

    /**
     * 사용자 ID로 활성 사용자의 동의 정보 조회
     *
     * <p>
     * 마이페이지에서 사용자의 전체 동의 현황을 표시할 때 사용됩니다.
     * 활성 상태(ACTIVE) 사용자의 동의 정보만 반환하며, 탈퇴한 사용자는 제외됩니다.
     * 법적 증빙용 조회는 별도 메서드를 사용해야 합니다.
     * </p>
     *
     * @param userId 사용자 ID
     * @return 해당 활성 사용자의 모든 동의 정보 목록, 탈퇴 사용자면 빈 리스트
     */
    @Query("SELECT uc FROM UserConsent uc WHERE uc.user.id = :userId AND uc.user.status = 'ACTIVE'")
    List<UserConsent> findByUserId(@Param("userId") Long userId);

    /**
     * 사용자 ID와 동의 타입으로 활성 사용자의 특정 동의 정보 조회
     *
     * <p>
     * 특정 동의 항목의 상태를 확인하거나 업데이트할 때 사용됩니다.
     * 활성 사용자의 동의만 대상으로 하며, 탈퇴한 사용자는 제외됩니다.
     * </p>
     *
     * @param userId      사용자 ID
     * @param consentType 동의 타입 (TERMS_OF_SERVICE, CALENDAR_PERSONALIZATION 등)
     * @return 해당 활성 사용자의 동의 정보 Optional, 존재하지 않는 경우 empty
     */
    @Query("SELECT uc FROM UserConsent uc WHERE uc.user.id = :userId AND uc.user.status = 'ACTIVE' AND uc.consentType = :consentType")
    Optional<UserConsent> findByUserIdAndConsentType(@Param("userId") Long userId, @Param("consentType") UserConsent.ConsentType consentType);

    /**
     * 특정 동의 타입에 동의한 모든 사용자 조회
     *
     * <p>
     * 약관 버전 업데이트 시 재동의가 필요한 사용자를 식별하거나
     * 특정 동의 항목의 이용 현황을 분석할 때 사용됩니다.
     * </p>
     *
     * @param consentType 조회할 동의 타입
     * @return 해당 동의에 동의한 사용자들의 동의 정보 목록
     */
    List<UserConsent> findByConsentTypeAndAgreedTrue(UserConsent.ConsentType consentType);

    /**
     * 사용자의 필수 동의 완료 여부 확인
     *
     * <p>
     * 온보딩 완료 조건을 체크하는 핵심 메서드입니다.
     * 서비스 이용약관, 개인정보처리방침, 일정 기반 추천 동의가 모두 완료되어야 true를 반환합니다.
     * 이 조건을 만족하지 않으면 사용자는 홈 화면에 접근할 수 없습니다.
     * </p>
     *
     * @param userId 사용자 ID
     * @return 필수 동의 3개가 모두 완료되었으면 true, 아니면 false
     */
    @Query("SELECT CASE WHEN COUNT(uc) = 3 THEN true ELSE false END " +
            "FROM UserConsent uc " +
            "WHERE uc.user.id = :userId " +
            "AND uc.consentType IN ('TERMS_OF_SERVICE', 'PRIVACY_POLICY', 'CALENDAR_PERSONALIZATION') " +
            "AND uc.agreed = true")
    boolean hasAllRequiredConsents(@Param("userId") Long userId);

    /**
     * 활성 사용자의 특정 동의 타입 동의 여부 확인
     *
     * <p>
     * AI 추천 서비스 접근 권한을 확인할 때 사용됩니다.
     * 활성 사용자만 대상으로 하며, 탈퇴한 사용자는 동의하지 않은 것으로 처리됩니다.
     * </p>
     *
     * @param userId      사용자 ID
     * @param consentType 확인할 동의 타입
     * @return 활성 사용자가 해당 동의를 했으면 true, 아니면 false
     */
    @Query("SELECT CASE WHEN COUNT(uc) > 0 THEN true ELSE false END " +
            "FROM UserConsent uc " +
            "WHERE uc.user.id = :userId AND uc.user.status = 'ACTIVE' " +
            "AND uc.consentType = :consentType " +
            "AND uc.agreed = true")
    boolean isConsentAgreed(@Param("userId") Long userId,
                            @Param("consentType") UserConsent.ConsentType consentType);

    /**
     * 활성 사용자의 동의한 선택 동의 목록 조회
     *
     * <p>
     * AI 추천 시스템에서 어떤 데이터 소스를 활용할 수 있는지 확인하기 위해 사용됩니다.
     * 활성 사용자만 대상으로 하며, 동의한 항목에 따라 개인화 추천의 범위와 깊이가 결정됩니다.
     * </p>
     *
     * @param userId 사용자 ID
     * @return 활성 사용자가 동의한 선택 동의 타입 목록
     */
    @Query("SELECT uc.consentType FROM UserConsent uc " +
            "WHERE uc.user.id = :userId AND uc.user.status = 'ACTIVE' " +
            "AND uc.consentType IN ('DIARY_PERSONALIZATION', 'TODO_PERSONALIZATION', 'BUCKET_PERSONALIZATION') " +
            "AND uc.agreed = true")
    List<UserConsent.ConsentType> findAgreedOptionalConsentTypes(@Param("userId") Long userId);

    /**
     * 법적 증빙용 사용자 동의 정보 조회 (탈퇴 사용자 포함)
     *
     * <p>
     * 법적 분쟁이나 감사 목적으로 사용자의 동의 기록을 조회할 때 사용됩니다.
     * 사용자 상태와 관계없이 모든 동의 기록을 반환하며, 탈퇴한 사용자의 기록도 포함됩니다.
     * 일반적인 서비스 로직에서는 사용하지 않고, 관리자 또는 법무팀에서만 사용합니다.
     * </p>
     *
     * @param userId 사용자 ID
     * @return 해당 사용자의 모든 동의 정보 목록 (사용자 상태 무관)
     */
    @Query("SELECT uc FROM UserConsent uc WHERE uc.user.id = :userId")
    List<UserConsent> findByUserIdForAudit(@Param("userId") Long userId);

    /**
     * 특정 동의 타입의 동의 사용자 수 조회
     *
     * <p>
     * 서비스 통계나 기능 사용률 분석을 위해 사용됩니다.
     * 어떤 AI 개인화 기능이 가장 많이 활용되는지 파악할 수 있습니다.
     * </p>
     *
     * @param consentType 조회할 동의 타입
     * @return 해당 동의를 한 사용자 수
     */
    @Query("SELECT COUNT(DISTINCT uc.user.id) FROM UserConsent uc " +
            "WHERE uc.consentType = :consentType AND uc.agreed = true")
    long countUsersWithConsent(@Param("consentType") UserConsent.ConsentType consentType);

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
    @Query("SELECT DISTINCT u.id FROM User u " +
            "WHERE u.status = 'ACTIVE' " +
            "AND (SELECT COUNT(uc) FROM UserConsent uc " +
            "     WHERE uc.user.id = u.id " +
            "     AND uc.consentType IN ('TERMS_OF_SERVICE', 'PRIVACY_POLICY', 'CALENDAR_PERSONALIZATION') " +
            "     AND uc.agreed = true) < 3")
    List<Long> findUsersWithIncompleteRequiredConsents();

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
    @Query("SELECT uc.user.id FROM UserConsent uc " +
            "WHERE uc.consentType = :consentType AND uc.agreed = false")
    List<Long> findUsersWhoWithdrewConsent(@Param("consentType") UserConsent.ConsentType consentType);

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
    @Query("SELECT uc FROM UserConsent uc " +
            "WHERE uc.agreedAt > :afterDate " +
            "ORDER BY uc.agreedAt DESC")
    List<UserConsent> findConsentsUpdatedAfter(@Param("afterDate") java.time.LocalDateTime afterDate);

    /**
     * 활성 사용자별 동의 완성도 조회
     *
     * <p>
     * 각 활성 사용자가 총 몇 개의 동의 항목에 동의했는지 조회합니다.
     * 탈퇴한 사용자는 제외하고, 활성 사용자의 참여도나 개인화 서비스 활용도 측정에 사용됩니다.
     * </p>
     *
     * @param userId 사용자 ID
     * @return 해당 활성 사용자가 동의한 총 항목 수 (탈퇴 사용자는 0)
     */
    @Query("SELECT COUNT(uc) FROM UserConsent uc " +
            "WHERE uc.user.id = :userId AND uc.user.status = 'ACTIVE' AND uc.agreed = true")
    long countAgreedConsentsByUser(@Param("userId") Long userId);

    /**
     * 전체 동의 현황 통계 조회
     *
     * <p>
     * 모든 동의 타입별 동의율을 한 번에 조회합니다.
     * 관리자 대시보드나 서비스 현황 보고서 작성에 활용할 수 있습니다.
     * </p>
     *
     * @return 동의 타입별 동의 사용자 수 통계
     */
    @Query("SELECT uc.consentType, COUNT(DISTINCT uc.user.id) " +
            "FROM UserConsent uc " +
            "WHERE uc.agreed = true " +
            "GROUP BY uc.consentType")
    List<Object[]> getConsentStatistics();

    /**
     * AI 개인화 동의 완료 사용자 조회
     *
     * <p>
     * 모든 선택 동의(일기, 할일, 버킷리스트)에 동의한 사용자들을 조회합니다.
     * 최고 수준의 개인화 서비스를 받을 수 있는 사용자 집단 식별에 사용됩니다.
     * </p>
     *
     * @return 모든 선택 동의를 완료한 사용자 ID 목록
     */
    @Query("SELECT u.id FROM User u " +
            "WHERE (SELECT COUNT(uc) FROM UserConsent uc " +
            "       WHERE uc.user.id = u.id " +
            "       AND uc.consentType IN ('DIARY_PERSONALIZATION', 'TODO_PERSONALIZATION', 'BUCKET_PERSONALIZATION') " +
            "       AND uc.agreed = true) = 3")
    List<Long> findUsersWithFullPersonalizationConsent();

    /**
     * 동의 항목별 철회율 조회
     *
     * <p>
     * 각 동의 항목이 얼마나 자주 철회되는지 분석하기 위해 사용됩니다.
     * 사용자가 부담스러워하는 동의 항목을 파악하여 서비스 개선에 활용할 수 있습니다.
     * </p>
     *
     * @return 동의 타입별 총 레코드 수와 철회 수 통계
     */
    @Query("SELECT uc.consentType, " +
            "       COUNT(*) as totalCount, " +
            "       SUM(CASE WHEN uc.agreed = false THEN 1 ELSE 0 END) as withdrawnCount " +
            "FROM UserConsent uc " +
            "WHERE uc.consentType IN ('DIARY_PERSONALIZATION', 'TODO_PERSONALIZATION', 'BUCKET_PERSONALIZATION') " +
            "GROUP BY uc.consentType")
    List<Object[]> getWithdrawalRateStatistics();
}