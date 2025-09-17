package com.tropical.backend.auth.dto.response;

import com.tropical.backend.auth.entity.User.WeekStart;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 사용자 선호 설정 조회 응답 DTO
 *
 * <p>
 * 캘린더 UI 렌더링, 설정 페이지 표시, 알림 시스템 등에서 사용자별 맞춤 설정을
 * 적용하기 위해 개인화된 선호 설정을 전달하는 응답 객체입니다.
 * </p>
 *
 * @author 왕택준
 * @version 0.1
 * @since 2025.09.17
 */
@Schema(name = "UserPreferenceResponse", description = "사용자 선호 설정 조회 응답")
public record UserPreferenceResponse(

        /**
         * 주 시작 요일
         * <p>캘린더에서 한 주의 첫 번째 요일로 표시될 요일입니다</p>
         */
        @Schema(description = "주 시작 요일", example = "MON")
        WeekStart weekStart,

        /**
         * 사용자 타임존 (IANA 표준)
         * <p>일정 시간 표시와 알림 발송을 현지 시간대 기준으로 처리하기 위해 사용됩니다</p>
         */
        @Schema(description = "시간대 (IANA Timezone ID)", example = "Asia/Seoul")
        String timezone,

        /**
         * 공휴일 표시 여부
         * <p>캘린더에서 국가별 공휴일을 시각적으로 표시할지 결정합니다</p>
         */
        @Schema(description = "공휴일 표시 여부", example = "true")
        boolean showHolidays,

        /**
         * 달력 날짜 체계
         * <p>양력(SOLAR) 또는 음력(LUNAR) 기준으로 달력을 표시합니다</p>
         */
        @Schema(description = "날짜 시스템 (SOLAR=양력, LUNAR=음력)", example = "SOLAR")
        String dateSystem
) {}