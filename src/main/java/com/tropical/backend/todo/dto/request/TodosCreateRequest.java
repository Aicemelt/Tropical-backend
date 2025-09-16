package com.tropical.backend.todo.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * Todo 생성 요청 DTO
 *
 * @author 백승현
 * @version 1.0
 * @since 2025.09.15
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TodosCreateRequest {
    @NotBlank(message = "할 일 내용은 필수입니다.")
    private String content;

    @JsonFormat(pattern = "yyyy-MM-dd")
    @TodosUpdateRequest.TodayOrFuture(message = "마감일은 오늘 이후 날짜여야 합니다.")
    private LocalDate dueDate;




}