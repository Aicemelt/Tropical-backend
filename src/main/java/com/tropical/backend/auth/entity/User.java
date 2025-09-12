package com.tropical.backend.auth.entity;

import com.tropical.backend.bucketList.entity.BucketList;
import com.tropical.backend.diary.entity.Diary;
import com.tropical.backend.schedule.entity.Schedule;
import com.tropical.backend.smalltalk.entity.SmalltalkTopic;
import com.tropical.backend.todo.entity.Todo;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 사용자 기본 정보를 관리하는 엔터티
 *
 * <p>
 * 로컬 계정과 소셜 계정을 모두 처리하는 통합 사용자 테이블입니다.
 * 사용자의 기본 정보, 인증 상태, 개인 설정, 알림 설정 등을 관리합니다.
 * </p>
 *
 * <p>주요 기능:</p>
 * <ul>
 *   <li>로컬 계정: 이메일/비밀번호 기반 인증</li>
 *   <li>소셜 계정: OAuth2 기반 소셜 로그인 (구글, 카카오, 네이버)</li>
 *   <li>개인화 설정: 캘린더 표시 설정, 알림 설정</li>
 *   <li>논리적 삭제: 물리적 삭제 대신 상태 변경으로 관리</li>
 * </ul>
 *
 * @author 왕택준
 * @version 0.1
 * @since 2025.09.13
 */
@Entity
@Table(name = "user")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"socialAccounts", "userConsents", "schedules", "diaries", "todos", "bucketLists", "smalltalkTopics"})
@Builder
public class User {

    /**
     * 사용자 고유 식별자 (Primary Key)
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 사용자 이메일 주소
     *
     * <p>
     * 로컬 계정의 경우 로그인 ID로 사용되며, 소셜 계정의 경우
     * 소셜 플랫폼에서 제공받은 이메일이 저장됩니다.
     * 동일 이메일로 여러 소셜 계정 생성을 허용합니다.
     * </p>
     */
    @Column(nullable = false)
    private String email;

    /**
     * 암호화된 비밀번호
     *
     * <p>
     * 로컬 계정만 사용하며, 소셜 계정의 경우 NULL입니다.
     * BCrypt 또는 Argon2id를 사용하여 해시화됩니다.
     * </p>
     */
    @Column(name = "password_hash")
    private String passwordHash;

    /**
     * 이메일 인증 완료 여부
     *
     * <p>
     * 로컬 계정: false로 시작하여 이메일 인증 완료 시 true
     * 소셜 계정: 소셜 플랫폼에서 이미 검증되었으므로 true
     * </p>
     */
    @Column(name = "email_verified", nullable = false)
    @Builder.Default
    private Boolean emailVerified = false;

    /**
     * 계정 타입 구분
     *
     * @see AccountType
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "account_type", nullable = false)
    private AccountType accountType;

    /**
     * 사용자 닉네임
     *
     * <p>
     * 전체 시스템에서 유일해야 하며, 소셜 로그인 시 중복되면
     * 자동으로 숫자 suffix가 추가됩니다. (예: "홍길동1", "홍길동2")
     * </p>
     */
    @Column(nullable = false, unique = true, length = 50)
    private String nickname;

    /**
     * 사용자 생년월일
     *
     * <p>선택 정보로, 향후 연령대별 추천 서비스에 활용될 수 있습니다.</p>
     */
    @Column(name = "birth_date")
    private LocalDate birthDate;

    /**
     * 온보딩 완료 여부
     *
     * <p>
     * 회원가입 후 필수/선택 동의를 모두 완료했는지 여부를 나타냅니다.
     * false인 사용자는 온보딩 페이지로 강제 리다이렉트됩니다.
     * </p>
     */
    @Column(name = "onboarding_completed", nullable = false)
    @Builder.Default
    private Boolean onboardingCompleted = false;

    /**
     * 계정 생성 시간
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    /**
     * 마지막 로그인 시간
     *
     * <p>로그인 성공 시마다 자동으로 업데이트됩니다.</p>
     */
    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    /**
     * 사용자 계정 상태
     *
     * @see UserStatus
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private UserStatus status = UserStatus.ACTIVE;

    // ===== 캘린더 표시 개인 설정 =====

    /**
     * 주 시작 요일 설정
     *
     * @see WeekStart
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "week_start", nullable = false)
    @Builder.Default
    private WeekStart weekStart = WeekStart.MON;

    /**
     * 시간대 설정
     *
     * <p>기본값: Asia/Seoul</p>
     */
    @Column(nullable = false)
    @Builder.Default
    private String timezone = "Asia/Seoul";

    /**
     * 공휴일 표시 여부
     *
     * <p>캘린더에서 한국 공휴일을 표시할지 여부를 결정합니다.</p>
     */
    @Column(name = "show_holidays", nullable = false)
    @Builder.Default
    private Boolean showHolidays = true;

    // ===== 알림 설정 =====

    /**
     * AI 스몰 토크 알림 활성화 여부
     */
    @Column(name = "smalltalk_notification_enabled", nullable = false)
    @Builder.Default
    private Boolean smalltalkNotificationEnabled = true;

    /**
     * AI 스몰 토크 알림 시간
     *
     * <p>기본값: 오전 8시</p>
     */
    @Column(name = "smalltalk_notification_time", nullable = false)
    @Builder.Default
    private LocalTime smalltalkNotificationTime = LocalTime.of(8, 0);

    /**
     * AI 스몰 토크 알림 요일 설정
     *
     * <p>
     * 가능한 값: "daily", "weekdays", "weekends", "custom"
     * custom인 경우 별도 요일 설정 테이블 참조
     * </p>
     */
    @Column(name = "smalltalk_notification_days", nullable = false)
    @Builder.Default
    private String smalltalkNotificationDays = "daily";

    /**
     * 일정 시작 전 알림 활성화 여부
     */
    @Column(name = "schedule_notification_enabled", nullable = false)
    @Builder.Default
    private Boolean scheduleNotificationEnabled = true;

    /**
     * 일정 시작 전 알림 시간 (분 단위)
     *
     * <p>기본값: 30분 전</p>
     */
    @Column(name = "schedule_notification_minutes", nullable = false)
    @Builder.Default
    private Integer scheduleNotificationMinutes = 30;

    /**
     * 투두 마감 알림 활성화 여부
     */
    @Column(name = "todo_notification_enabled", nullable = false)
    @Builder.Default
    private Boolean todoNotificationEnabled = true;

    /**
     * 투두 마감 알림 시간
     *
     * <p>기본값: 오전 8시</p>
     */
    @Column(name = "todo_notification_time", nullable = false)
    @Builder.Default
    private LocalTime todoNotificationTime = LocalTime.of(8, 0);

    /**
     * 투두 마감 며칠 전 알림 설정
     *
     * <p>기본값: 1일 전</p>
     */
    @Column(name = "todo_notification_days_before", nullable = false)
    @Builder.Default
    private Integer todoNotificationDaysBefore = 1;

    // ===== 연관관계 =====

    /**
     * 사용자의 소셜 계정 연동 정보 목록
     *
     * <p>
     * 한 사용자가 여러 소셜 제공자(구글, 카카오, 네이버)와
     * 동시에 연동할 수 있습니다.
     * </p>
     */
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<SocialAccount> socialAccounts = new ArrayList<>();

    /**
     * 사용자의 동의 정보 목록
     *
     * <p>필수 동의와 선택 동의 정보를 모두 포함합니다.</p>
     */
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<UserConsent> userConsents = new ArrayList<>();

    /**
     * 사용자의 일정 목록
     */
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Schedule> schedules = new ArrayList<>();

    /**
     * 사용자의 일기 목록
     */
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Diary> diaries = new ArrayList<>();

    /**
     * 사용자의 투두 목록
     */
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Todo> todos = new ArrayList<>();

    /**
     * 사용자의 버킷리스트 목록
     */
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<BucketList> bucketLists = new ArrayList<>();

    /**
     * 사용자의 AI 스몰 토크 주제 목록
     */
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<SmalltalkTopic> smalltalkTopics = new ArrayList<>();

    // ===== 열거형 정의 =====

    /**
     * 계정 타입 열거형
     */
    public enum AccountType {
        /**
         * 로컬 계정 (이메일/비밀번호)
         */
        LOCAL,
        /**
         * 소셜 계정 (OAuth2)
         */
        SOCIAL
    }

    /**
     * 사용자 상태 열거형
     */
    public enum UserStatus {
        /**
         * 활성 상태
         */
        ACTIVE,
        /**
         * 차단 상태
         */
        BLOCKED,
        /**
         * 삭제 상태 (논리적 삭제)
         */
        DELETED
    }

    /**
     * 주 시작 요일 열거형
     */
    public enum WeekStart {
        /**
         * 일요일 시작
         */
        SUN,
        /**
         * 월요일 시작
         */
        MON
    }

    // ===== 비즈니스 메서드 =====

    /**
     * 로컬 계정 생성용 정적 팩토리 메서드
     *
     * @param email        사용자 이메일
     * @param passwordHash 암호화된 비밀번호
     * @param nickname     닉네임
     * @return 로컬 사용자 엔터티
     */
    public static User createLocalUser(String email, String passwordHash, String nickname) {
        return User.builder()
                .email(email)
                .passwordHash(passwordHash)
                .nickname(nickname)
                .accountType(AccountType.LOCAL)
                .emailVerified(false)
                .build();
    }

    /**
     * 소셜 계정 생성용 정적 팩토리 메서드
     *
     * @param email    소셜 플랫폼에서 제공받은 이메일
     * @param nickname 닉네임
     * @return 소셜 사용자 엔터티
     */
    public static User createSocialUser(String email, String nickname) {
        return User.builder()
                .email(email)
                .passwordHash(null)
                .nickname(nickname)
                .accountType(AccountType.SOCIAL)
                .emailVerified(true)
                .build();
    }

    /**
     * 마지막 로그인 시간 업데이트
     */
    public void updateLastLogin() {
        this.lastLoginAt = LocalDateTime.now();
    }

    /**
     * 이메일 인증 완료 처리
     */
    public void verifyEmail() {
        this.emailVerified = true;
    }

    /**
     * 온보딩 완료 처리
     */
    public void completeOnboarding() {
        this.onboardingCompleted = true;
    }

    /**
     * 닉네임 변경
     *
     * @param nickname 새로운 닉네임
     */
    public void changeNickname(String nickname) {
        this.nickname = nickname;
    }

    /**
     * 계정 논리적 삭제 처리
     *
     * <p>
     * 물리적 삭제 대신 상태를 DELETED로 변경하고
     * 개인정보를 마스킹 처리합니다.
     * </p>
     */
    public void softDelete() {
        this.status = UserStatus.DELETED;
        this.email = "deleted_" + this.id + "_" + System.currentTimeMillis() + "@deleted.com";
        this.nickname = "탈퇴회원_" + this.id;
    }

    /**
     * 활성 계정 여부 확인
     *
     * @return 활성 상태면 true, 아니면 false
     */
    public boolean isActive() {
        return this.status == UserStatus.ACTIVE;
    }

    /**
     * 로컬 계정 여부 확인
     *
     * @return 로컬 계정이면 true, 아니면 false
     */
    public boolean isLocalAccount() {
        return this.accountType == AccountType.LOCAL;
    }

    /**
     * 소셜 계정 여부 확인
     *
     * @return 소셜 계정이면 true, 아니면 false
     */
    public boolean isSocialAccount() {
        return this.accountType == AccountType.SOCIAL;
    }

    /**
     * 이메일 인증 완료 여부 확인
     *
     * @return 인증 완료면 true, 아니면 false
     */
    public boolean isEmailVerified() {
        return Boolean.TRUE.equals(this.emailVerified);
    }

    /**
     * 온보딩 완료 여부 확인
     *
     * @return 온보딩 완료면 true, 아니면 false
     */
    public boolean isOnboardingCompleted() {
        return Boolean.TRUE.equals(this.onboardingCompleted);
    }
}