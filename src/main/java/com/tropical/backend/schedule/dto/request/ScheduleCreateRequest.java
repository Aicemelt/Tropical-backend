package com.tropical.backend.schedule.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * 일정 생성 요청 DTO
 *
 * @author 신동준
 * @since 2025.09.16
 */
@Data
public class ScheduleCreateRequest {

    @NotBlank(message = "일정 제목은 필수입니다")
    @Size(max = 255, message = "일정 제목은 255자를 초과할 수 없습니다")
    private String title;

    @Size(max = 1000, message = "메모는 1000자를 초과할 수 없습니다")
    private String memo;

    @NotNull(message = "일정 날짜는 필수입니다")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate scheduleDate;

    @JsonFormat(pattern = "HH:mm")
    private LocalTime startTime;

    @JsonFormat(pattern = "HH:mm")
    private LocalTime endTime;

    @Size(max = 100, message = "장소는 100자를 초과할 수 없습니다")
    private String location;

    @Size(max = 255, message = "참석자 정보는 255자를 초과할 수 없습니다")
    private String attendees;
}
