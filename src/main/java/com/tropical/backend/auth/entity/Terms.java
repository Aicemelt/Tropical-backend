package com.tropical.backend.auth.entity;

import com.tropical.backend.auth.entity.UserConsent.ConsentType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * 약관 및 동의서 관리 엔티티
 *
 * <p>
 * 서비스 이용약관, 개인정보처리방침, 각종 동의서의 내용과 버전을 관리합니다.
 * 법무팀에서 약관을 수정하면 새 버전으로 저장되며, 사용자는 항상 최신 버전을 확인할 수 있습니다.
 * 약관 타입과 버전의 조합은 유일해야 하며, 각 타입당 하나의 활성 약관만 존재합니다.
 * </p>
 *
 * <p>주요 기능:</p>
 * <ul>
 *   <li>약관 내용 및 버전 관리</li>
 *   <li>온보딩 시 동의서 제공</li>
 *   <li>마이페이지에서 약관 재확인</li>
 *   <li>법적 요구사항 준수를 위한 이력 관리</li>
 *   <li>사용자 동의와의 연결을 통한 버전 추적</li>
 * </ul>
 *
 * @author 왕택준
 * @version 0.2
 * @since 2025.09.18
 */
@Entity
@Table(name = "terms",
        uniqueConstraints = @UniqueConstraint(
                columnNames = {"consent_type", "version"},
                name = "uk_terms_type_version"
        ))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Terms {

    /**
     * 약관 고유 식별자 (Primary Key)
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 동의 항목 타입
     *
     * <p>UserConsent.ConsentType과 연동되어 각 동의 항목별 약관을 관리합니다.</p>
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "consent_type", nullable = false)
    private ConsentType consentType;

    /**
     * 약관 제목
     */
    @Column(nullable = false, length = 200)
    private String title;

    /**
     * 약관 내용
     *
     * <p>HTML 또는 Markdown 형식으로 저장됩니다.</p>
     */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    /**
     * 약관 버전
     *
     * <p>
     * 의미있는 버전 관리를 위해 "1.0", "1.1", "2.0" 형식을 사용합니다.
     * 법무팀에서 수정 시마다 버전이 증가하며, 동일한 ConsentType 내에서 버전은 고유해야 합니다.
     * </p>
     */
    @Column(nullable = false, length = 10)
    private String version;

    /**
     * 현재 활성 버전 여부
     *
     * <p>
     * 각 ConsentType당 하나의 약관만 active=true 상태를 유지합니다.
     * 사용자에게는 항상 활성 버전의 약관이 제공되며, 새 버전 등록 시 기존 버전은 자동으로 비활성화됩니다.
     * </p>
     */
    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;

    /**
     * 약관 생성 시간
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    /**
     * 약관 생성자 (관리자 ID)
     */
    @Column(name = "created_by")
    private Long createdBy;

    // ===== 비즈니스 메서드 =====

    /**
     * 약관 생성 정적 팩토리 메서드
     *
     * @param consentType 동의 항목 타입
     * @param title       약관 제목
     * @param content     약관 내용
     * @param version     약관 버전
     * @return 약관 엔티티
     */
    public static Terms createTerms(ConsentType consentType, String title, String content, String version) {
        return Terms.builder()
                .consentType(consentType)
                .title(title)
                .content(content)
                .version(version)
                .active(true)
                .build();
    }

    /**
     * 약관 비활성화
     *
     * <p>새로운 버전의 약관이 등록될 때 기존 약관을 비활성화합니다.</p>
     */
    public void deactivate() {
        this.active = false;
    }

    /**
     * 약관 활성화
     *
     * <p>특정 버전을 다시 활성화할 때 사용합니다.</p>
     */
    public void activate() {
        this.active = true;
    }

    /**
     * 활성 상태 확인
     *
     * @return 활성 상태면 true
     */
    public boolean isActive() {
        return Boolean.TRUE.equals(this.active);
    }

    /**
     * 약관 버전 비교
     *
     * @param otherVersion 비교할 버전
     * @return 현재 버전이 더 최신이면 1, 같으면 0, 더 오래된 버전이면 -1
     */
    public int compareVersion(String otherVersion) {
        if (otherVersion == null) return 1;
        return this.version.compareTo(otherVersion);
    }
}