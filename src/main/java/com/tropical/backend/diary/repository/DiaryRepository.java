package com.tropical.backend.diary.repository;

import com.tropical.backend.diary.entity.Diary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * 일기 데이터 접근 리포지토리
 *
 * <p>
 * Diary 엔티티에 대한 데이터베이스 접근을 담당하는 리포지토리입니다.
 * 기본 CRUD 기능과 함께 날짜별, 감정별, 날씨별 조회 기능을 제공합니다.
 * </p>
 *
 * @author 신동준
 * @version 0.1
 * @since 2025.09.15
 */
@Repository
public interface DiaryRepository extends JpaRepository<Diary, Long> {

    /**
     * 사용자의 모든 일기를 조회합니다
     *
     * @param userId 사용자 ID
     * @return 사용자의 일기 목록
     */
    List<Diary> findByUser_IdOrderByDiaryDateDesc(Long userId);

    /**
     * 특정 날짜의 일기를 조회합니다
     *
     * @param userId 사용자 ID
     * @param diaryDate 조회할 날짜
     * @return 해당 날짜의 일기
     */
    Optional<Diary> findByUser_IdAndDiaryDate(Long userId, LocalDate diaryDate);

    /**
     * 월별 일기를 조회합니다
     *
     * @param userId 사용자 ID
     * @param startDate 월 시작일
     * @param endDate 월 종료일
     * @return 해당 월의 일기 목록
     */
    @Query("SELECT d FROM Diary d WHERE d.user.id = :userId AND d.diaryDate BETWEEN :startDate AND :endDate ORDER BY d.diaryDate DESC")
    List<Diary> findByUserIdAndDiaryDateBetween(@Param("userId") Long userId, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    /**
     * 감정별 일기를 조회합니다
     *
     * @param userId 사용자 ID
     * @param emotion 감정 상태
     * @return 해당 감정의 일기 목록
     */
    List<Diary> findByUser_IdAndEmotionOrderByDiaryDateDesc(Long userId, String emotion);

    /**
     * 날씨별 일기를 조회합니다
     *
     * @param userId 사용자 ID
     * @param weather 날씨 상태
     * @return 해당 날씨의 일기 목록
     */
    List<Diary> findByUser_IdAndWeatherOrderByDiaryDateDesc(Long userId, String weather);

    /**
     * 사용자의 특정 날짜에 일기가 존재하는지 확인합니다
     *
     * @param userId 사용자 ID
     * @param diaryDate 확인할 날짜
     * @return 존재 여부
     */
    boolean existsByUser_IdAndDiaryDate(Long userId, LocalDate diaryDate);
}