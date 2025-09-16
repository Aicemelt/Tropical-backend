package com.tropical.backend.todo.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Todo 응답 DTO
 * API 명세서의 GET /todos 응답 형식에 맞춤
 *
 * @author 백승현
 * @version 1.0
 * @since 2025.09.16
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TodosResponse {
    private Long todoId;
    private String content;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate dueDate;

    private Boolean isCompleted;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime updatedAt;
}