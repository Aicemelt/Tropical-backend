package com.tropical.backend.auth.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * 사용자 동의 정보를 관리하는 엔터티
 *
 * <p>
 * 필수 동의와 선택 동의를 구분하여 관리하며,
 * AI 추천 서비스의 개인화 수준을 결정하는 핵심 정보입니다.
 * </p>
 *
 * <p>주요 기능:</p>
 * <ul>
 *   <li>필수 동의: 서비스 이용을 위한 최소 요구사항</li>
 *   <li>선택 동의: AI 추천 개인화를 위한 추가 데이터 수집 동의</li>
 *   <li>동의 철회 및 변경 이력 관리</li>
 *   <li>GDPR 및 개인정보보호법 준수</li>
 * </ul>
 *
 * @author 왕택준
 * @version 0.1
 * @since 2025.09.13
 */
@Entity
@Table(name = "user_consent",
        uniqueConstraints = @UniqueConstraint(
                columnNames = {"user_id", "consent_type"},
                name = "uk_user_consent_type"
        ))
@Getter
@ToString(exclude = {"user"})
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserConsent {

    /**
     * 사용자 동의 정보 고유 식별자 (Primary Key)
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 동의한 사용자 정보
     *
     * <p>지연 로딩을 사용하여 성능을 최적화합니다.</p>
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * 동의 항목 타입
     *
     * <p>
     * 필수 동의와 선택 동의로 구분되며,
     * AI 추천 시스템과의 연동에 직접적으로 영향을 미칩니다.
     * </p>
     *
     * @see ConsentType
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "consent_type", nullable = false)
    private ConsentType consentType;

    /**
     * 동의 여부
     *
     * <p>
     * true: 동의함, false: 동의하지 않음
     * 필수 동의는 모두 true여야 서비스 이용이 가능합니다.
     * </p>
     */
    @Column(nullable = false)
    private Boolean agreed;

    /**
     * 동의 처리 시간
     *
     * <p>
     * 최초 동의 또는 동의 변경 시 자동으로 설정됩니다.
     * 법적 요구사항에 따른 동의 이력 추적을 위해 사용됩니다.
     * </p>
     */
    @CreationTimestamp
    @Column(name = "agreed_at", nullable = false)
    private LocalDateTime agreedAt;

    /**
     * 동의 항목 타입 열거형
     *
     * <p>
     * AI 추천 시스템의 데이터 소스와 직접 연결되어 있으며,
     * 각 동의 항목에 따라 수집 및 활용되는 데이터가 결정됩니다.
     * </p>
     */
    public enum ConsentType {
        // ===== 필수 동의 (서비스 이용 최소 요구사항) =====

        /**
         * 서비스 이용약관 동의
         *
         * <p>플랫폼 이용을 위한 기본 약관 동의입니다.</p>
         */
        TERMS_OF_SERVICE("서비스 이용약관", true),

        /**
         * 일정 기반 추천 동의
         *
         * <p>
         * 캘린더 앱의 핵심 기능으로, 사용자의 일정 데이터를 기반으로
         * AI 스몰 토크를 제공하기 위한 필수 동의입니다.
         * </p>
         */
        CALENDAR_PERSONALIZATION("일정 기반 추천 동의", true),

        // ===== 선택 동의 (AI 추가 개인화) =====

        /**
         * 일기 기반 추천 동의
         *
         * <p>
         * 사용자의 일기 데이터를 분석하여 감정 상태 및 기분을 파악하고,
         * 이를 바탕으로 한 개인화된 AI 추천을 제공합니다.
         * </p>
         */
        DIARY_PERSONALIZATION("일기 기반 추천 동의", false),

        /**
         * 할일 기반 추천 동의
         *
         * <p>
         * 사용자의 투두 리스트 패턴을 분석하여 업무/학습 스타일을 파악하고,
         * 생산성 향상을 위한 개인화된 AI 추천을 제공합니다.
         * </p>
         */
        TODO_PERSONALIZATION("할일 기반 추천 동의", false),

        /**
         * 버킷리스트 기반 추천 동의
         *
         * <p>
         * 사용자의 버킷리스트를 분석하여 취미, 관심사, 목표를 파악하고,
         * 라이프스타일 개선을 위한 개인화된 AI 추천을 제공합니다.
         * </p>
         */
        BUCKET_PERSONALIZATION("버킷리스트 기반 추천 동의", false);

        private final String description;
        private final boolean required;

        ConsentType(String description, boolean required) {
            this.description = description;
            this.required = required;
        }

        /**
         * 동의 항목 설명 반환
         *
         * @return 동의 항목 설명
         */
        public String getDescription() {
            return description;
        }

        /**
         * 필수 동의 여부 확인
         *
         * @return 필수 동의면 true, 선택 동의면 false
         */
        public boolean isRequired() {
            return required;
        }

        /**
         * 필수 동의 항목들 반환
         *
         * @return 필수 동의 항목 배열
         */
        public static ConsentType[] getRequiredConsents() {
            return new ConsentType[]{
                    TERMS_OF_SERVICE,
                    CALENDAR_PERSONALIZATION
            };
        }

        /**
         * 선택 동의 항목들 반환
         *
         * @return 선택 동의 항목 배열
         */
        public static ConsentType[] getOptionalConsents() {
            return new ConsentType[]{
                    DIARY_PERSONALIZATION,
                    TODO_PERSONALIZATION,
                    BUCKET_PERSONALIZATION
            };
        }
    }

    // ===== 비즈니스 메서드 =====

    /**
     * 사용자 동의 생성 정적 팩토리 메서드
     *
     * @param user        동의한 사용자
     * @param consentType 동의 항목 타입
     * @param agreed      동의 여부
     * @return 사용자 동의 엔터티
     */
    public static UserConsent createUserConsent(User user, ConsentType consentType, boolean agreed) {
        return UserConsent.builder()
                .user(user)
                .consentType(consentType)
                .agreed(agreed)
                .build();
    }

    /**
     * 동의 상태 변경
     *
     * <p>
     * 동의 상태를 변경하면 agreedAt 시간이 자동으로 업데이트됩니다.
     * 이는 JPA의 @CreationTimestamp와 함께 동작하여 변경 이력을 추적합니다.
     * </p>
     *
     * @param agreed 새로운 동의 상태
     */
    public void updateAgreement(boolean agreed) {
        this.agreed = agreed;
        this.agreedAt = LocalDateTime.now();
    }

    /**
     * 사용자 설정 (연관관계 편의 메서드)
     *
     * <p>
     * 양방향 연관관계를 안전하게 설정합니다.
     * 사용자의 동의 목록에도 자동으로 추가됩니다.
     * </p>
     *
     * @param user 연결할 사용자
     */
    public void setUser(User user) {
        this.user = user;
        if (user != null && !user.getUserConsents().contains(this)) {
            user.getUserConsents().add(this);
        }
    }

    /**
     * 동의 여부 확인
     *
     * @return 동의했으면 true, 아니면 false
     */
    public boolean isAgreed() {
        return Boolean.TRUE.equals(this.agreed);
    }

    /**
     * 동의 철회
     *
     * <p>
     * 선택 동의 항목에 대해서만 철회가 가능하며,
     * 필수 동의 항목은 철회할 수 없습니다.
     * </p>
     *
     * @throws IllegalStateException 필수 동의 항목을 철회하려는 경우
     */
    public void withdraw() {
        if (this.consentType.isRequired()) {
            throw new IllegalStateException("필수 동의 항목은 철회할 수 없습니다: " + this.consentType.getDescription());
        }
        updateAgreement(false);
    }

    /**
     * 동의 재동의
     *
     * <p>철회된 선택 동의 항목에 대해 다시 동의할 때 사용합니다.</p>
     */
    public void reConsent() {
        updateAgreement(true);
    }
}