package com.tropical.backend.diary.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

/**
 * 일기 수정 요청 DTO
 *
 * @author 신동준
 * @since 2025.09.16
 */
@Data
public class DiaryUpdateRequest {

    @Size(max = 255, message = "일기 제목은 255자를 초과할 수 없습니다")
    private String title;

    private String content;

    @Size(max = 20, message = "감정 정보는 20자를 초과할 수 없습니다")
    private String emotion;

    @Size(max = 20, message = "날씨 정보는 20자를 초과할 수 없습니다")
    private String weather;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate diaryDate;
}
