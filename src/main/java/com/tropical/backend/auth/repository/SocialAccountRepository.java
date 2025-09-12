package com.tropical.backend.auth.repository;

import com.tropical.backend.auth.entity.SocialAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 소셜 계정 연동 정보 데이터 접근 Repository
 *
 * <p>
 * 카카오, 구글, 네이버 등의 소셜 로그인 계정 정보를 관리하는 데이터 접근 계층입니다.
 * OAuth2 인증, 소셜 계정 연동/해제, 사용자별 소셜 계정 관리 등의 기능을 지원합니다.
 * </p>
 *
 * <p>주요 기능:</p>
 * <ul>
 *   <li>소셜 제공자별 계정 조회 및 중복 방지</li>
 *   <li>활성 사용자 필터링을 통한 보안 강화</li>
 *   <li>사용자별 다중 소셜 계정 연동 관리</li>
 *   <li>소셜 계정 연동 해제 및 정리</li>
 *   <li>소셜 로그인 통계 및 분석 데이터 제공</li>
 * </ul>
 *
 * @author 왕택준
 * @version 0.1
 * @since 2025.09.13
 */
@Repository
public interface SocialAccountRepository extends JpaRepository<SocialAccount, Long> {

    /**
     * 제공자와 제공자 사용자 ID로 소셜 계정 조회
     *
     * <p>
     * OAuth2 로그인 시 기존 계정 존재 여부를 확인하는 핵심 메서드입니다.
     * (provider, providerUserId) 조합은 유니크 제약이 있어 중복 연결을 방지합니다.
     * </p>
     *
     * @param provider       소셜 제공자 (KAKAO, GOOGLE, NAVER)
     * @param providerUserId 소셜 제공자의 사용자 고유 ID
     * @return 매칭되는 소셜 계정 Optional, 존재하지 않는 경우 empty
     */
    Optional<SocialAccount> findByProviderAndProviderUserId(
            SocialAccount.SocialProvider provider,
            String providerUserId);

    /**
     * 사용자 ID로 소셜 계정 목록 조회
     *
     * <p>
     * 한 사용자가 연동한 모든 소셜 계정을 조회합니다.
     * 마이페이지에서 연동 상태 확인이나 소셜 계정 관리에 사용됩니다.
     * </p>
     *
     * @param userId 사용자 ID
     * @return 해당 사용자의 소셜 계정 목록, 없는 경우 빈 리스트
     */
    List<SocialAccount> findByUserId(Long userId);

    /**
     * 사용자 ID와 제공자로 특정 소셜 계정 조회
     *
     * <p>
     * 특정 소셜 제공자의 계정 연동 해제나 정보 확인 시 사용됩니다.
     * 예: 사용자의 구글 계정만 연동 해제하고 싶을 때
     * </p>
     *
     * @param userId   사용자 ID
     * @param provider 소셜 제공자
     * @return 매칭되는 소셜 계정 Optional, 연동되지 않은 경우 empty
     */
    Optional<SocialAccount> findByUserIdAndProvider(Long userId, SocialAccount.SocialProvider provider);

    /**
     * 특정 소셜 계정의 존재 여부 확인
     *
     * <p>
     * 소셜 계정 중복 연결 방지를 위해 빠르게 존재 여부만 확인합니다.
     * OAuth2 인증 전 사전 체크에 사용할 수 있습니다.
     * </p>
     *
     * @param provider       소셜 제공자
     * @param providerUserId 소셜 제공자의 사용자 ID
     * @return 해당 소셜 계정이 이미 연동되어 있으면 true, 아니면 false
     */
    boolean existsByProviderAndProviderUserId(
            SocialAccount.SocialProvider provider,
            String providerUserId);

    /**
     * 활성 사용자의 소셜 계정만 조회
     *
     * <p>
     * 소셜 로그인 시 탈퇴한 사용자의 계정은 제외하고 조회합니다.
     * 보안상 비활성 사용자의 소셜 계정으로는 로그인할 수 없도록 합니다.
     * </p>
     *
     * @param provider       소셜 제공자
     * @param providerUserId 소셜 제공자의 사용자 ID
     * @return 활성 사용자에 연결된 소셜 계정 Optional
     */
    @Query("SELECT sa FROM SocialAccount sa JOIN sa.user u " +
            "WHERE sa.provider = :provider AND sa.providerUserId = :providerUserId " +
            "AND u.status = 'ACTIVE'")
    Optional<SocialAccount> findByProviderAndProviderUserIdWithActiveUser(
            @Param("provider") SocialAccount.SocialProvider provider,
            @Param("providerUserId") String providerUserId);

    /**
     * 사용자의 모든 소셜 계정 삭제
     *
     * <p>
     * 회원 탈퇴 시 해당 사용자와 연결된 모든 소셜 계정 정보를 삭제합니다.
     * 개인정보보호 및 GDPR 준수를 위해 사용됩니다.
     * </p>
     *
     * @param userId 삭제할 사용자 ID
     */
    void deleteByUserId(Long userId);

    /**
     * 특정 소셜 제공자의 연동 계정 수 조회
     *
     * <p>
     * 서비스 통계나 소셜 로그인 사용 현황 분석을 위해 사용됩니다.
     * 어떤 소셜 제공자가 가장 많이 사용되는지 파악할 수 있습니다.
     * </p>
     *
     * @param provider 소셜 제공자
     * @return 해당 제공자로 연동된 계정 수
     */
    @Query("SELECT COUNT(sa) FROM SocialAccount sa JOIN sa.user u " +
            "WHERE sa.provider = :provider AND u.status = 'ACTIVE'")
    long countActiveAccountsByProvider(@Param("provider") SocialAccount.SocialProvider provider);

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
    @Query("SELECT sa.user.id FROM SocialAccount sa " +
            "GROUP BY sa.user.id " +
            "HAVING COUNT(sa) > 1")
    List<Long> findUserIdsWithMultipleSocialAccounts();

    /**
     * 특정 기간 이후 연동된 소셜 계정 조회
     *
     * <p>
     * 최근 소셜 로그인 가입 동향을 파악하기 위해 사용됩니다.
     * 마케팅 분석이나 성장 지표 측정에 활용할 수 있습니다.
     * </p>
     *
     * @param afterDate 기준 날짜
     * @return 해당 날짜 이후 연동된 소셜 계정 목록
     */
    @Query("SELECT sa FROM SocialAccount sa JOIN sa.user u " +
            "WHERE sa.linkedAt > :afterDate AND u.status = 'ACTIVE'")
    List<SocialAccount> findAccountsLinkedAfter(@Param("afterDate") java.time.LocalDateTime afterDate);

    /**
     * 고아 소셜 계정 조회 (삭제된 사용자와 연결된 계정)
     *
     * <p>
     * 데이터 정리나 무결성 검증을 위해 사용됩니다.
     * 배치 작업에서 불필요한 소셜 계정 정보를 정리할 때 활용할 수 있습니다.
     * </p>
     *
     * @return 삭제된 사용자와 연결된 소셜 계정 목록
     */
    @Query("SELECT sa FROM SocialAccount sa JOIN sa.user u " +
            "WHERE u.status = 'DELETED'")
    List<SocialAccount> findOrphanedAccounts();
}