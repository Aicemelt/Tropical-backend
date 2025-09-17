package com.tropical.backend.auth.dto.response;

import com.tropical.backend.auth.entity.User.WeekStart;
import com.tropical.backend.auth.entity.UserConsent.ConsentType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Map;

/**
 * 사용자 선호 설정 조회 응답 DTO
 *
 * <p>
 * 사용자의 전체 개인화 설정을 프론트엔드에 전달하는 통합 응답 객체입니다.
 * 프로필 정보, 캘린더 설정, 알림 설정, 동의 상태 등 마이페이지에서 필요한
 * 모든 사용자 설정 정보를 한 번에 제공합니다.
 * </p>
 *
 * @author 왕택준
 * @version 0.2
 * @since 2025.09.18
 */
@Schema(name = "UserPreferenceResponse", description = "사용자 선호 설정 조회 응답")
public record UserPreferenceResponse(

        // ===============================
        // 프로필 정보
        // ===============================

        /**
         * 사용자 닉네임
         * <p>마이페이지에서 표시되는 사용자 표시명입니다</p>
         */
        @Schema(description = "사용자 닉네임", example = "홍길동")
        String nickname,

        /**
         * 사용자 생년월일
         * <p>선택 정보로, 연령대별 추천 서비스에 활용됩니다</p>
         */
        @Schema(description = "생년월일", example = "1990-05-15")
        LocalDate birthDate,

        // ===============================
        // 캘린더 표시 설정
        // ===============================

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
        String dateSystem,

        // ===============================
        // AI 스몰토크 알림 설정
        // ===============================

        /**
         * AI 스몰토크 알림 활성화 여부
         * <p>AI가 제안하는 일상 대화 주제 알림을 받을지 결정합니다</p>
         */
        @Schema(description = "AI 스몰토크 알림 활성화 여부", example = "true")
        boolean smalltalkNotificationEnabled,

        /**
         * AI 스몰토크 알림 시간
         * <p>매일 스몰토크 알림을 받을 시간을 설정합니다</p>
         */
        @Schema(description = "AI 스몰토크 알림 시간", example = "08:00:00")
        LocalTime smalltalkNotificationTime,

        /**
         * AI 스몰토크 알림 요일 설정
         * <p>알림을 받을 요일을 설정합니다 (daily, weekdays, weekends, custom)</p>
         */
        @Schema(description = "AI 스몰토크 알림 요일", example = "daily")
        String smalltalkNotificationDays,

        // ===============================
        // 일정 알림 설정
        // ===============================

        /**
         * 일정 시작 전 알림 활성화 여부
         * <p>캘린더 일정 시작 전 미리 알림을 받을지 결정합니다</p>
         */
        @Schema(description = "일정 시작 전 알림 활성화 여부", example = "true")
        boolean scheduleNotificationEnabled,

        /**
         * 일정 시작 전 알림 시간 (분 단위)
         * <p>일정 시작 몇 분 전에 알림을 받을지 설정합니다</p>
         */
        @Schema(description = "일정 시작 전 알림 시간 (분)", example = "30")
        int scheduleNotificationMinutes,

        // ===============================
        // 투두 마감 알림 설정
        // ===============================

        /**
         * 투두 마감 알림 활성화 여부
         * <p>할일의 마감일 알림을 받을지 결정합니다</p>
         */
        @Schema(description = "투두 마감 알림 활성화 여부", example = "true")
        boolean todoNotificationEnabled,

        /**
         * 투두 마감 알림 시간
         * <p>마감일에 알림을 받을 시간을 설정합니다</p>
         */
        @Schema(description = "투두 마감 알림 시간", example = "08:00:00")
        LocalTime todoNotificationTime,

        /**
         * 투두 마감 며칠 전 알림 설정
         * <p>마감일 며칠 전부터 미리 알림을 받을지 설정합니다</p>
         */
        @Schema(description = "투두 마감 며칠 전 알림", example = "1")
        int todoNotificationDaysBefore,

        // ===============================
        // 동의 상태 정보
        // ===============================

        /**
         * 필수 동의 현황 (읽기 전용)
         * <p>서비스 이용을 위한 필수 동의 항목들로, 약관 내용 확인만 가능하고 수정은 불가합니다</p>
         */
        @Schema(description = "필수 동의 현황 (읽기 전용)")
        Map<ConsentType, Boolean> requiredConsents,

        /**
         * 선택 동의 현황 (수정 가능)
         * <p>AI 개인화 서비스를 위한 선택 동의 항목들로, 마이페이지에서 ON/OFF 토글이 가능합니다</p>
         */
        @Schema(description = "선택 동의 현황 (수정 가능)")
        Map<ConsentType, Boolean> optionalConsents
) {
}