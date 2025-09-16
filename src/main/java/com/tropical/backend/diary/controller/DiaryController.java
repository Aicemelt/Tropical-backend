package com.tropical.backend.diary.controller;

import com.tropical.backend.auth.entity.User;
import com.tropical.backend.diary.dto.request.DiaryCreateRequest;
import com.tropical.backend.diary.dto.request.DiaryUpdateRequest;
import com.tropical.backend.diary.dto.response.DiaryResponse;
import com.tropical.backend.diary.entity.Diary;
import com.tropical.backend.diary.service.DiaryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * 일기 관리 컨트롤러
 *
 * @author 신동준
 * @since 2025.09.16
 */
@RestController
@RequestMapping("/api/diaries")
@RequiredArgsConstructor
public class DiaryController {

    private final DiaryService diaryService;

    /**
     * 새 일기 작성
     */
    @PostMapping
    public ResponseEntity<DiaryResponse> createDiary(
            @Valid @RequestBody DiaryCreateRequest request,
            @AuthenticationPrincipal User user) {

        Diary diary = Diary.builder()
                .user(user)
                .title(request.getTitle())
                .content(request.getContent())
                .emotion(request.getEmotion())
                .weather(request.getWeather())
                .diaryDate(request.getDiaryDate())
                .build();

        Diary savedDiary = diaryService.createDiary(diary);
        DiaryResponse response = convertToResponse(savedDiary);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * 특정 일기 상세 조회
     */
    @GetMapping("/{diaryId}")
    public ResponseEntity<DiaryResponse> getDiary(
            @PathVariable Long diaryId,
            @AuthenticationPrincipal User user) {

        Diary diary = diaryService.getDiaryById(diaryId);
        // TODO: 사용자 권한 검증 로직 추가 필요 (일기 소유자 확인)
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
            @AuthenticationPrincipal User user) {

        Diary updateData = Diary.builder()
                .title(request.getTitle())
                .content(request.getContent())
                .emotion(request.getEmotion())
                .weather(request.getWeather())
                .diaryDate(request.getDiaryDate())
                .build();

        Diary updatedDiary = diaryService.updateDiary(diaryId, updateData);
        DiaryResponse response = convertToResponse(updatedDiary);

        return ResponseEntity.ok(response);
    }

    /**
     * 특정 일기 삭제
     */
    @DeleteMapping("/{diaryId}")
    public ResponseEntity<Void> deleteDiary(
            @PathVariable Long diaryId,
            @AuthenticationPrincipal User user) {

        // TODO: 사용자 권한 검증 로직 추가 필요 (일기 소유자 확인)
        diaryService.deleteDiary(diaryId);

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
                .createdAt(diary.getCreatedAt())
                .updatedAt(diary.getUpdatedAt())
                .build();
    }
}
