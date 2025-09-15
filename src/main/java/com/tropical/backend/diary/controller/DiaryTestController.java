package com.tropical.backend.diary.controller;

import com.tropical.backend.diary.entity.Diary;
import com.tropical.backend.diary.service.DiaryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * 일기 관리 컨트롤러 (임시 테스트용)
 *
 * <p>
 * DiaryService를 테스트하기 위한 임시 컨트롤러입니다.
 * 기본적인 CRUD 기능과 조회 기능을 제공하여 
 * 서비스 계층의 동작을 확인할 수 있습니다.
 * </p>
 *
 * @author 신동준
 * @version 0.1
 * @since 2025.09.15
 */
@RestController
@RequestMapping("/api/test/diaries")
@RequiredArgsConstructor
@Slf4j
public class DiaryTestController {

    private final DiaryService diaryService;

    /**
     * 사용자의 모든 일기 조회
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Diary>> getDiariesByUserId(@PathVariable Long userId) {
        log.info("사용자 일기 조회 API 호출: userId = {}", userId);
        List<Diary> diaries = diaryService.getDiariesByUserId(userId);
        return ResponseEntity.ok(diaries);
    }

    /**
     * 특정 날짜의 일기 조회
     */
    @GetMapping("/user/{userId}/date/{date}")
    public ResponseEntity<Diary> getDiaryByDate(
            @PathVariable Long userId,
            @PathVariable String date) {
        log.info("날짜별 일기 조회 API 호출: userId = {}, date = {}", userId, date);
        LocalDate localDate = LocalDate.parse(date);
        Optional<Diary> diary = diaryService.getDiaryByDate(userId, localDate);
        
        if (diary.isPresent()) {
            return ResponseEntity.ok(diary.get());
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * 월별 일기 조회
     */
    @GetMapping("/user/{userId}/month/{year}/{month}")
    public ResponseEntity<List<Diary>> getDiariesByMonth(
            @PathVariable Long userId,
            @PathVariable int year,
            @PathVariable int month) {
        log.info("월별 일기 조회 API 호출: userId = {}, year = {}, month = {}", userId, year, month);
        List<Diary> diaries = diaryService.getDiariesByMonth(userId, year, month);
        return ResponseEntity.ok(diaries);
    }

    /**
     * 감정별 일기 조회
     */
    @GetMapping("/user/{userId}/emotion/{emotion}")
    public ResponseEntity<List<Diary>> getDiariesByEmotion(
            @PathVariable Long userId,
            @PathVariable String emotion) {
        log.info("감정별 일기 조회 API 호출: userId = {}, emotion = {}", userId, emotion);
        List<Diary> diaries = diaryService.getDiariesByEmotion(userId, emotion);
        return ResponseEntity.ok(diaries);
    }

    /**
     * 날씨별 일기 조회
     */
    @GetMapping("/user/{userId}/weather/{weather}")
    public ResponseEntity<List<Diary>> getDiariesByWeather(
            @PathVariable Long userId,
            @PathVariable String weather) {
        log.info("날씨별 일기 조회 API 호출: userId = {}, weather = {}", userId, weather);
        List<Diary> diaries = diaryService.getDiariesByWeather(userId, weather);
        return ResponseEntity.ok(diaries);
    }

    /**
     * 일기 상세 조회
     */
    @GetMapping("/{diaryId}")
    public ResponseEntity<Diary> getDiaryById(@PathVariable Long diaryId) {
        log.info("일기 상세 조회 API 호출: diaryId = {}", diaryId);
        Diary diary = diaryService.getDiaryById(diaryId);
        return ResponseEntity.ok(diary);
    }

    /**
     * 특정 날짜 일기 존재 여부 확인
     */
    @GetMapping("/user/{userId}/exists/{date}")
    public ResponseEntity<Boolean> existsByDate(
            @PathVariable Long userId,
            @PathVariable String date) {
        log.info("일기 존재 확인 API 호출: userId = {}, date = {}", userId, date);
        LocalDate localDate = LocalDate.parse(date);
        boolean exists = diaryService.existsByDate(userId, localDate);
        return ResponseEntity.ok(exists);
    }

    /**
     * 테스트용 응답 확인
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Diary API is working!");
    }
}