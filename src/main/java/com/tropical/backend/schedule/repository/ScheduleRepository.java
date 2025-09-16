package com.tropical.backend.schedule.repository;

import com.tropical.backend.schedule.entity.Schedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

/**
 * 일정 정보 데이터 액세스 리포지토리
 *
 * <p>
 * 사용자 일정 정보에 대한 데이터베이스 접근을 담당하는 리포지토리 인터페이스입니다.
 * Spring Data JPA를 기반으로 하며, 일정의 CRUD 작업과 다양한 조회 조건을 지원합니다.
 * 성능 최적화를 위해 인덱스를 활용한 쿼리 메서드들이 포함되어 있습니다.
 * </p>
 *
 * <p>주요 제공 기능:</p>
 * <ul>
 *   <li>사용자별 일정 조회 (날짜/시간 순 정렬)</li>
 *   <li>날짜 기반 일정 조회 (특정 날짜, 날짜 범위)</li>
 *   <li>월별 일정 조회 (캘린더 뷰 지원)</li>
 *   <li>완료 상태별 일정 필터링</li>
 *   <li>기한 경과 일정 조회 (알림 기능 지원)</li>
 * </ul>
 *
 * @author 신동준
 * @version 0.1
 * @since 2025.09.14
 */
@Repository
public interface ScheduleRepository extends JpaRepository<Schedule, Long> {

    /**
     * 사용자별 일정 조회 (날짜/시간 순 정렬)
     *
     * <p>
     * 특정 사용자의 모든 일정을 조회하여 날짜 오름차순, 시작 시간 오름차순으로 정렬합니다.
     * 사용자의 전체 일정 목록을 시간순으로 표시할 때 사용됩니다.
     * </p>
     *
     * @param userId 조회할 사용자의 ID
     * @return 해당 사용자의 모든 일정 목록 (날짜/시간 순 정렬)
     */
    List<Schedule> findByUserIdOrderByScheduleDateAscStartTimeAsc(Long userId);

    /**
     * 특정 날짜의 일정 조회
     *
     * <p>
     * 사용자의 특정 날짜에 등록된 모든 일정을 조회합니다.
     * 일별 일정 상세 보기나 특정 날짜 클릭 시 사용됩니다.
     * </p>
     *
     * @param userId       조회할 사용자의 ID
     * @param scheduleDate 조회할 특정 날짜
     * @return 해당 날짜의 일정 목록
     */
    List<Schedule> findByUserIdAndScheduleDate(Long userId, LocalDate scheduleDate);

    /**
     * 날짜 범위별 일정 조회
     *
     * <p>
     * 지정된 시작 날짜부터 종료 날짜까지의 모든 일정을 조회합니다.
     * 주간 뷰, 월간 뷰 등 기간별 일정 표시에 활용됩니다.
     * </p>
     *
     * @param userId    조회할 사용자의 ID
     * @param startDate 조회 시작 날짜 (포함)
     * @param endDate   조회 종료 날짜 (포함)
     * @return 해당 기간의 일정 목록
     */
    List<Schedule> findByUserIdAndScheduleDateBetween(Long userId, LocalDate startDate, LocalDate endDate);

    /**
     * 완료 상태별 일정 조회
     *
     * <p>
     * 완료된 일정 또는 미완료 일정만을 필터링하여 조회합니다.
     * 할 일 목록 관리나 완료된 일정 통계에 활용됩니다.
     * </p>
     *
     * @param userId      조회할 사용자의 ID
     * @param isCompleted 완료 상태 (true: 완료, false: 미완료)
     * @return 해당 완료 상태의 일정 목록
     */
    List<Schedule> findByUserIdAndIsCompleted(Long userId, Boolean isCompleted);

    /**
     * 월별 일정 조회 (커스텀 쿼리)
     *
     * <p>
     * 특정 연도와 월에 해당하는 모든 일정을 조회합니다.
     * 캘린더 월 뷰에서 해당 월의 모든 일정을 표시할 때 사용되며,
     * 날짜와 시간 순으로 정렬되어 반환됩니다.
     * </p>
     *
     * @param userId 조회할 사용자의 ID
     * @param year   조회할 연도 (예: 2025)
     * @param month  조회할 월 (1-12)
     * @return 해당 월의 일정 목록 (날짜/시간 순 정렬)
     */
    @Query("SELECT s FROM Schedule s WHERE s.user.id = :userId " +
           "AND YEAR(s.scheduleDate) = :year AND MONTH(s.scheduleDate) = :month " +
           "ORDER BY s.scheduleDate ASC, s.startTime ASC")
    List<Schedule> findMonthlySchedules(@Param("userId") Long userId,
                                       @Param("year") int year,
                                       @Param("month") int month);

    /**
     * 기한 경과 미완료 일정 조회
     *
     * <p>
     * 지정된 날짜 이전의 미완료 일정들을 조회합니다.
     * 알림 기능이나 놓친 일정 확인에 활용되며,
     * 날짜 순으로 정렬되어 반환됩니다.
     * </p>
     *
     * @param userId  조회할 사용자의 ID
     * @param endDate 기준 날짜 (이 날짜 이전의 미완료 일정 조회)
     * @return 기한이 경과된 미완료 일정 목록 (날짜 순 정렬)
     */
    @Query("SELECT s FROM Schedule s WHERE s.user.id = :userId " +
           "AND s.scheduleDate <= :endDate AND s.isCompleted = false " +
           "ORDER BY s.scheduleDate ASC")
    List<Schedule> findOverdueSchedules(@Param("userId") Long userId,
                                       @Param("endDate") LocalDate endDate);
}
