package com.tropical.backend.calendar.repository;

import com.tropical.backend.calendar.entity.Holiday;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

/**
 * 공휴일 데이터 접근 Repository
 *
 * <p>
 * 대한민국 공휴일/연휴 데이터를 조회하기 위한 데이터 접근 계층입니다.
 * 월/기간 조회는 데이터베이스 함수(MONTH 등) 대신 <b>기간 겹침</b> 조건을 사용합니다.
 * </p>
 *
 * <p>주요 기능:</p>
 * <ul>
 *   <li>기간 겹침(월/주 범위) 공휴일 조회</li>
 *   <li>특정 날짜가 포함된 공휴일 조회</li>
 *   <li>연도별 공휴일 조회</li>
 *   <li>대체공휴일만 조회</li>
 *   <li>중복 저장 방지를 위한 존재 여부 확인</li>
 * </ul>
 *
 * @author 왕택준
 * @version 0.1
 * @since 2025.09.16
 */
@Repository
public interface HolidayRepository extends JpaRepository<Holiday, Long> {

    /**
     * 기간 겹침(Overlap) 조건으로 공휴일을 조회합니다.
     *
     * <p>{@code startDate <= :monthEnd AND endDate >= :monthStart}</p>
     *
     * @param countryCode 국가 코드(예: "KR")
     * @param monthStart  조회 기간의 시작일(포함)
     * @param monthEnd    조회 기간의 종료일(포함)
     * @return 기간과 겹치는 공휴일 목록(시작일 오름차순)
     */
    @Query("SELECT h FROM Holiday h " +
           "WHERE h.countryCode = :countryCode " +
           "AND h.startDate <= :monthEnd " +
           "AND h.endDate >= :monthStart " +
           "ORDER BY h.startDate")
    List<Holiday> findOverlappingHolidays(
            @Param("countryCode") String countryCode,
            @Param("monthStart") LocalDate monthStart,
            @Param("monthEnd") LocalDate monthEnd
    );

    /**
     * 특정 날짜가 포함된 공휴일을 조회합니다.
     *
     * @param countryCode 국가 코드
     * @param date        조회 날짜
     * @return 해당 날짜를 포함하는 공휴일 목록(시작일 오름차순)
     */
    @Query("SELECT h FROM Holiday h " +
           "WHERE h.countryCode = :countryCode " +
           "AND :date BETWEEN h.startDate AND h.endDate " +
           "ORDER BY h.startDate")
    List<Holiday> findHolidaysContainingDate(
            @Param("countryCode") String countryCode,
            @Param("date") LocalDate date
    );


    /**
     * 특정 날짜에 해당하는 공휴일 중 지정된 타입만 조회합니다.
     *
     * @param countryCode 국가 코드
     * @param date        조회 날짜
     * @param types       포함할 공휴일 타입 목록 (예: PUBLIC_HOLIDAY, NATIONAL_HOLIDAY, SUBSTITUTE_HOLIDAY)
     * @return 지정된 날짜와 타입 조건을 만족하는 공휴일 목록(시작일 오름차순)
     */
    @Query("""
            select h from Holiday h
            where h.countryCode = :countryCode
              and :date between h.startDate and h.endDate
              and h.holidayType in :types
            order by h.startDate
            """)
    List<Holiday> findOnDateByTypes(
            @Param("countryCode") String countryCode,
            @Param("date") LocalDate date,
            @Param("types") Collection<Holiday.HolidayType> types
    );

    /**
     * 연도별 공휴일을 조회합니다.
     *
     * @param countryCode 국가 코드
     * @param year        연도
     * @return 해당 연도의 공휴일 목록(시작일 오름차순)
     */
    List<Holiday> findByCountryCodeAndYearOrderByStartDate(String countryCode, Short year);

    /**
     * 대체공휴일만 조회합니다.
     *
     * @param countryCode 국가 코드
     * @return 대체공휴일 목록(시작일 오름차순)
     */
    List<Holiday> findByCountryCodeAndIsSubstituteTrueOrderByStartDate(String countryCode);

    /**
     * 동일 공휴일 존재 여부를 확인합니다.
     *
     * @param countryCode 국가 코드
     * @param startDate   시작일
     * @param endDate     종료일
     * @param nameKo      공휴일명(한글)
     * @return 존재하면 {@code true}
     */
    boolean existsByCountryCodeAndStartDateAndEndDateAndNameKo(
            String countryCode,
            LocalDate startDate,
            LocalDate endDate,
            String nameKo
    );
}
