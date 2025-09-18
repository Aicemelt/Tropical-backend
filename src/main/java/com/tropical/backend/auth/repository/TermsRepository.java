package com.tropical.backend.auth.repository;

import com.tropical.backend.auth.entity.Terms;
import com.tropical.backend.auth.entity.UserConsent.ConsentType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 약관 데이터 접근 레포지토리
 *
 * @author 왕택준
 * @version 0.1
 * @since 2025.09.18
 */
@Repository
public interface TermsRepository extends JpaRepository<Terms, Long> {

    /**
     * 특정 동의 타입의 활성 약관 조회
     *
     * <p>
     * 각 ConsentType당 활성 약관은 1개만 존재하므로,
     * findTop...과 동일한 결과를 반환합니다.
     * 비즈니스 로직의 명확성을 위해 이 메서드를 주로 사용하세요.
     * </p>
     *
     * @param consentType 동의 항목 타입
     * @return 활성 약관 (없으면 Optional.empty())
     */
    Optional<Terms> findByConsentTypeAndActiveTrue(ConsentType consentType);

    /**
     * 모든 활성 약관 조회
     *
     * @return 활성 약관 목록
     */
    List<Terms> findByActiveTrueOrderByConsentType();

    /**
     * 특정 동의 타입의 모든 약관 버전 조회 (최신순)
     *
     * @param consentType 동의 항목 타입
     * @return 약관 버전 목록
     */
    List<Terms> findByConsentTypeOrderByCreatedAtDesc(ConsentType consentType);

    /**
     * 특정 동의 타입과 버전의 약관 조회
     *
     * @param consentType 동의 항목 타입
     * @param version     약관 버전
     * @return 해당 버전의 약관
     */
    Optional<Terms> findByConsentTypeAndVersion(ConsentType consentType, String version);

    /**
     * 특정 동의 타입의 활성 약관을 비활성화
     *
     * <p>
     * 벌크 업데이트이므로 clearAutomatically, flushAutomatically 옵션을 추가하여
     * 영속성 컨텍스트와 DB 간 불일치를 방지합니다.
     * </p>
     *
     * @param consentType 동의 항목 타입
     * @return 업데이트된 행 수
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Terms t SET t.active = false WHERE t.consentType = :consentType AND t.active = true")
    int deactivateByConsentType(@Param("consentType") ConsentType consentType);
}