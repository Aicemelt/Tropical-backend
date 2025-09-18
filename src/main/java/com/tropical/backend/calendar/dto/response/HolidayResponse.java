package com.tropical.backend.calendar.dto.response;

import com.tropical.backend.calendar.entity.Holiday;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.LocalDate;

/**
 * 공휴일 응답 DTO.
 *
 * <p>
 * Holiday 엔터티를 클라이언트 친화적인 형태로 변환하여 제공하는 데이터 전송 객체입니다.
 * 내부 엔터티를 그대로 노출하지 않고 API 스펙에 맞는 필요한 필드만 선별하여 반환합니다.
 * </p>
 *
 * <p>설계 원칙:</p>
 * <ul>
 *   <li><b>엔터티 은닉</b>: 내부 구조를 클라이언트에 직접 노출하지 않음</li>
 *   <li><b>API 안정성</b>: 엔터티 변경이 API 스펙에 미치는 영향 최소화</li>
 *   <li><b>클라이언트 편의성</b>: 프론트엔드에서 사용하기 편한 형태로 데이터 제공</li>
 *   <li><b>문서화</b>: Swagger/OpenAPI 자동 문서 생성을 위한 스키마 정의</li>
 * </ul>
 *
 * <p>필드 설명:</p>
 * <ul>
 *   <li><b>countryCode</b>: 현재는 "KR" 고정, 향후 다국가 지원 시 활용</li>
 *   <li><b>date</b>: 공휴일 날짜 (현재 시스템은 하루 단위만 지원)</li>
 *   <li><b>startDate/endDate</b>: 향후 연휴(여러 날) 지원을 위한 확장 필드</li>
 *   <li><b>nameKo</b>: 한국어 공휴일명 (현재 다국어 미지원)</li>
 *   <li><b>holidayType</b>: 공휴일 분류 (법정공휴일, 국경일, 대체공휴일 등)</li>
 *   <li><b>isSubstitute</b>: 대체공휴일 여부 (빠른 판단을 위한 편의 필드)</li>
 * </ul>
 *
 * @param countryCode 국가 코드 (ISO 3166-1 alpha-2, 현재 "KR"만 지원)
 * @param date 공휴일 날짜 (하루 단위, startDate와 동일)
 * @param startDate 공휴일 시작 날짜 (현재는 date와 동일, 향후 연휴 지원용)
 * @param endDate 공휴일 종료 날짜 (현재는 date와 동일, 향후 연휴 지원용)
 * @param nameKo 한국어 공휴일명 (예: "신정", "설날", "어린이날")
 * @param holidayType 공휴일 분류 타입 (HolidayType enum)
 * @param isSubstitute 대체공휴일 여부 (true면 대체공휴일)
 *
 * @author  왕택준
 * @version 1.1 (Enhanced with date convenience field and improved naming)
 * @since   2025.09.17
 */
@Schema(name = "HolidayResponse", description = "공휴일 정보")
@Builder
public record HolidayResponse(
        @Schema(description = "국가 코드 (ISO 3166-1 alpha-2)", example = "KR")
        String countryCode,

        @Schema(description = "공휴일 날짜", example = "2025-01-01")
        LocalDate date,

        @Schema(description = "공휴일 시작 날짜 (현재는 date와 동일)", example = "2025-01-01")
        LocalDate startDate,

        @Schema(description = "공휴일 종료 날짜 (현재는 date와 동일)", example = "2025-01-01")
        LocalDate endDate,

        @Schema(description = "한국어 공휴일명", example = "신정")
        String nameKo,

        @Schema(description = "공휴일 분류", example = "PUBLIC_HOLIDAY", 
               allowableValues = {"PUBLIC_HOLIDAY", "NATIONAL_HOLIDAY", "SUBSTITUTE_HOLIDAY", 
                                "MEMORIAL_DAY", "SEASONAL_DIVISION", "TRADITIONAL_DAY"})
        Holiday.HolidayType holidayType,

        @Schema(description = "대체공휴일 여부", example = "false")
        boolean isSubstitute
) {

    /**
     * Holiday 엔터티로부터 HolidayResponse를 생성하는 정적 팩토리 메서드.
     *
     * <p>
     * 엔터티의 모든 관련 정보를 DTO로 안전하게 변환합니다.
     * null 체크나 기본값 설정이 필요한 경우 이 메서드에서 처리합니다.
     * </p>
     *
     * <p>변환 규칙:</p>
     * <ul>
     *   <li>date 필드는 startDate와 동일한 값으로 설정 (현재 하루 단위 공휴일만 지원)</li>
     *   <li>모든 필드가 null이 아님을 보장 (엔터티 레벨에서 검증됨)</li>
     *   <li>enum 타입은 그대로 전달하여 타입 안전성 유지</li>
     * </ul>
     *
     * @param holiday 변환할 Holiday 엔터티 (null이 아니어야 함)
     * @return 변환된 HolidayResponse 객체
     * @throws IllegalArgumentException holiday가 null인 경우
     */
    public static HolidayResponse from(Holiday holiday) {
        if (holiday == null) {
            throw new IllegalArgumentException("Holiday 엔터티는 null일 수 없습니다.");
        }

        return HolidayResponse.builder()
                .countryCode(holiday.getCountryCode())
                .date(holiday.getStartDate())  // 편의를 위한 단일 날짜 필드
                .startDate(holiday.getStartDate())
                .endDate(holiday.getEndDate())
                .nameKo(holiday.getNameKo())
                .holidayType(holiday.getHolidayType())
                .isSubstitute(holiday.isSubstitute())
                .build();
    }
}