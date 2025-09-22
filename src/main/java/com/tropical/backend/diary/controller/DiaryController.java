package com.tropical.backend.diary.controller;

import com.tropical.backend.diary.dto.request.DiaryCreateRequest;
import com.tropical.backend.diary.dto.request.DiaryUpdateRequest;
import com.tropical.backend.diary.dto.response.DiaryResponse;
import com.tropical.backend.diary.entity.Diary;
import com.tropical.backend.diary.service.DiaryService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 일기 관리 컨트롤러
 *
 * @author 신동준
 * @since 2025.09.16
 */
@RestController
@RequestMapping("/api/v1/diaries")
@RequiredArgsConstructor
@Tag(name = "Diary", description = "일기 API")
public class DiaryController {

    private final DiaryService diaryService;

    /**
     * 새 일기 작성
     */
    @PostMapping
    public ResponseEntity<DiaryResponse> createDiary(
            @Valid @RequestBody DiaryCreateRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        // 인증된 사용자 정보 검증
        if (userDetails == null) {
            throw new IllegalArgumentException("인증된 사용자 정보가 필요합니다");
        }

        // UserDetails에서 User ID 추출
        Long userId = Long.valueOf(userDetails.getUsername());

        // User ID를 가지고 Diary 생성
        Diary diary = Diary.builder()
                .title(request.getTitle())
                .content(request.getContent())
                .emotion(request.getEmotion())
                .weather(request.getWeather())
                .diaryDate(request.getDiaryDate())
                .build();

        // 서비스에서 User ID로 Diary 생성
        Diary savedDiary = diaryService.createDiaryWithUserId(userId, diary);
        DiaryResponse response = convertToResponse(savedDiary);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * 사용자의 모든 일기 조회
     */
    @GetMapping
    public ResponseEntity<List<DiaryResponse>> getAllDiaries(
            @AuthenticationPrincipal UserDetails userDetails) {

        if (userDetails == null) {
            throw new IllegalArgumentException("인증된 사용자 정보가 필요합니다");
        }

        Long userId = Long.valueOf(userDetails.getUsername());
        List<Diary> diaries = diaryService.getDiariesByUserId(userId);

        List<DiaryResponse> responses = diaries.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(responses);
    }

    /**
     * 특정 날짜의 일기 조회
     */
    @GetMapping("/date/{date}")
    public ResponseEntity<DiaryResponse> getDiaryByDate(
            @PathVariable LocalDate date,
            @AuthenticationPrincipal UserDetails userDetails) {

        if (userDetails == null) {
            throw new IllegalArgumentException("인증된 사용자 정보가 필요합니다");
        }

        Long userId = Long.valueOf(userDetails.getUsername());
        Optional<Diary> diary = diaryService.getDiaryByDate(userId, date);

        if (diary.isPresent()) {
            DiaryResponse response = convertToResponse(diary.get());
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * 특정 월의 일기 조회
     */
    @GetMapping("/month/{year}/{month}")
    public ResponseEntity<List<DiaryResponse>> getDiariesByMonth(
            @PathVariable int year,
            @PathVariable int month,
            @AuthenticationPrincipal UserDetails userDetails) {

        if (userDetails == null) {
            throw new IllegalArgumentException("인증된 사용자 정보가 필요합니다");
        }

        Long userId = Long.valueOf(userDetails.getUsername());
        List<Diary> diaries = diaryService.getDiariesByMonth(userId, year, month);

        List<DiaryResponse> responses = diaries.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(responses);
    }

    /**
     * 감정별 일기 조회
     */
    @GetMapping("/emotion/{emotion}")
    public ResponseEntity<List<DiaryResponse>> getDiariesByEmotion(
            @PathVariable String emotion,
            @AuthenticationPrincipal UserDetails userDetails) {

        if (userDetails == null) {
            throw new IllegalArgumentException("인증된 사용자 정보가 필요합니다");
        }

        Long userId = Long.valueOf(userDetails.getUsername());
        List<Diary> diaries = diaryService.getDiariesByEmotion(userId, emotion);

        List<DiaryResponse> responses = diaries.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(responses);
    }

    /**
     * 날씨별 일기 조회
     */
    @GetMapping("/weather/{weather}")
    public ResponseEntity<List<DiaryResponse>> getDiariesByWeather(
            @PathVariable String weather,
            @AuthenticationPrincipal UserDetails userDetails) {

        if (userDetails == null) {
            throw new IllegalArgumentException("인증된 사용자 정보가 필요합니다");
        }

        Long userId = Long.valueOf(userDetails.getUsername());
        List<Diary> diaries = diaryService.getDiariesByWeather(userId, weather);

        List<DiaryResponse> responses = diaries.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(responses);
    }

    /**
     * 특정 날짜에 일기 존재 여부 확인
     */
    @GetMapping("/exists/{date}")
    public ResponseEntity<Boolean> checkDiaryExists(
            @PathVariable LocalDate date,
            @AuthenticationPrincipal UserDetails userDetails) {

        if (userDetails == null) {
            throw new IllegalArgumentException("인증된 사용자 정보가 필요합니다");
        }

        Long userId = Long.valueOf(userDetails.getUsername());
        boolean exists = diaryService.existsByDate(userId, date);

        return ResponseEntity.ok(exists);
    }

    /**
     * 특정 일기 상세 조회
     */
    @GetMapping("/{diaryId}")
    public ResponseEntity<DiaryResponse> getDiary(
            @PathVariable Long diaryId,
            @AuthenticationPrincipal UserDetails userDetails) {

        if (userDetails == null) {
            throw new IllegalArgumentException("인증된 사용자 정보가 필요합니다");
        }

        Long userId = Long.valueOf(userDetails.getUsername());
        Diary diary = diaryService.getDiaryByIdAndUserId(diaryId, userId);

        DiaryResponse response = convertToResponse(diary);
        return ResponseEntity.ok(response);
    }

    /**
     * 특정 일기 수정
     */
    @PutMapping("/{diaryId}")
    public ResponseEntity<DiaryResponse> updateDiary(
            @PathVariable Long diaryId,
            @Valid @RequestBody DiaryUpdateRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        if (userDetails == null) {
            throw new IllegalArgumentException("인증된 사용자 정보가 필요합니다");
        }

        Long userId = Long.valueOf(userDetails.getUsername());

        Diary updateData = Diary.builder()
                .title(request.getTitle())
                .content(request.getContent())
                .emotion(request.getEmotion())
                .weather(request.getWeather())
                .diaryDate(request.getDiaryDate())
                .build();

        Diary updatedDiary = diaryService.updateDiaryByUserId(diaryId, userId, updateData);
        DiaryResponse response = convertToResponse(updatedDiary);

        return ResponseEntity.ok(response);
    }

    /**
     * 특정 일기 삭제
     */
    @DeleteMapping("/{diaryId}")
    public ResponseEntity<Void> deleteDiary(
            @PathVariable Long diaryId,
            @AuthenticationPrincipal UserDetails userDetails) {

        if (userDetails == null) {
            throw new IllegalArgumentException("인증된 사용자 정보가 필요합니다");
        }

        Long userId = Long.valueOf(userDetails.getUsername());
        diaryService.deleteDiaryByUserId(diaryId, userId);

        return ResponseEntity.noContent().build();
    }

    /**
     * Diary 엔티티를 DiaryResponse DTO로 변환
     */
    private DiaryResponse convertToResponse(Diary diary) {
        return DiaryResponse.builder()
                .diaryId(diary.getId())
                .title(diary.getTitle())
                .content(diary.getContent())
                .emotion(diary.getEmotion())
                .weather(diary.getWeather())
                .diaryDate(diary.getDiaryDate())
                .build();
    }
}
