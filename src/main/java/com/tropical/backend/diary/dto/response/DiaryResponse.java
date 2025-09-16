package com.tropical.backend.diary.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 일기 응답 DTO
 *
 * @author 신동준
 * @since 2025.09.16
 */
@Data
@Builder
public class DiaryResponse {

    private Long diaryId;
    private String title;
    private String content;
    private String emotion;
    private String weather;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate diaryDate;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime updatedAt;
}
