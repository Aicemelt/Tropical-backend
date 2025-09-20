package com.tropical.backend.calendar.controller;


import com.tropical.backend.calendar.dto.response.DayOffStatusResponse;
import com.tropical.backend.calendar.dto.response.HolidayCheckResponse;
import com.tropical.backend.calendar.dto.response.HolidayEventResponse;
import com.tropical.backend.calendar.dto.response.HolidayResponse;
import com.tropical.backend.calendar.entity.Holiday;
import com.tropical.backend.calendar.service.HolidayService;
import com.tropical.backend.common.util.YearWithin;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/**
 * 공휴일 조회 REST API 컨트롤러.
 *
 * <p>
 * 한국 공휴일 정보를 제공하는 RESTful API를 구현합니다.
 * 캐시 우선 전략을 통해 빠른 응답 속도를 제공하며,
 * 캐시 미스 시에만 외부 API를 호출하여 데이터를 수집합니다.
 * </p>
 *
 * <p>주요 기능:</p>
 * <ul>
 *   <li>월별 공휴일 목록 조회 (실제 휴무일, 기념일, 24절기 포함)</li>
 *   <li>특정 날짜의 실제 휴무일 여부 확인</li>
 *   <li>Bean Validation을 통한 입력 파라미터 검증</li>
 *   <li>Swagger/OpenAPI 문서화 지원</li>
 * </ul>
 *
 * @author 왕택준
 * @version 1.0
 * @since 2025.09.17
 */
@Slf4j
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/holidays")
@Tag(name = "Holiday", description = "공휴일 조회 API")
public class HolidayController {

    private final HolidayService holidayService;

    /**
     * 월별 공휴일 목록을 조회합니다.
     *
     * <p>
     * 지정된 연도와 월의 모든 공휴일, 기념일, 24절기 정보를 반환합니다.
     * 캐시 우선 전략을 사용하여 빠른 응답 속도를 제공하며,
     * 캐시에 데이터가 없는 경우에만 외부 API를 호출합니다.
     * </p>
     *
     * <p>포함되는 특일 유형:</p>
     * <ul>
     *   <li><b>법정 공휴일</b>: 신정, 설날, 어린이날, 현충일, 광복절, 개천절, 한글날, 성탄절</li>
     *   <li><b>국경일</b>: 3·1절, 제헌절, 광복절, 개천절, 한글날</li>
     *   <li><b>대체공휴일</b>: 공휴일이 주말과 겹칠 때 지정되는 휴일</li>
     *   <li><b>기념일</b>: 어버이날, 스승의날, 어린이날 등</li>
     *   <li><b>24절기</b>: 입춘, 하지, 추분, 동지 등</li>
     * </ul>
     *
     * @param year  조회할 연도 (1900년 이상, 현재 연도 +5 이하 권장)
     * @param month 조회할 월 (1-12)
     * @return 해당 월의 공휴일 목록 (HolidayDto 배열)
     * @throws IllegalArgumentException 연도나 월이 유효 범위를 벗어난 경우
     */
    @GetMapping("/monthly")
    @Operation(
            summary = "월별 공휴일 조회",
            description = "캐시 우선 전략으로 지정된 월의 모든 공휴일, 기념일, 24절기 정보를 조회합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 파라미터"),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    public ResponseEntity<List<HolidayResponse>> getMonthlyHolidays(
            @Parameter(description = "조회할 연도 (1900 이상, 현재 연도 +5 이하)")
            @RequestParam
            @YearWithin(min = 1900)
            int year,

            @Parameter(description = "조회할 월 (1-12)", example = "1")
            @RequestParam
            @Min(value = 1, message = "월은 1 이상이어야 합니다")
            @Max(value = 12, message = "월은 12 이하여야 합니다")
            int month
    ) {
        log.debug("월별 공휴일 조회 요청 - year: {}, month: {}", year, month);

        List<Holiday> holidays = holidayService.getMonthlyHolidays(year, month);
        List<HolidayResponse> response = holidays.stream()
                .map(HolidayResponse::from)
                .toList();

        log.debug("월별 공휴일 조회 완료 - year: {}, month: {}, 결과 수: {}", year, month, response.size());

        return ResponseEntity.ok(response);
    }

    /**
     * 특정 날짜가 실제 휴무일인지 확인합니다.
     *
     * <p>
     * 지정된 날짜가 법정 공휴일, 국경일, 대체공휴일에 해당하는지 검사합니다.
     * 단순 기념일이나 24절기는 실제 휴무일이 아니므로 false를 반환합니다.
     * 주말(토요일, 일요일) 여부는 별도로 확인해야 합니다.
     * </p>
     *
     * <p>실제 휴무일로 인정하는 타입:</p>
     * <ul>
     *   <li><b>PUBLIC_HOLIDAY</b>: 법정 공휴일</li>
     *   <li><b>NATIONAL_HOLIDAY</b>: 국경일</li>
     *   <li><b>SUBSTITUTE_HOLIDAY</b>: 대체공휴일</li>
     * </ul>
     *
     * <p>휴무일로 인정하지 않는 타입:</p>
     * <ul>
     *   <li>MEMORIAL_DAY: 기념일 (어버이날, 스승의날 등)</li>
     *   <li>SEASONAL_DIVISION: 24절기</li>
     *   <li>TRADITIONAL_DAY: 전통 기념일</li>
     * </ul>
     *
     * @param date 확인할 날짜 (ISO-8601 형식: yyyy-MM-dd)
     * @return 날짜와 휴무일 여부를 포함한 응답 객체
     * @throws IllegalArgumentException 날짜 형식이 올바르지 않은 경우
     */
    @GetMapping("/check")
    @Operation(
            summary = "특정 날짜 휴일 여부 확인",
            description = "해당 날짜가 실제 휴무일(법정공휴일, 국경일, 대체공휴일)인지 여부를 반환합니다. 기념일이나 24절기는 제외됩니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 날짜 형식"),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    public ResponseEntity<HolidayCheckResponse> checkHolidayStatus(
            @Parameter(description = "확인할 날짜 (yyyy-MM-dd)", example = "2025-01-01")
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate date
    ) {
        log.debug("단일 날짜 휴일 여부 확인 요청 - date: {}", date);

        HolidayCheckResponse response = holidayService.checkHolidayStatus(date);

        log.debug("단일 날짜 휴일 여부 확인 완료 - date: {}, isHoliday: {}, holidayName: {}",
                response.date(), response.isHoliday(), response.holidayName());

        return ResponseEntity.ok(response);
    }

    /**
     * 월 단위 휴무일 상태를 조회합니다.
     *
     * <p>
     * 지정된 월의 각 날짜(1일~말일)에 대해 주말 여부, 공휴일 여부,
     * 전체 휴무일(빨간날) 여부, 그리고 공휴일명을 반환합니다.
     * 캘린더 UI에서 월별 날짜 상태 표시에 활용할 수 있습니다.
     * </p>
     *
     * <p>반환 정보:</p>
     * <ul>
     *   <li><b>isWeekend</b>: 토요일 또는 일요일 여부</li>
     *   <li><b>isHoliday</b>: 실제 공휴일(법정/국경/대체) 여부</li>
     *   <li><b>isDayOff</b>: 휴무일(주말 ∨ 공휴일) 여부</li>
     *   <li><b>holidayName</b>: 공휴일인 경우 공휴일명, 아니면 null</li>
     * </ul>
     *
     * @param year  조회할 연도 (1900년 이상)
     * @param month 조회할 월 (1-12)
     * @return 해당 월 각 날짜의 휴무일 상태 정보 리스트
     * @throws IllegalArgumentException 연도나 월이 유효 범위를 벗어난 경우
     */
    @GetMapping("/month-status")
    @Operation(
            summary = "월 단위 휴무일 상태 조회",
            description = "지정된 월의 각 날짜에 대해 주말/공휴일/전체 휴무일 여부와 공휴일명을 반환합니다. 캘린더 UI 구현에 유용합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 파라미터"),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    public ResponseEntity<List<DayOffStatusResponse>> getMonthStatus(
            @Parameter(description = "조회할 연도", example = "2025")
            @RequestParam
            @YearWithin(min = 1900)
            int year,

            @Parameter(description = "조회할 월 (1-12)", example = "1")
            @RequestParam
            @Min(value = 1, message = "월은 1 이상이어야 합니다")
            @Max(value = 12, message = "월은 12 이하여야 합니다")
            int month
    ) {
        log.debug("월 단위 휴무일 상태 조회 요청 - year: {}, month: {}", year, month);

        List<DayOffStatusResponse> statusList = holidayService.getMonthDayOffStatus(year, month);

        log.debug("월 단위 휴무일 상태 조회 완료 - year: {}, month: {}, 날짜 수: {}",
                year, month, statusList.size());

        return ResponseEntity.ok(statusList);
    }

    /**
     * FullCalendar 배경 이벤트용 공휴일 데이터를 조회합니다.
     *
     * <p>
     * 지정된 기간 내의 모든 공휴일을 FullCalendar에서 배경 이벤트로
     * 표시할 수 있는 형태로 반환합니다. 각 공휴일은 하루 단위 이벤트로 처리되며,
     * 캘린더 UI에서 배경색으로 강조 표시하는데 활용됩니다.
     * </p>
     *
     * <p>이벤트 속성:</p>
     * <ul>
     *   <li><b>display</b>: "background" - 배경 이벤트로 표시</li>
     *   <li><b>className</b>: "fc-holiday-bg" - CSS 스타일링용 클래스</li>
     *   <li><b>allDay</b>: true - 종일 이벤트</li>
     *   <li><b>title</b>: 공휴일명</li>
     * </ul>
     *
     * <p>주의사항:</p>
     * <p>end 파라미터는 FullCalendar의 관례에 따라 <b>미포함(exclusive)</b>으로 처리됩니다.
     * 예: start=2025-01-01, end=2025-02-01이면 1월 31일까지 조회</p>
     *
     * @param start 시작 날짜 (포함)
     * @param end   종료 날짜 (미포함)
     * @return 공휴일 배경 이벤트 리스트
     * @throws IllegalArgumentException start/end가 null이거나 end가 start보다 이전인 경우
     */
    @GetMapping("/events")
    @Operation(
            summary = "공휴일 배경 이벤트 조회",
            description = "FullCalendar 배경 이벤트로 표시할 공휴일 데이터를 반환합니다. end는 미포함(exclusive) 처리됩니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 날짜 파라미터"),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    public ResponseEntity<List<HolidayEventResponse>> getHolidayEvents(
            @Parameter(description = "시작 날짜 (포함, yyyy-MM-dd)", example = "2025-01-01")
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate start,

            @Parameter(description = "종료 날짜 (미포함, yyyy-MM-dd)", example = "2025-02-01")
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate end
    ) {
        log.debug("공휴일 배경 이벤트 조회 요청 - start: {}, end: {}", start, end);

        // end를 exclusive에서 inclusive로 변환 (end - 1일)
        LocalDate inclusiveEnd = end.minusDays(1);
        List<HolidayEventResponse> events = holidayService.getHolidayEvents(start, inclusiveEnd);

        log.debug("공휴일 배경 이벤트 조회 완료 - start: {}, end: {}, 이벤트 수: {}",
                start, end, events.size());

        return ResponseEntity.ok(events);
    }
}