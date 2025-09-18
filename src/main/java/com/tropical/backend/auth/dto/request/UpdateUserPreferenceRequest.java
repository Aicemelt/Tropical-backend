package com.tropical.backend.auth.dto.request;

import com.tropical.backend.auth.entity.UserConsent.ConsentType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Map;

/**
 * 사용자 선호 설정 부분 수정 요청 DTO
 *
 * <p>
 * 마이페이지에서 사용자의 전체 개인화 설정을 수정하는 통합 요청 객체입니다.
 * null 값인 필드는 기존 설정을 유지하며, 요청에 포함된 필드만 업데이트됩니다.
 * 필수 동의는 수정할 수 없고, 선택 동의만 변경 가능합니다.
 * </p>
 *
 * @author 왕택준
 * @version 0.2
 * @since 2025.09.18
 */
@Schema(name = "UpdateUserPreferenceRequest", description = "사용자 선호 설정 부분 수정 요청")
public record UpdateUserPreferenceRequest(

        // ===============================
        // 프로필 정보
        // ===============================

        /**
         * 사용자 닉네임
         * <p>마이페이지에서 표시되는 사용자 표시명을 변경합니다. null이면 기존 값 유지</p>
         */
        @Schema(description = "사용자 닉네임", example = "새로운닉네임", nullable = true)
        String nickname,

        /**
         * 사용자 생년월일
         * <p>연령대별 추천 서비스를 위한 생년월일을 설정합니다. null이면 기존 값 유지</p>
         */
        @Schema(description = "생년월일", example = "1990-05-15", nullable = true)
        LocalDate birthDate,

        // ===============================
        // 캘린더 표시 설정
        // ===============================

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
        String dateSystem,

        // ===============================
        // AI 스몰토크 알림 설정
        // ===============================

        /**
         * AI 스몰토크 알림 활성화 여부
         * <p>AI가 제안하는 일상 대화 주제 알림을 받을지 결정합니다. null이면 기존 설정 유지</p>
         */
        @Schema(description = "AI 스몰토크 알림 활성화 여부", example = "true", nullable = true)
        Boolean smalltalkNotificationEnabled,

        /**
         * AI 스몰토크 알림 시간
         * <p>매일 스몰토크 알림을 받을 시간을 설정합니다. null이면 기존 설정 유지</p>
         */
        @Schema(description = "AI 스몰토크 알림 시간", example = "08:00:00", nullable = true)
        LocalTime smalltalkNotificationTime,

        /**
         * AI 스몰토크 알림 요일 설정
         * <p>알림을 받을 요일을 설정합니다 (daily, weekdays, weekends, custom). null이면 기존 설정 유지</p>
         */
        @Schema(description = "AI 스몰토크 알림 요일", example = "daily", nullable = true)
        String smalltalkNotificationDays,

        // ===============================
        // 일정 알림 설정
        // ===============================

        /**
         * 일정 시작 전 알림 활성화 여부
         * <p>캘린더 일정 시작 전 미리 알림을 받을지 결정합니다. null이면 기존 설정 유지</p>
         */
        @Schema(description = "일정 시작 전 알림 활성화 여부", example = "true", nullable = true)
        Boolean scheduleNotificationEnabled,

        /**
         * 일정 시작 전 알림 시간 (분 단위)
         * <p>일정 시작 몇 분 전에 알림을 받을지 설정합니다. null이면 기존 설정 유지</p>
         */
        @Schema(description = "일정 시작 전 알림 시간 (분)", example = "30", nullable = true)
        Integer scheduleNotificationMinutes,

        // ===============================
        // 투두 마감 알림 설정
        // ===============================

        /**
         * 투두 마감 알림 활성화 여부
         * <p>할일의 마감일 알림을 받을지 결정합니다. null이면 기존 설정 유지</p>
         */
        @Schema(description = "투두 마감 알림 활성화 여부", example = "true", nullable = true)
        Boolean todoNotificationEnabled,

        /**
         * 투두 마감 알림 시간
         * <p>마감일에 알림을 받을 시간을 설정합니다. null이면 기존 설정 유지</p>
         */
        @Schema(description = "투두 마감 알림 시간", example = "08:00:00", nullable = true)
        LocalTime todoNotificationTime,

        /**
         * 투두 마감 며칠 전 알림 설정
         * <p>마감일 며칠 전부터 미리 알림을 받을지 설정합니다. null이면 기존 설정 유지</p>
         */
        @Schema(description = "투두 마감 며칠 전 알림", example = "1", nullable = true)
        Integer todoNotificationDaysBefore,

        // ===============================
        // 선택 동의 설정 (필수 동의는 수정 불가)
        // ===============================

        /**
         * 선택 동의 상태 변경
         * <p>AI 개인화 서비스를 위한 선택 동의만 변경 가능합니다. 필수 동의는 포함하면 안 됩니다. null이면 동의 상태 변경 없음</p>
         */
        @Schema(
                description = "선택 동의 상태 변경 (diaryPersonalization, todoPersonalization, bucketPersonalization만 가능)",
                example = """
        {
          "diaryPersonalization": true,
          "todoPersonalization": false,
          "bucketPersonalization": true
        }
        """,
                nullable = true
        )
        Map<ConsentType, Boolean> optionalConsents
) {
}