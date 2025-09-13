package com.tropical.backend.auth.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * 소셜 로그인 연동 정보를 관리하는 엔터티
 *
 * <p>
 * 각 소셜 플랫폼(카카오, 구글, 네이버)의 계정 정보를 저장합니다.
 * 한 사용자가 여러 소셜 제공자와 동시에 연동할 수 있도록 설계되었습니다.
 * </p>
 *
 * <p>주요 기능:</p>
 * <ul>
 *   <li>소셜 제공자별 사용자 고유 ID 저장</li>
 *   <li>사용자 계정과의 연관관계 관리</li>
 *   <li>동일한 소셜 계정의 중복 연결 방지</li>
 * </ul>
 *
 * @author 왕택준
 * @version 0.1
 * @since 2025.09.13
 */
@Entity
@Table(name = "social_account",
        uniqueConstraints = @UniqueConstraint(
                columnNames = {"provider", "provider_user_id"},
                name = "uk_social_account_provider_user"
        ))
@ToString(exclude = {"user"})
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SocialAccount {

    /**
     * 소셜 계정 연동 정보 고유 식별자 (Primary Key)
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 연결된 사용자 정보
     *
     * <p>
     * 지연 로딩을 사용하여 성능을 최적화합니다.
     * 소셜 계정 정보 조회 시 사용자 정보가 필요한 경우에만 로드됩니다.
     * </p>
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * 소셜 로그인 제공자
     *
     * <p>현재 지원되는 제공자: 카카오, 구글, 네이버</p>
     *
     * @see SocialProvider
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SocialProvider provider;

    /**
     * 소셜 제공자의 사용자 고유 ID
     *
     * <p>
     * 각 소셜 플랫폼에서 제공하는 사용자 고유 식별자:
     * </p>
     * <ul>
     *   <li>카카오: id (숫자)</li>
     *   <li>구글: sub (문자열)</li>
     *   <li>네이버: id (문자열)</li>
     * </ul>
     */
    @Column(name = "provider_user_id", nullable = false)
    private String providerUserId;

    /**
     * 소셜 계정 연결 시간
     *
     * <p>최초 소셜 로그인 시 자동으로 설정됩니다.</p>
     */
    @CreationTimestamp
    @Column(name = "linked_at", nullable = false)
    private LocalDateTime linkedAt;

    /**
     * 소셜 로그인 제공자 열거형
     *
     * <p>
     * Spring Security OAuth2의 registrationId와 매핑되며,
     * 각 제공자별 OAuth2 설정과 연동됩니다.
     * </p>
     */
    public enum SocialProvider {
        /**
         * 카카오 로그인
         */
        KAKAO("kakao"),
        /**
         * 구글 로그인
         */
        GOOGLE("google"),
        /**
         * 네이버 로그인
         */
        NAVER("naver");

        private final String registrationId;

        SocialProvider(String registrationId) {
            this.registrationId = registrationId;
        }

        /**
         * OAuth2 registrationId 반환
         *
         * @return Spring Security OAuth2 설정의 registrationId
         */
        public String getRegistrationId() {
            return registrationId;
        }

        /**
         * Spring Security OAuth2 registrationId로부터 SocialProvider 찾기
         *
         * @param registrationId OAuth2 registrationId
         * @return 매칭되는 SocialProvider
         * @throws IllegalArgumentException 지원하지 않는 registrationId인 경우
         */
        public static SocialProvider fromRegistrationId(String registrationId) {
            for (SocialProvider provider : values()) {
                if (provider.registrationId.equals(registrationId)) {
                    return provider;
                }
            }
            throw new IllegalArgumentException("지원하지 않는 소셜 로그인 제공자입니다: " + registrationId);
        }
    }

    // ===== 비즈니스 메서드 =====

    /**
     * 소셜 계정 생성 정적 팩토리 메서드
     *
     * @param user           연결할 사용자
     * @param provider       소셜 제공자
     * @param providerUserId 소셜 제공자의 사용자 ID
     * @return 소셜 계정 엔터티
     */
    public static SocialAccount createSocialAccount(User user, SocialProvider provider, String providerUserId) {
        return SocialAccount.builder()
                .user(user)
                .provider(provider)
                .providerUserId(providerUserId)
                .build();
    }

    /**
     * 사용자 설정 (연관관계 편의 메서드)
     *
     * <p>
     * 양방향 연관관계를 안전하게 설정합니다.
     * 사용자의 소셜 계정 목록에도 자동으로 추가됩니다.
     * </p>
     *
     * @param user 연결할 사용자
     */
    public void setUser(User user) {
        this.user = user;
        if (user != null && !user.getSocialAccounts().contains(this)) {
            user.getSocialAccounts().add(this);
        }
    }

    /**
     * 소셜 계정 연결 해제
     *
     * <p>
     * 사용자와의 연관관계를 안전하게 해제합니다.
     * 사용자의 소셜 계정 목록에서도 자동으로 제거됩니다.
     * </p>
     */
    public void unlinkFromUser() {
        if (this.user != null) {
            this.user.getSocialAccounts().remove(this);
            this.user = null;
        }
    }
}