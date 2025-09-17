package com.tropical.backend.auth.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 사용자 선호 설정 부분 수정 요청 DTO
 *
 * <p>
 * 마이페이지 설정 화면에서 사용자가 원하는 항목만 선택적으로 수정할 수 있도록 설계된 요청 객체입니다.
 * null 값인 필드는 기존 설정을 유지하며, 요청에 포함된 필드만 업데이트됩니다.
 * </p>
 *
 * @author 왕택준
 * @version 0.1
 * @since 2025.09.17
 */
@Schema(name = "UpdateUserPreferenceRequest", description = "사용자 선호 설정 부분 수정 요청")
public record UpdateUserPreferenceRequest(

        /**
         * 주 시작 요일 (SUN|MON)
         * <p>캘린더 UI에서 한 주의 시작 요일을 결정합니다. null이면 기존 설정 유지</p>
         */
        @Schema(description = "주 시작 요일(SUN|MON)", example = "MON", nullable = true)
        String weekStart,

        /**
         * 사용자 타임존 (IANA 표준)
         * <p>일정과 알림의 시간을 현지 시간대에 맞춰 표시하기 위해 사용됩니다. null이면 기존 설정 유지</p>
         */
        @Schema(description = "사용자 타임존(IANA)", example = "Asia/Seoul", nullable = true)
        String timezone,

        /**
         * 공휴일 표시 여부
         * <p>캘린더에서 국가별 공휴일을 표시할지 결정합니다. null이면 기존 설정 유지</p>
         */
        @Schema(description = "공휴일 표시 여부", example = "true", nullable = true)
        Boolean showHolidays,

        /**
         * 달력 날짜 체계 (SOLAR|LUNAR)
         * <p>양력(SOLAR) 또는 음력(LUNAR) 기준으로 달력을 표시합니다. null이면 기존 설정 유지</p>
         */
        @Schema(description = "달력 날짜 체계(SOLAR|LUNAR)", example = "LUNAR", nullable = true)
        String dateSystem
) {}