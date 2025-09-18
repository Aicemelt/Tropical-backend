package com.tropical.backend.calendar.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

/**
 * 캘린더 UI용 휴무일(빨간날) 상태 응답 DTO.
 *
 * <p>
 * - isWeekend: 주말 여부<br>
 * - isHoliday: 실제 공휴일(법정/국경/대체) 여부<br>
 * - isDayOff : 빨간날 여부(주말 ∨ 공휴일)<br>
 * - holidayName: 공휴일명(공휴일일 때만 값 존재)<br>
 * </p>
 *
 * @param date        날짜 (yyyy-MM-dd)
 * @param isWeekend   주말 여부
 * @param isHoliday   공휴일 여부
 * @param isDayOff    빨간날 여부 (주말 OR 공휴일)
 * @param holidayName 공휴일명(공휴일인 경우)
 */
@Schema(name = "DayOffStatusResponse", description = "캘린더 UI용 주말/공휴일/빨간날 상태 응답")
public record DayOffStatusResponse(
        @Schema(example = "2025-01-01") LocalDate date,
        boolean isWeekend,
        boolean isHoliday,
        boolean isDayOff,
        String holidayName
) {}
