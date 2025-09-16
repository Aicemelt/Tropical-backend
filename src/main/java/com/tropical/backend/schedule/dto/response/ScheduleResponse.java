package com.tropical.backend.schedule.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * 일정 응답 DTO
 *
 * @author 신동준
 * @since 2025.09.16
 */
@Data
@Builder
public class ScheduleResponse {

    private Long scheduleId;
    private String title;
    private String memo;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate scheduleDate;

    @JsonFormat(pattern = "HH:mm")
    private LocalTime startTime;

    @JsonFormat(pattern = "HH:mm")
    private LocalTime endTime;

    private String location;
    private String attendees;
    private Boolean isCompleted;
}
