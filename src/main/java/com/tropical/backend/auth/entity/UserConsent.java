package com.tropical.backend.auth.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
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
 * 약관 버전 추적을 통해 법적 요구사항을 준수합니다.
 * </p>
 *
 * <p>주요 기능:</p>
 * <ul>
 *   <li>필수 동의: 서비스 이용을 위한 최소 요구사항</li>
 *   <li>선택 동의: AI 추천 개인화를 위한 추가 데이터 수집 동의</li>
 *   <li>동의 철회 및 변경 이력 관리</li>
 *   <li>약관 버전별 동의 추적 - 법적 증빙 지원</li>
 *   <li>GDPR 및 개인정보보호법 준수</li>
 * </ul>
 *
 * @author 왕택준
 * @version 0.2
 * @since 2025.09.17
 */
@Entity
@Table(name = "user_consent",
        uniqueConstraints = @UniqueConstraint(
                columnNames = {"user_id", "consent_type"},
                name = "uk_user_consent_type"
        ))
@Getter
@Setter
@ToString(exclude = {"user", "terms"})
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
     * 연결된 약관 정보
     *
     * <p>
     * 사용자가 동의한 약관의 원문과 연결됩니다.
     * 약관이 업데이트되어도 동의 당시의 약관 정보를 추적할 수 있습니다.
     * </p>
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "terms_id")
    private Terms terms;

    /**
     * 동의 당시 약관 버전 스냅샷
     *
     * <p>
     * 법적 증빙을 위해 사용자가 동의한 약관의 정확한 버전을 보존합니다.
     * 약관이 업데이트되어도 "어떤 버전에 동의했는지"를 추적할 수 있습니다.
     * </p>
     */
    @Column(name = "terms_version_snapshot", length = 10)
    private String termsVersionSnapshot;

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
     * JSON API에서는 camelCase로 직렬화/역직렬화됩니다.
     * </p>
     */
    public enum ConsentType {
        // ===== 필수 동의 (서비스 이용 최소 요구사항) =====

        /**
         * 서비스 이용약관 동의
         *
         * <p>플랫폼 이용을 위한 기본 약관 동의입니다.</p>
         */
        @JsonProperty("termsOfService")
        TERMS_OF_SERVICE("서비스 이용약관", true),

        /**
         * 개인정보처리방침 동의
         *
         * <p>개인정보 수집·이용을 위한 법적 필수 동의입니다.</p>
         */
        @JsonProperty("privacyPolicy")
        PRIVACY_POLICY("개인정보처리방침", true),

        /**
         * 일정 기반 추천 동의
         *
         * <p>
         * 캘린더 앱의 핵심 기능으로, 사용자의 일정 데이터를 기반으로
         * AI 스몰 토크를 제공하기 위한 필수 동의입니다.
         * </p>
         */
        @JsonProperty("calendarPersonalization")
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
        @JsonProperty("diaryPersonalization")
        DIARY_PERSONALIZATION("일기 기반 추천 동의", false),

        /**
         * 할일 기반 추천 동의
         *
         * <p>
         * 사용자의 투두 리스트 패턴을 분석하여 업무/학습 스타일을 파악하고,
         * 생산성 향상을 위한 개인화된 AI 추천을 제공합니다.
         * </p>
         */
        @JsonProperty("todoPersonalization")
        TODO_PERSONALIZATION("할일 기반 추천 동의", false),

        /**
         * 버킷리스트 기반 추천 동의
         *
         * <p>
         * 사용자의 버킷리스트를 분석하여 취미, 관심사, 목표를 파악하고,
         * 라이프스타일 개선을 위한 개인화된 AI 추천을 제공합니다.
         * </p>
         */
        @JsonProperty("bucketPersonalization")
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
                    PRIVACY_POLICY,
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
     * 사용자 동의 생성 정적 팩토리 메서드 (약관 버전 추적 포함)
     *
     * <p>
     * 약관과 연결된 동의를 생성하며, 동의 당시의 약관 버전을 스냅샷으로 저장합니다.
     * 법적 증빙을 위해 어떤 버전의 약관에 동의했는지 추적할 수 있습니다.
     * </p>
     *
     * @param user        동의한 사용자
     * @param consentType 동의 항목 타입
     * @param agreed      동의 여부
     * @param activeTerms 동의 당시 활성 약관 (null 가능)
     * @return 사용자 동의 엔터티
     */
    public static UserConsent createUserConsent(User user, ConsentType consentType, boolean agreed, Terms activeTerms) {
        return UserConsent.builder()
                .user(user)
                .consentType(consentType)
                .agreed(agreed)
                .terms(activeTerms)
                .termsVersionSnapshot(activeTerms != null ? activeTerms.getVersion() : null)
                .build();
    }

    /**
     * 기존 호환성을 위한 팩토리 메서드
     *
     * @param user        동의한 사용자
     * @param consentType 동의 항목 타입
     * @param agreed      동의 여부
     * @return 사용자 동의 엔터티
     * @deprecated 약관 버전 추적을 위해 {@link #createUserConsent(User, ConsentType, boolean, Terms)} 사용 권장
     */
    @Deprecated
    public static UserConsent createUserConsent(User user, ConsentType consentType, boolean agreed) {
        return createUserConsent(user, consentType, agreed, null);
    }

    /**
     * 동의 상태 변경 (약관 버전 업데이트 포함)
     *
     * <p>
     * 동의 상태를 변경하면서 현재 활성 약관 정보도 함께 업데이트합니다.
     * 동의 변경 시점의 약관 버전을 정확히 추적할 수 있습니다.
     * </p>
     *
     * @param agreed      새로운 동의 상태
     * @param activeTerms 현재 활성 약관 (null 가능)
     */
    public void updateAgreement(boolean agreed, Terms activeTerms) {
        this.agreed = agreed;
        this.agreedAt = LocalDateTime.now();

        if (activeTerms != null) {
            this.terms = activeTerms;
            this.termsVersionSnapshot = activeTerms.getVersion();
        }
    }

    /**
     * 동의 상태 변경 (기존 호환성 유지)
     *
     * @param agreed 새로운 동의 상태
     * @deprecated 약관 버전 추적을 위해 {@link #updateAgreement(boolean, Terms)} 사용 권장
     */
    @Deprecated
    public void updateAgreement(boolean agreed) {
        updateAgreement(agreed, null);
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
        updateAgreement(false, this.terms); // 현재 약관 정보 유지하면서 철회
    }

    /**
     * 동의 재동의
     *
     * <p>철회된 선택 동의 항목에 대해 다시 동의할 때 사용합니다.</p>
     */
    public void reConsent() {
        updateAgreement(true, this.terms); // 현재 약관 정보 유지하면서 재동의
    }

    /**
     * 약관 버전 업데이트
     *
     * <p>
     * 새로운 약관 버전이 등록되었을 때 사용자의 동의 정보에 연결된
     * 약관 정보를 업데이트합니다. 동의 상태는 유지됩니다.
     * </p>
     *
     * @param newActiveTerms 새로운 활성 약관
     */
    public void updateTermsReference(Terms newActiveTerms) {
        if (newActiveTerms != null && newActiveTerms.getConsentType() == this.consentType) {
            this.terms = newActiveTerms;
            this.termsVersionSnapshot = newActiveTerms.getVersion();
        }
    }

    /**
     * 동의한 약관 버전 확인
     *
     * @return 동의한 약관의 버전 (스냅샷)
     */
    public String getAgreedTermsVersion() {
        return termsVersionSnapshot;
    }

    /**
     * 최신 약관 버전과 비교
     *
     * @param currentActiveTerms 현재 활성 약관
     * @return 동의한 버전이 최신 버전과 같으면 true
     */
    public boolean isAgreedToLatestVersion(Terms currentActiveTerms) {
        if (currentActiveTerms == null || termsVersionSnapshot == null) {
            return false;
        }
        return termsVersionSnapshot.equals(currentActiveTerms.getVersion());
    }
}