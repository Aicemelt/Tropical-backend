package com.tropical.backend.diary.entity;

import com.tropical.backend.auth.entity.User;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 사용자 일기 정보 엔티티
 */
@Entity
@Table(name = "diary", indexes = {
    @Index(name = "idx_diary_user_date", columnList = "user_id, diary_date"),
    @Index(name = "idx_diary_date", columnList = "diary_date")
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@ToString(exclude = {"user"})
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class Diary {

    /**
     * 일기 고유 식별자
     *
     * <p>자동 증가하는 Primary Key로 각 일기를 고유하게 식별합니다.</p>
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    /**
     * 일기 소유자
     *
     * <p>이 일기를 작성한 사용자와의 연관관계를 나타냅니다.
     * 지연 로딩(LAZY)을 사용하여 성능을 최적화하고,
     * 사용자별 일기 분리를 통해 개인정보 보안을 보장합니다.</p>
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @NotNull(message = "사용자는 필수입니다")
    private User user;

    /**
     * 일기 제목
     *
     * <p>필수 입력 항목으로, 사용자가 일기를 쉽게 식별할 수 있는 제목입니다.
     * 최대 255자까지 입력 가능하며, 빈 값이나 공백만으로는 저장할 수 없습니다.</p>
     */
    @Column(name = "title", nullable = false, length = 100)
    @NotBlank(message = "일기 제목은 필수입니다")
    @Size(max = 100, message = "일기 제목은 100자를 초과할 수 없습니다")
    private String title;

    /**
     * 일기 내용
     *
     * <p>일기의 핵심인 상세 내용을 저장하는 필수 필드입니다.
     * TEXT 타입으로 긴 내용도 저장 가능하며, 사용자의 하루 일상과
     * 생각을 자유롭게 기록할 수 있습니다.</p>
     */
    @Column(name = "content", columnDefinition = "TEXT")
    @NotBlank(message = "일기 내용은 필수입니다")
    @Size(max = 5000, message = "일기 내용은 5000자를 초과할 수 없습니다")
    private String content;

    /**
     * 일기 작성 날짜
     *
     * <p>일기가 작성된 날짜를 나타내는 필수 필드입니다.
     * 날짜별 일기 조회와 캘린더 통합 기능의 핵심 기준이 됩니다.
     * 하루에 하나의 일기만 작성할 수 있도록 제한될 수 있습니다.</p>
     */
    @Column(name = "diary_date", nullable = false)
    @NotNull(message = "일기 날짜는 필수입니다")
    private LocalDate diaryDate;

    /**
     * 감정 상태
     *
     * <p>일기 작성 당시의 감정 상태를 나타내는 필수 필드입니다.
     * Enum 타입으로 관리되어 데이터 일관성을 보장하며,
     * 감정 분석 및 통계 기능에 활용됩니다.</p>
     *
     * @see Emotion
     */
    @Column(name = "emotion", length = 20)
    @Size(max = 20, message = "감정 정보는 20자를 초과할 수 없습니다")
    private String emotion;

    /**
     * 날씨 정보
     *
     * <p>일기 작성 당일의 날씨 상태를 나타내는 필수 필드입니다.
     * Enum 타입으로 관리되어 데이터 일관성을 보장하며,
     * 감정과 날씨의 상관관계 분석에 활용될 수 있습니다.</p>
     *
     * @see Weather
     */
    @Column(name = "weather", length = 20)
    @Size(max = 20, message = "날씨 정보는 20자를 초과할 수 없습니다")
    private String weather;

    /**
     * 일기 생성 시간
     *
     * <p>일기가 처음 작성된 시간을 자동으로 기록합니다.
     * JPA Auditing 기능을 통해 엔티티 생성 시 자동 설정됩니다.</p>
     */
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * 일기 최종 수정 시간
     *
     * <p>일기 정보가 마지막으로 수정된 시간을 자동으로 기록합니다.
     * JPA Auditing 기능을 통해 엔티티 수정 시 자동 업데이트됩니다.</p>
     */
    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
