package com.tropical.backend.calendar.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

/**
 * 특정 날짜 휴일 여부 확인 응답 DTO.
 *
 * <p>
 * 클라이언트가 특정 날짜가 실제 휴무일인지 빠르게 확인할 수 있도록 
 * 최소한의 정보만을 포함하는 경량 응답 객체입니다.
 * 상세한 공휴일 정보가 필요한 경우 월별 조회 API를 사용하시기 바랍니다.
 * </p>
 *
 * <p>설계 목적:</p>
 * <ul>
 *   <li><b>빠른 판단</b>: 특정 날짜의 휴무일 여부만 즉시 확인</li>
 *   <li><b>경량 응답</b>: 네트워크 트래픽과 파싱 비용 최소화</li>
 *   <li><b>캐시 친화적</b>: 단순한 구조로 캐싱 효율성 향상</li>
 *   <li><b>UI 친화적</b>: 달력이나 일정 앱에서 바로 활용 가능</li>
 * </ul>
 *
 * <p>휴무일 판단 기준:</p>
 * <ul>
 *   <li><b>포함</b>: 법정공휴일, 국경일, 대체공휴일</li>
 *   <li><b>제외</b>: 기념일, 24절기, 전통 기념일 (실제 쉬는 날이 아님)</li>
 *   <li><b>주말</b>: 별도 확인 필요 (토요일, 일요일은 이 API 범위 밖)</li>
 * </ul>
 *
 * @param date 조회한 날짜 (요청과 동일한 값)
 * @param isHoliday 실제 휴무일 여부 (true: 휴무일, false: 평일)
 * @param holidayName 공휴일명 (isHoliday가 true인 경우에만 값 존재, 평일이면 null)
 *
 * @author  왕택준
 * @version 0.1
 * @since   2025.09.17
 */
@Schema(name = "HolidayCheckResponse", description = "특정 날짜 휴일 여부 확인 결과")
public record HolidayCheckResponse(
        @Schema(description = "조회한 날짜", example = "2025-01-01")
        LocalDate date,

        @Schema(description = "실제 휴무일 여부 (법정공휴일, 국경일, 대체공휴일만 해당)", example = "true")
        boolean isHoliday,

        @Schema(description = "공휴일명 (휴무일인 경우에만 제공)", example = "신정", nullable = true)
        String holidayName
) {

    /**
     * 평일(휴무일 아님)에 대한 응답을 생성하는 정적 팩토리 메서드.
     *
     * <p>
     * isHoliday = false, holidayName = null로 설정된 응답 객체를 생성합니다.
     * 코드 가독성을 높이고 일관된 객체 생성을 보장합니다.
     * </p>
     *
     * @param date 조회한 날짜
     * @return 평일임을 나타내는 HolidayCheckResponse 객체
     */
    public static HolidayCheckResponse notHoliday(LocalDate date) {
        return new HolidayCheckResponse(date, false, null);
    }

    /**
     * 휴무일에 대한 응답을 생성하는 정적 팩토리 메서드.
     *
     * <p>
     * isHoliday = true로 설정하고 해당 공휴일의 이름을 포함한 응답 객체를 생성합니다.
     * 클라이언트에서 공휴일 정보를 바로 표시할 수 있도록 돕습니다.
     * </p>
     *
     * @param date 조회한 날짜
     * @param holidayName 공휴일명 (예: "신정", "설날", "어린이날")
     * @return 휴무일임을 나타내는 HolidayCheckResponse 객체
     */
    public static HolidayCheckResponse holiday(LocalDate date, String holidayName) {
        return new HolidayCheckResponse(date, true, holidayName);
    }

    /**
     * 2개 파라미터 생성자 (호환성 지원).
     *
     * <p>
     * 기존 컨트롤러 코드에서 2개 파라미터로 호출하는 경우를 지원합니다.
     * holidayName은 null로 설정되며, 필요시 팩토리 메서드 사용을 권장합니다.
     * </p>
     *
     * @param date 조회한 날짜
     * @param isHoliday 휴무일 여부
     */
    public HolidayCheckResponse(LocalDate date, boolean isHoliday) {
        this(date, isHoliday, null);
    }
}