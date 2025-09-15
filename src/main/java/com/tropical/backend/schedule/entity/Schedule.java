package com.tropical.backend.schedule.entity;

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
import java.time.LocalTime;

/**
 * 사용자 일정 정보 엔티티
 *
 * <p>
 * 사용자가 등록한 개인 일정 정보를 관리하는 핵심 엔티티입니다.
 * 일정의 제목, 메모, 날짜, 시간, 장소 등의 상세 정보와 완료 상태를 포함하며,
 * 월별 캘린더 뷰와 일정 관리 기능의 기반이 됩니다.
 * </p>
 *
 * @author 신동준
 * @version 0.1
 * @since 2025.09.14
 */
@Entity
@Table(name = "schedule", indexes = {
    @Index(name = "idx_schedule_user_date", columnList = "user_id, schedule_date"),
    @Index(name = "idx_schedule_date", columnList = "schedule_date"),
    @Index(name = "idx_schedule_completed", columnList = "is_completed")
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@ToString(exclude = {"user"})
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class Schedule {

    /**
     * 일정 고유 식별자
     *
     * <p>자동 증가하는 Primary Key로 각 일정을 고유하게 식별합니다.</p>
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    /**
     * 일정 제목
     *
     * <p>필수 입력 항목으로, 사용자가 일정을 쉽게 식별할 수 있는 제목입니다.
     * 최대 100자까지 입력 가능하며, 빈 값이나 공백만으로는 저장할 수 없습니다.</p>
     */
    @NotBlank
    @Size(max = 100)
    @Column(name = "title", nullable = false, length = 100)
    private String title;

    /**
     * 일정 상세 메모
     *
     * <p>일정에 대한 상세한 설명이나 추가 정보를 저장하는 선택적 필드입니다.
     * 최대 1000자까지 입력 가능하며, 긴 내용도 저장 가능합니다.</p>
     */
    @Lob
    @Size(max = 1000)
    @Column(name = "memo", columnDefinition = "TEXT")
    private String memo;

    /**
     * 일정 날짜
     *
     * <p>일정이 예정된 날짜입니다. 필수 입력 항목으로,
     * 캘린더 뷰와 월별 조회 기능의 핵심 기준이 됩니다.</p>
     */
    @NotNull
    @Column(name = "schedule_date", nullable = false)
    private LocalDate scheduleDate;

    /**
     * 일정 시작 시간
     *
     * <p>일정의 시작 시간을 나타내는 선택적 필드입니다.
     * 시간이 정해지지 않은 하루 종일 일정의 경우 null일 수 있습니다.</p>
     */
    @Column(name = "start_time")
    private LocalTime startTime;

    /**
     * 일정 종료 시간
     *
     * <p>일정의 종료 시간을 나타내는 선택적 필드입니다.
     * 시작 시간과 함께 일정의 소요 시간을 계산하는 데 사용됩니다.</p>
     */
    @Column(name = "end_time")
    private LocalTime endTime;

    /**
     * 일정 장소
     *
     * <p>일정이 진행될 장소나 위치 정보를 저장하는 선택적 필드입니다.
     * 최대 200자까지 입력 가능하며, 구체적인 장소 정보를 제공합니다.</p>
     */
    @Size(max = 200)
    @Column(name = "location", length = 200)
    private String location;

    /**
     * 참석자 정보
     *
     * <p>일정에 참석하는 사람들의 정보를 저장하는 선택적 필드입니다.
     * 최대 500자까지 입력 가능하며, 여러 참석자는 구분자로 나누어 저장됩니다.</p>
     */
    @Size(max = 500)
    @Column(name = "attendees", length = 500)
    private String attendees;

    /**
     * 일정 완료 상태
     *
     * <p>일정의 완료 여부를 나타내는 필드입니다.
     * 기본값은 false(미완료)이며, 사용자가 일정 완료 처리 시 true로 변경됩니다.</p>
     */
    @Column(name = "is_completed", nullable = false)
    @Builder.Default
    private Boolean isCompleted = false;

    /**
     * 일정 생성 시간
     *
     * <p>일정이 처음 생성된 시간을 자동으로 기록합니다.
     * JPA Auditing 기능을 통해 엔티티 생성 시 자동 설정됩니다.</p>
     */
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * 일정 최종 수정 시간
     *
     * <p>일정 정보가 마지막으로 수정된 시간을 자동으로 기록합니다.
     * JPA Auditing 기능을 통해 엔티티 수정 시 자동 업데이트됩니다.</p>
     */
    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * 일정 소유자
     *
     * <p>이 일정을 생성한 사용자와의 연관관계를 나타냅니다.
     * 지연 로딩(LAZY)을 사용하여 성능을 최적화하고,
     * 사용자별 일정 분리를 통해 데이터 보안을 보장합니다.</p>
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @NotNull(message = "사용자는 필수입니다")
    private User user;
}