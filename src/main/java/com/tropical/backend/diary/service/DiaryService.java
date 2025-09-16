package com.tropical.backend.diary.service;

import com.tropical.backend.diary.entity.Diary;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * 일기 관리 서비스 인터페이스
 *
 * @author 신동준
 * @version 0.1
 * @since 2025.09.15
 */
public interface DiaryService {

    // === 인증된 사용자용 메서드들 (주로 사용) ===

    /**
     * User ID와 함께 새로운 일기를 생성합니다
     */
    Diary createDiaryWithUserId(Long userId, Diary diary);

    /**
     * 일기 ID와 사용자 ID로 일기를 조회합니다 (권한 검증 포함)
     */
    Diary getDiaryByIdAndUserId(Long diaryId, Long userId);

    /**
     * 사용자 권한을 검증하여 일기를 수정합니다
     */
    Diary updateDiaryByUserId(Long diaryId, Long userId, Diary diary);

    /**
     * 사용자 권한을 검증하여 일기를 삭제합니다
     */
    void deleteDiaryByUserId(Long diaryId, Long userId);

    // === 조회용 메서드들 ===

    /**
     * 사용자의 모든 일기를 조회합니다
     */
    List<Diary> getDiariesByUserId(Long userId);

    /**
     * 특정 날짜의 일기를 조회합니다
     */
    Optional<Diary> getDiaryByDate(Long userId, LocalDate date);

    /**
     * 월별 일기를 조회합니다
     */
    List<Diary> getDiariesByMonth(Long userId, int year, int month);

    /**
     * 감정별 일기를 조회합니다
     */
    List<Diary> getDiariesByEmotion(Long userId, String emotion);

    /**
     * 날씨별 일기를 조회합니다
     */
    List<Diary> getDiariesByWeather(Long userId, String weather);

    /**
     * 특정 날짜에 일기가 존재하는지 확인합니다
     */
    boolean existsByDate(Long userId, LocalDate date);

    // === 기존 메서드들 (호환성 유지용 - 사용 안함) ===

    Diary createDiary(Diary diary);
    Diary getDiaryById(Long diaryId);
    Diary updateDiary(Long diaryId, Diary diary);
    void deleteDiary(Long diaryId);
}
