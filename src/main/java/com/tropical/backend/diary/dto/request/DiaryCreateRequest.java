package com.tropical.backend.diary.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

/**
 * 일기 생성 요청 DTO
 *
 * @author 신동준
 * @since 2025.09.16
 */
@Data
public class DiaryCreateRequest {

    @NotBlank(message = "일기 제목은 필수입니다")
    @Size(max = 255, message = "일기 제목은 255자를 초과할 수 없습니다")
    private String title;

    @NotBlank(message = "일기 내용은 필수입니다")
    private String content;

    @NotNull(message = "감정 상태는 필수입니다")
    @Size(max = 20, message = "감정 정보는 20자를 초과할 수 없습니다")
    private String emotion;

    @NotNull(message = "날씨 정보는 필수입니다")
    @Size(max = 20, message = "날씨 정보는 20자를 초과할 수 없습니다")
    private String weather;

    @NotNull(message = "일기 날짜는 필수입니다")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate diaryDate;
}
