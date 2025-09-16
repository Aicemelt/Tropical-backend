package com.tropical.backend.schedule.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 일정 완료/미완료 토글 요청 DTO
 *
 * @author 신동준
 * @since 2025.09.16
 */
@Data
public class ScheduleCompleteRequest {

    @NotNull(message = "완료 상태는 필수입니다")
    private Boolean isCompleted;
}
