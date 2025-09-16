package com.tropical.backend.auth.repository;

import com.tropical.backend.auth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 사용자 정보 데이터 접근 Repository
 *
 * <p>
 * 로컬 계정과 소셜 계정을 모두 처리하는 사용자 데이터 접근 계층입니다.
 * 인증, 회원가입, 사용자 관리 등의 기능을 위한 데이터베이스 쿼리를 제공합니다.
 * </p>
 *
 * <p>주요 기능:</p>
 * <ul>
 *   <li>활성 사용자 필터링 (논리적 삭제 대응)</li>
 *   <li>이메일/닉네임 중복 검증</li>
 *   <li>JWT 토큰 검증을 위한 사용자 조회</li>
 *   <li>온보딩 상태 기반 사용자 조회</li>
 *   <li>닉네임 자동 생성을 위한 패턴 검색</li>
 * </ul>
 *
 * @author 왕택준
 * @version 0.1
 * @since 2025.09.13
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * 이메일로 활성 사용자 조회
     *
     * <p>
     * 논리적 삭제된 사용자(DELETED 상태)는 제외하고 조회합니다.
     * 로그인 시 사용자 인증에 주로 사용됩니다.
     * </p>
     *
     * @param email 조회할 사용자 이메일
     * @return 활성 상태의 사용자 Optional, 존재하지 않거나 삭제된 사용자인 경우 empty
     */
    @Query("SELECT u FROM User u WHERE u.email = :email AND u.status = 'ACTIVE'")
    Optional<User> findByEmailAndActive(@Param("email") String email);

    /**
     * 이메일로 사용자 조회 (상태 무관)
     *
     * <p>
     * 사용자의 상태에 관계없이 이메일로 조회합니다.
     * 회원가입 시 이메일 중복 체크나 이메일 인증 시 사용됩니다.
     * </p>
     *
     * @param email 조회할 사용자 이메일
     * @return 해당 이메일의 사용자 Optional, 존재하지 않는 경우 empty
     */
    Optional<User> findByEmail(String email);

    /**
     * 닉네임으로 사용자 조회
     *
     * <p>
     * 닉네임 중복 체크나 사용자 검색 시 사용됩니다.
     * 닉네임은 시스템 전체에서 유일해야 합니다.
     * </p>
     *
     * @param nickname 조회할 사용자 닉네임
     * @return 해당 닉네임의 사용자 Optional, 존재하지 않는 경우 empty
     */
    Optional<User> findByNickname(String nickname);

    /**
     * 닉네임 존재 여부 확인
     *
     * <p>
     * 회원가입이나 닉네임 변경 시 중복 여부를 빠르게 확인합니다.
     * 성능상 count 쿼리보다 exists 쿼리가 더 효율적입니다.
     * </p>
     *
     * @param nickname 확인할 닉네임
     * @return 해당 닉네임이 이미 존재하면 true, 사용 가능하면 false
     */
    boolean existsByNickname(String nickname);

    /**
     * 이메일 존재 여부 확인 (활성 계정만)
     *
     * <p>
     * 활성 상태의 계정 중에서만 이메일 중복을 체크합니다.
     * 탈퇴한 사용자의 이메일은 재사용이 가능하도록 합니다.
     * </p>
     *
     * @param email 확인할 이메일
     * @return 활성 계정 중 해당 이메일이 존재하면 true, 사용 가능하면 false
     */
    @Query("SELECT CASE WHEN COUNT(u) > 0 THEN true ELSE false END " +
            "FROM User u WHERE u.email = :email AND u.status = 'ACTIVE'")
    boolean existsByEmailAndActive(@Param("email") String email);

    /**
     * 사용자 ID와 이메일로 활성 사용자 조회
     *
     * <p>
     * JWT 토큰 검증 시 사용됩니다. 토큰에 포함된 사용자 ID와 이메일이
     * 모두 일치하는 활성 사용자만 반환하여 보안을 강화합니다.
     * </p>
     *
     * @param id    사용자 ID (JWT subject에서 추출)
     * @param email 사용자 이메일 (JWT claim에서 추출)
     * @return ID와 이메일이 모두 일치하는 활성 사용자 Optional
     */
    @Query("SELECT u FROM User u WHERE u.id = :id AND u.email = :email AND u.status = 'ACTIVE'")
    Optional<User> findByIdAndEmailAndActive(@Param("id") Long id, @Param("email") String email);

    /**
     * 활성 사용자만 ID로 조회
     *
     * <p>
     * 기본 findById와 달리 활성 상태인 사용자만 반환합니다.
     * 대부분의 비즈니스 로직에서 탈퇴한 사용자는 접근할 수 없도록 합니다.
     * </p>
     *
     * @param id 사용자 ID
     * @return 활성 상태의 사용자 Optional, 존재하지 않거나 비활성 상태인 경우 empty
     */
    @Query("SELECT u FROM User u WHERE u.id = :id AND u.status = 'ACTIVE'")
    Optional<User> findByIdAndActive(@Param("id") Long id);

    /**
     * 닉네임 패턴으로 사용자 수 조회
     *
     * <p>
     * 소셜 로그인 시 닉네임 중복이 발생할 때 자동으로 suffix를 추가하기 위해 사용됩니다.
     * 예: "홍길동" → "홍길동1", "홍길동2" 생성 시 기존 개수를 확인합니다.
     * </p>
     *
     * @param nicknamePattern 닉네임 패턴 (LIKE 연산자 사용, 예: "홍길동%")
     * @return 해당 패턴과 일치하는 사용자 수
     */
    @Query("SELECT COUNT(u) FROM User u WHERE u.nickname LIKE :nicknamePattern")
    long countByNicknamePattern(@Param("nicknamePattern") String nicknamePattern);

    /**
     * 온보딩 미완료 사용자 조회
     *
     * <p>
     * 회원가입 후 온보딩(필수 동의)을 완료하지 않은 사용자를 조회합니다.
     * 해당 사용자들은 홈 화면 대신 온보딩 페이지로 리다이렉트됩니다.
     * </p>
     *
     * @param id 사용자 ID
     * @return 온보딩이 미완료된 활성 사용자 Optional
     */
    @Query("SELECT u FROM User u WHERE u.id = :id AND u.status = 'ACTIVE' AND u.onboardingCompleted = false")
    Optional<User> findByIdAndOnboardingIncomplete(@Param("id") Long id);

    /**
     * 이메일 인증 미완료 사용자 조회
     *
     * <p>
     * 로컬 계정 중에서 이메일 인증을 완료하지 않은 사용자를 조회합니다.
     * 이메일 인증 처리 시 대상 사용자 확인에 사용됩니다.
     * </p>
     *
     * @param email 사용자 이메일
     * @return 이메일 인증이 미완료된 활성 사용자 Optional
     */
    @Query("SELECT u FROM User u WHERE u.email = :email AND u.status = 'ACTIVE' " +
            "AND u.accountType = 'LOCAL' AND u.emailVerified = false")
    Optional<User> findByEmailAndUnverified(@Param("email") String email);

    /**
     * 로컬 계정 이메일 중복 확인
     *
     * <p>
     * 로컬 계정끼리만 이메일 중복을 체크합니다.
     * 소셜 계정과는 별도로 관리하여 같은 이메일의 다중 계정을 허용합니다.
     * </p>
     *
     * @param email 확인할 이메일
     * @return 로컬 계정 중 해당 이메일이 존재하면 true, 사용 가능하면 false
     */
    @Query("SELECT CASE WHEN COUNT(u) > 0 THEN true ELSE false END " +
           "FROM User u WHERE u.email = :email AND u.status = 'ACTIVE' AND u.accountType = 'LOCAL'")
    boolean existsByEmailAndActiveAndLocal(@Param("email") String email);

    /**
     * 이메일 인증 완료 여부 확인
     *
     * <p>
     * 이메일 인증이 이미 완료된 사용자인지 빠르게 확인합니다.
     * 중복 인증 요청 처리 시 사용됩니다.
     * </p>
     *
     * @param email 확인할 이메일
     * @return 해당 이메일이 인증 완료된 상태면 true, 아니면 false
     */
    @Query("SELECT CASE WHEN COUNT(u) > 0 THEN true ELSE false END " +
           "FROM User u WHERE u.email = :email AND u.emailVerified = true AND u.status = 'ACTIVE'")
    boolean existsByEmailAndVerified(@Param("email") String email);

    /**
     * 특정 기간 이후 마지막 로그인한 사용자 목록 조회
     *
     * <p>
     * 사용자 활동 분석이나 휴면 계정 관리를 위해 사용됩니다.
     * 관리자 기능이나 배치 작업에서 활용할 수 있습니다.
     * </p>
     *
     * @param afterDate 기준 날짜 (이후에 로그인한 사용자들)
     * @return 해당 기간 이후 로그인한 활성 사용자 목록
     */
    @Query("SELECT u FROM User u WHERE u.status = 'ACTIVE' AND u.lastLoginAt > :afterDate")
    java.util.List<User> findActiveUsersLoggedInAfter(@Param("afterDate") java.time.LocalDateTime afterDate);

    /**
     * 계정 타입별 활성 사용자 수 조회
     *
     * <p>
     * 로컬 계정과 소셜 계정의 비율을 파악하기 위한 통계 쿼리입니다.
     * 서비스 분석이나 대시보드에서 활용할 수 있습니다.
     * </p>
     *
     * @param accountType 계정 타입 (LOCAL 또는 SOCIAL)
     * @return 해당 계정 타입의 활성 사용자 수
     */
    @Query("SELECT COUNT(u) FROM User u WHERE u.status = 'ACTIVE' AND u.accountType = :accountType")
    long countActiveUsersByAccountType(@Param("accountType") User.AccountType accountType);
}