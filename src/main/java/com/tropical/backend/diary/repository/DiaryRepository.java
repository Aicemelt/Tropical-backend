package com.tropical.backend.diary.repository;

import com.tropical.backend.diary.entity.Diary;
import com.tropical.backend.diary.entity.Emotion;
import com.tropical.backend.diary.entity.Weather;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * 일기 정보 데이터 액세스 리포지토리
 *
 * <p>
 * 사용자 일기 정보에 대한 데이터베이스 접근을 담당하는 리포지토리 인터페이스입니다.
 * Spring Data JPA를 기반으로 하며, 일기의 CRUD 작업과 다양한 조회 조건을 지원합니다.
 * 감정 및 날씨 기반 필터링, 날짜 기반 검색 등 일기 특화 기능들이 포함되어 있습니다.
 * </p>
 *
 * <p>주요 제공 기능:</p>
 * <ul>
 *   <li>사용자별 일기 조회 (최신순 정렬)</li>
 *   <li>날짜 기반 일기 조회 (특정 날짜, 날짜 범위, 월별)</li>
 *   <li>감정 상태별 일기 필터링</li>
 *   <li>날씨별 일기 필터링</li>
 *   <li>일기 존재 여부 확인 (중복 방지)</li>
 *   <li>특정 기간 이후 일기 조회</li>
 * </ul>
 *
 * @author 신동준
 * @version 0.1
 * @since 2025.09.14
 */
@Repository
public interface DiaryRepository extends JpaRepository<Diary, Long> {

    /**
     * 사용자별 일기 조회 (최신순 정렬)
     *
     * <p>
     * 특정 사용자의 모든 일기를 조회하여 작성 날짜 내림차순으로 정렬합니다.
     * 사용자의 일기 목록을 최신순으로 표시할 때 사용됩니다.
     * </p>
     *
     * @param userId 조회할 사용자의 ID
     * @return 해당 사용자의 모든 일기 목록 (최신순 정렬)
     */
    List<Diary> findByUserIdOrderByDiaryDateDesc(Long userId);

    /**
     * 특정 날짜의 일기 조회
     *
     * <p>
     * 사용자의 특정 날짜에 작성된 일기를 조회합니다.
     * 일반적으로 하루에 하나의 일기만 작성되므로 Optional로 반환됩니다.
     * 일별 일기 상세 보기나 특정 날짜 일기 수정 시 사용됩니다.
     * </p>
     *
     * @param userId    조회할 사용자의 ID
     * @param diaryDate 조회할 특정 날짜
     * @return 해당 날짜의 일기 (Optional)
     */
    Optional<Diary> findByUserIdAndDiaryDate(Long userId, LocalDate diaryDate);

    /**
     * 날짜 범위별 일기 조회
     *
     * <p>
     * 지정된 시작 날짜부터 종료 날짜까지의 모든 일기를 조회합니다.
     * 주간 일기 목록, 월간 일기 목록 등 기간별 일기 표시에 활용됩니다.
     * </p>
     *
     * @param userId    조회할 사용자의 ID
     * @param startDate 조회 시작 날짜 (포함)
     * @param endDate   조회 종료 날짜 (포함)
     * @return 해당 기간의 일기 목록
     */
    List<Diary> findByUserIdAndDiaryDateBetween(Long userId, LocalDate startDate, LocalDate endDate);

    /**
     * 감정별 일기 조회
     *
     * <p>
     * 특정 감정 상태로 작성된 모든 일기를 조회합니다.
     * 감정 분석이나 특정 감정의 일기만 모아보는 기능에 활용됩니다.
     * </p>
     *
     * @param userId  조회할 사용자의 ID
     * @param emotion 조회할 감정 상태
     * @return 해당 감정으로 작성된 일기 목록
     * @see Emotion
     */
    List<Diary> findByUserIdAndEmotion(Long userId, Emotion emotion);

    /**
     * 날씨별 일기 조회
     *
     * <p>
     * 특정 날씨 상태에서 작성된 모든 일기를 조회합니다.
     * 날씨와 감정의 상관관계 분석이나 특정 날씨의 일기만 모아보는 기능에 활용됩니다.
     * </p>
     *
     * @param userId  조회할 사용자의 ID
     * @param weather 조회할 날씨 상태
     * @return 해당 날씨에서 작성된 일기 목록
     * @see Weather
     */
    List<Diary> findByUserIdAndWeather(Long userId, Weather weather);

    /**
     * 월별 일기 조회 (커스텀 쿼리)
     *
     * <p>
     * 특정 연도와 월에 해당하는 모든 일기를 조회합니다.
     * 캘린더 월 뷰에서 해당 월의 모든 일기를 표시할 때 사용되며,
     * 날짜 내림차순으로 정렬되어 반환됩니다.
     * </p>
     *
     * @param userId 조회할 사용자의 ID
     * @param year   조회할 연도 (예: 2025)
     * @param month  조회할 월 (1-12)
     * @return 해당 월의 일기 목록 (최신순 정렬)
     */
    @Query("SELECT d FROM Diary d WHERE d.user.id = :userId " +
           "AND YEAR(d.diaryDate) = :year AND MONTH(d.diaryDate) = :month " +
           "ORDER BY d.diaryDate DESC")
    List<Diary> findMonthlyDiaries(@Param("userId") Long userId,
                                  @Param("year") int year,
                                  @Param("month") int month);

    /**
     * 특정 날짜 이후 일기 조회
     *
     * <p>
     * 지정된 날짜 이후에 작성된 모든 일기를 조회합니다.
     * 최근 일기 조회나 특정 시점 이후의 일기 분석에 활용되며,
     * 날짜 내림차순으로 정렬되어 반환됩니다.
     * </p>
     *
     * @param userId    조회할 사용자의 ID
     * @param startDate 기준 날짜 (이 날짜 이후의 일기 조회)
     * @return 기준 날짜 이후의 일기 목록 (최신순 정렬)
     */
    @Query("SELECT d FROM Diary d WHERE d.user.id = :userId " +
           "AND d.diaryDate >= :startDate " +
           "ORDER BY d.diaryDate DESC")
    List<Diary> findDiariesAfterDate(@Param("userId") Long userId,
                                    @Param("startDate") LocalDate startDate);

    /**
     * 사용자의 특정 날짜 일기 존재 여부 확인
     *
     * <p>
     * 특정 날짜에 이미 일기가 작성되어 있는지 확인합니다.
     * 하루에 하나의 일기만 작성하도록 제한하는 비즈니스 로직에서 사용되며,
     * 성능상 count 쿼리보다 exists 쿼리가 더 효율적입니다.
     * </p>
     *
     * @param userId    확인할 사용자의 ID
     * @param diaryDate 확인할 날짜
     * @return 해당 날짜에 일기가 존재하면 true, 없으면 false
     */
    boolean existsByUserIdAndDiaryDate(Long userId, LocalDate diaryDate);
}