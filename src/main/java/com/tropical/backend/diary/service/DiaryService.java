package com.tropical.backend.diary.service;

import com.tropical.backend.diary.entity.Diary;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * 일기 관리 서비스 인터페이스
 *
 * <p>
 * Diary 엔티티에 대한 비즈니스 로직을 정의하는 서비스 계층 인터페이스입니다.
 * CRUD 기본 기능과 날짜별 조회, 감정/날씨별 조회 등의 비즈니스 기능을 제공합니다.
 * </p>
 *
 * @author 신동준
 * @version 0.1
 * @since 2025.09.15
 */
public interface DiaryService {

    /**
     * 새로운 일기를 생성합니다
     *
     * @param diary 생성할 일기 정보
     * @return 생성된 일기
     */
    Diary createDiary(Diary diary);

    /**
     * ID로 일기를 조회합니다
     *
     * @param diaryId 조회할 일기 ID
     * @return 조회된 일기
     */
    Diary getDiaryById(Long diaryId);

    /**
     * 사용자의 모든 일기를 조회합니다
     *
     * @param userId 사용자 ID
     * @return 사용자의 일기 목록
     */
    List<Diary> getDiariesByUserId(Long userId);

    /**
     * 특정 날짜의 일기를 조회합니다
     *
     * @param userId 사용자 ID
     * @param date 조회할 날짜
     * @return 해당 날짜의 일기 (Optional)
     */
    Optional<Diary> getDiaryByDate(Long userId, LocalDate date);

    /**
     * 월별 일기를 조회합니다
     *
     * @param userId 사용자 ID
     * @param year 연도
     * @param month 월
     * @return 해당 월의 일기 목록
     */
    List<Diary> getDiariesByMonth(Long userId, int year, int month);

    /**
     * 감정별 일기를 조회합니다
     *
     * @param userId 사용자 ID
     * @param emotion 감정 상태
     * @return 해당 감정의 일기 목록
     */
    List<Diary> getDiariesByEmotion(Long userId, String emotion);

    /**
     * 날씨별 일기를 조회합니다
     *
     * @param userId 사용자 ID
     * @param weather 날씨 상태
     * @return 해당 날씨의 일기 목록
     */
    List<Diary> getDiariesByWeather(Long userId, String weather);

    /**
     * 일기 정보를 수정합니다
     *
     * @param diaryId 수정할 일기 ID
     * @param diary 수정할 일기 정보
     * @return 수정된 일기
     */
    Diary updateDiary(Long diaryId, Diary diary);

    /**
     * 일기를 삭제합니다
     *
     * @param diaryId 삭제할 일기 ID
     */
    void deleteDiary(Long diaryId);

    /**
     * 특정 날짜에 일기가 존재하는지 확인합니다
     *
     * @param userId 사용자 ID
     * @param date 확인할 날짜
     * @return 존재 여부
     */
    boolean existsByDate(Long userId, LocalDate date);
}
