package com.tropical.backend.calendar.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Check;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * 공휴일 정보를 관리하는 엔터티.
 *
 * <p>
 * 한국천문연구원 공휴일 API 등 외부 소스에서 수집한 공휴일 데이터를 저장/관리합니다.
 * API 호출을 최소화하기 위해 DB에 캐시 형태로 적재하여 재사용하며,
 * 캘린더 화면/기능에 필요한 공휴일 정보를 제공합니다.
 * </p>
 *
 * <p>주요 기능:</p>
 * <ul>
 *   <li>국경일, 공휴일, 기념일 등 다양한 특일 정보 저장</li>
 *   <li>연휴 기간(시작일~종료일) 표현 및 조회</li>
 *   <li>대체공휴일 구분 관리</li>
 *   <li>외부 API 연동 시 중복 방지/동기화를 위한 식별 정보 보관</li>
 * </ul>
 *
 * <p><b>주의</b>: 월 조회 시 데이터베이스 함수(MONTH 등)를 사용하지 말고,
 * 기간 겹침(Overlap) 조건(예: {@code startDate <= :monthEnd && endDate >= :monthStart})으로 조회하시길 권장합니다.</p>
 *
 * @author  왕택준
 * @version 0.1
 * @since   2025.09.16
 */
@Entity
@Table(
        name = "holiday"
        // TODO: 운영/성능 이슈 발생 시 인덱스 및 유니크 제약을 추가 필요
        // 예시)
        // indexes = {
        //     @Index(name = "idx_holiday_country_year",  columnList = "country_code, year"),
        //     @Index(name = "idx_holiday_date_range",    columnList = "start_date, end_date"),
        //     @Index(name = "idx_holiday_country_start", columnList = "country_code, start_date")
        // },
        // uniqueConstraints = @UniqueConstraint(
        //         name = "uk_holiday_unique",
        //         columnNames = {"country_code", "start_date", "end_date", "name_ko"}
        // )
)
@Check(constraints = "start_date <= end_date")
@Getter
@Setter
@ToString
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class Holiday {

    /**
     * 공휴일 고유 식별자(Primary Key).
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 국가 코드.
     *
     * <p>
     * ISO 3166-1 alpha-2 형식의 국가 코드입니다.
     * 현재는 대한민국("KR")만 지원하지만, 향후 다국가 확장을 고려하여 설계되었습니다.
     * </p>
     */
    @Column(name = "country_code", nullable = false, length = 2)
    @Builder.Default
    private String countryCode = "KR";

    /**
     * 공휴일 시작 날짜.
     *
     * <p>
     * 공휴일이 하루인 경우 시작일과 종료일이 동일합니다.
     * 연휴인 경우 연휴의 첫 날짜가 됩니다.
     * </p>
     */
    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    /**
     * 공휴일 종료 날짜.
     *
     * <p>
     * 공휴일이 하루인 경우 시작일과 동일합니다.
     * 연휴인 경우 연휴의 마지막 날짜가 됩니다(예: 추석 연휴 9/16~9/18).
     * </p>
     */
    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    /**
     * 공휴일 한국어 명칭.
     *
     * <p>예: "설날", "추석", "어린이날", "광복절" 등</p>
     */
    @Column(name = "name_ko", nullable = false, length = 100)
    private String nameKo;

    /**
     * 대체공휴일 여부.
     *
     * <p>
     * 공휴일이 토/일과 겹칠 때 지정되는 대체공휴일인지 여부입니다.
     * {@code true}: 대체공휴일, {@code false}: 원래 공휴일
     * </p>
     */
    @Column(name = "is_substitute", nullable = false)
    @Builder.Default
    private Boolean isSubstitute = false;

    /**
     * 공휴일 연도.
     *
     * <p>
     * 조회/인덱스 최적화를 위한 파생 컬럼입니다.
     * 기본적으로 {@code startDate} 기준으로 설정되며 저장/수정 시 자동 갱신됩니다.
     * </p>
     */
    @Column(nullable = false)
    private Short year;

    /**
     * 공휴일 타입 분류.
     *
     * <p>한국천문연구원 API의 특일(dateKind)을 도메인 친화적으로 매핑합니다.</p>
     *
     * @see HolidayType
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "holiday_type", nullable = false, length = 20)
    @Builder.Default
    private HolidayType holidayType = HolidayType.PUBLIC_HOLIDAY;

    /**
     * 외부 API 고유 식별자.
     *
     * <p>
     * 한국천문연구원 API의 {@code seq} 또는 고유 식별자(필요 시)를 보관하여
     * 중복 방지 및 동기화에 사용합니다.
     * </p>
     */
    @Column(name = "external_id", length = 50)
    private String externalId;

    /**
     * 외부 데이터 소스.
     *
     * <p>예: "KASI_API" (한국천문연구원)</p>
     */
    @Column(name = "external_source", length = 30)
    @Builder.Default
    private String externalSource = "KASI_API";

    /**
     * 데이터 생성 시각.
     *
     * <p>엔터티 최초 저장 시점으로 자동 설정됩니다.</p>
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * 데이터 수정 시각.
     *
     * <p>엔터티 수정 시마다 현재 시각으로 자동 갱신됩니다.</p>
     */
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // =====================================================================
    // JPA 생명주기 콜백
    // =====================================================================

    /**
     * 엔터티 저장/수정 전 콜백.
     *
     * <p>
     * 시작일 기준으로 연도 값을 자동으로 갱신하여 일관성을 유지합니다.
     * </p>
     */
    @PrePersist
    @PreUpdate
    protected void onUpdateYear() {
        if (this.startDate != null) {
            this.year = (short) this.startDate.getYear();
        }
    }

    // =====================================================================
    // 열거형 정의
    // =====================================================================

    /**
     * 공휴일 타입 열거형.
     *
     * <p>한국천문연구원 API의 {@code dateKind}와 개념적으로 매핑됩니다.</p>
     */
    public enum HolidayType {
        /**
         * 국경일 (예: 3·1절, 광복절, 개천절, 한글날).
         */
        NATIONAL_HOLIDAY("국경일"),

        /**
         * 공휴일 (예: 설날, 추석, 어린이날, 부처님오신날).
         */
        PUBLIC_HOLIDAY("공휴일"),

        /**
         * 기념일 (예: 현충일, 제헌절, 4·19혁명 기념일).
         */
        MEMORIAL_DAY("기념일"),

        /**
         * 24절기 (예: 입춘, 춘분, 하지, 동지).
         */
        SEASONAL_DIVISION("24절기"),

        /**
         * 잡절 (예: 단오, 한식, 중양절).
         */
        TRADITIONAL_DAY("잡절"),

        /**
         * 대체공휴일 (주말과 겹칠 때 지정되는 휴일).
         */
        SUBSTITUTE_HOLIDAY("대체공휴일");

        private final String description;

        HolidayType(String description) {
            this.description = description;
        }

        /**
         * 열거형 설명을 반환합니다.
         *
         * @return 공휴일 타입 설명
         */
        public String getDescription() {
            return description;
        }
    }

    // =====================================================================
    // 편의 메서드
    // =====================================================================

    /**
     * 연휴 여부를 반환합니다.
     *
     * <p>시작일과 종료일이 다르면 연휴로 판단합니다.</p>
     *
     * @return 연휴면 {@code true}, 하루짜리 공휴일이면 {@code false}
     */
    public boolean isExtendedHoliday() {
        return !this.startDate.equals(this.endDate);
    }

    /**
     * 특정 날짜가 이 공휴일 기간에 포함되는지 확인합니다.
     *
     * @param date 확인할 날짜 (null 불가)
     * @return 포함되면 {@code true}, 아니면 {@code false}
     * @throws NullPointerException date가 null인 경우
     */
    public boolean containsDate(LocalDate date) {
        Objects.requireNonNull(date, "date must not be null");
        return (date.isEqual(startDate) || date.isAfter(startDate)) &&
               (date.isEqual(endDate)   || date.isBefore(endDate));
    }
}
