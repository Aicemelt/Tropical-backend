package com.tropical.backend.todo.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Todo 완료/미완료 처리 요청 DTO
 *
 * @author 백승현
 * @version 1.0
 * @since 2025.09.16
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TodosCompleteRequest {
    @NotNull(message = "완료 상태는 필수입니다.")
    private Boolean isCompleted;
}