package com.tropical.backend.calendar.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

/**
 * FullCalendar 배경 이벤트용 공휴일 응답 DTO.
 *
 * <p>
 * display="background"로 내려 UI 배경에 공휴일을 표시합니다.
 * </p>
 *
 * @param start     시작일 (yyyy-MM-dd, 포함)
 * @param end       종료일 (yyyy-MM-dd, 미포함; FullCalendar 관례)
 * @param title     이벤트 제목 (예: 신정)
 * @param display   표시 방식 (권장: "background")
 * @param className CSS 클래스명 (예: "fc-holiday-bg")
 * @param allDay    종일 여부 (항상 true)
 */
@Schema(name = "HolidayEventResponse", description = "FullCalendar 배경 공휴일 이벤트")
public record HolidayEventResponse(
        @Schema(example = "2025-01-01") LocalDate start,
        @Schema(example = "2025-01-02") LocalDate end,
        @Schema(example = "신정") String title,
        @Schema(example = "background") String display,
        @Schema(example = "fc-holiday-bg") String className,
        boolean allDay
) {
    public static HolidayEventResponse background(LocalDate date, String title, String className) {
        // FullCalendar는 end를 '미포함'으로 처리 → 다음날로 설정
        return new HolidayEventResponse(date, date.plusDays(1), title, "background", className, true);
    }
}
