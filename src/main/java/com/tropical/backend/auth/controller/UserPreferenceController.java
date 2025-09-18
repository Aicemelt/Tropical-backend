package com.tropical.backend.auth.controller;

import com.tropical.backend.auth.dto.request.UpdateUserPreferenceRequest;
import com.tropical.backend.auth.dto.response.UserPreferenceResponse;
import com.tropical.backend.auth.service.UserPreferenceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 사용자 선호 설정 통합 관리 컨트롤러
 *
 * <p>
 * 사용자의 전체 개인화 설정을 관리하는 REST API를 제공합니다.
 * 프로필 정보, 캘린더 설정, 알림 설정, 동의 상태 등 마이페이지에서 필요한
 * 모든 사용자 설정을 조회하고 수정할 수 있는 통합 인터페이스입니다.
 * </p>
 *
 * <p>주요 기능:</p>
 * <ul>
 *   <li>통합 선호 설정 조회 - 마이페이지 전체 데이터 제공</li>
 *   <li>부분 설정 수정 - 사용자가 원하는 항목만 선택적 변경</li>
 *   <li>프로필 정보 관리 - 닉네임, 생년월일</li>
 *   <li>캘린더 표시 옵션 - 주 시작 요일, 공휴일 표시, 날짜 체계</li>
 *   <li>알림 설정 관리 - AI 스몰토크, 일정, 투두 알림</li>
 *   <li>동의 상태 관리 - 필수 동의 조회, 선택 동의 수정</li>
 * </ul>
 *
 * <p>보안:</p>
 * <ul>
 *   <li>모든 API는 인증된 사용자만 접근 가능</li>
 *   <li>본인의 설정만 조회/수정 가능 (@PreAuthorize 적용)</li>
 *   <li>필수 동의는 조회만 가능, 선택 동의만 수정 가능</li>
 * </ul>
 *
 * @author 왕택준
 * @version 0.2
 * @since 2025.09.18
 */
@Tag(name = "User Preferences", description = "사용자 선호 설정 통합 관리 API")
@RestController
@RequestMapping("/api/v1/users/me")
@RequiredArgsConstructor
public class UserPreferenceController {

    private final UserPreferenceService userPreferenceService;

    /**
     * 내 선호 설정 통합 조회
     *
     * <p>
     * 로그인한 사용자의 전체 개인화 설정을 조회합니다.
     * 마이페이지에서 필요한 모든 설정 정보를 한 번의 API 호출로 제공하여
     * 프론트엔드의 초기 로딩 성능을 최적화합니다.
     * </p>
     *
     * <p>조회되는 설정 그룹:</p>
     * <ul>
     *   <li><strong>프로필 정보:</strong> 닉네임, 생년월일</li>
     *   <li><strong>캘린더 설정:</strong> 주 시작 요일, 타임존, 공휴일 표시, 날짜 체계</li>
     *   <li><strong>AI 스몰토크 알림:</strong> 활성화 여부, 시간, 요일</li>
     *   <li><strong>일정 알림:</strong> 활성화 여부, 사전 알림 시간</li>
     *   <li><strong>투두 알림:</strong> 활성화 여부, 알림 시간, 사전 알림 일수</li>
     *   <li><strong>필수 동의:</strong> 서비스 이용약관, 개인정보처리방침, 일정 기반 추천 (읽기 전용)</li>
     *   <li><strong>선택 동의:</strong> AI 개인화 서비스 동의 상태 (수정 가능)</li>
     * </ul>
     *
     * <p>사용 시나리오:</p>
     * <ul>
     *   <li>마이페이지 초기 로딩 시 모든 설정 상태 표시</li>
     *   <li>캘린더 UI 초기화를 위한 표시 옵션 조회</li>
     *   <li>알림 시스템 설정을 위한 사용자 선호도 조회</li>
     *   <li>AI 개인화 서비스 이용 가능 여부 확인</li>
     * </ul>
     *
     * @return 사용자의 전체 선호 설정 정보
     */
    @GetMapping("/preferences")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "내 선호 설정 통합 조회",
            description = "프로필 정보, 캘린더 설정, 알림 설정, 동의 상태를 포함한 사용자의 전체 개인화 설정을 반환합니다."
    )
    public ResponseEntity<UserPreferenceResponse> getMyPreferences() {
        return ResponseEntity.ok(userPreferenceService.getMyPreferences());
    }

    /**
     * 내 선호 설정 통합 부분 수정
     *
     * <p>
     * 로그인한 사용자의 전체 개인화 설정을 부분적으로 수정합니다.
     * 마이페이지에서 사용자가 변경하고 싶은 설정만 선택적으로 업데이트할 수 있으며,
     * 변경된 설정은 즉시 전체 시스템에 반영됩니다.
     * </p>
     *
     * <p>수정 가능한 설정 그룹:</p>
     * <ul>
     *   <li><strong>프로필 정보:</strong></li>
     *   <ul>
     *     <li>nickname: 사용자 표시명</li>
     *     <li>birthDate: 생년월일 (yyyy-MM-dd 형식)</li>
     *   </ul>
     *   <li><strong>캘린더 설정:</strong></li>
     *   <ul>
     *     <li>weekStart: 주 시작 요일 (SUN|MON)</li>
     *     <li>timezone: 타임존 (IANA 형식, 예: Asia/Seoul)</li>
     *     <li>showHolidays: 공휴일 표시 여부 (true|false)</li>
     *     <li>dateSystem: 날짜 체계 (SOLAR|LUNAR)</li>
     *   </ul>
     *   <li><strong>AI 스몰토크 알림:</strong></li>
     *   <ul>
     *     <li>smalltalkNotificationEnabled: 알림 활성화 여부</li>
     *     <li>smalltalkNotificationTime: 알림 시간 (HH:mm:ss 형식)</li>
     *     <li>smalltalkNotificationDays: 알림 요일 (daily|weekdays|weekends|custom)</li>
     *   </ul>
     *   <li><strong>일정 알림:</strong></li>
     *   <ul>
     *     <li>scheduleNotificationEnabled: 알림 활성화 여부</li>
     *     <li>scheduleNotificationMinutes: 사전 알림 시간 (분 단위)</li>
     *   </ul>
     *   <li><strong>투두 알림:</strong></li>
     *   <ul>
     *     <li>todoNotificationEnabled: 알림 활성화 여부</li>
     *     <li>todoNotificationTime: 알림 시간 (HH:mm:ss 형식)</li>
     *     <li>todoNotificationDaysBefore: 사전 알림 일수</li>
     *   </ul>
     *   <li><strong>선택 동의:</strong></li>
     *   <ul>
     *     <li>optionalConsents: AI 개인화 서비스 동의 상태 (DIARY_PERSONALIZATION, TODO_PERSONALIZATION, BUCKET_PERSONALIZATION)</li>
     *   </ul>
     * </ul>
     *
     * <p>부분 수정 특징:</p>
     * <ul>
     *   <li>요청 본문에 포함된 필드만 수정됩니다</li>
     *   <li>null 값인 필드는 기존 값을 유지합니다</li>
     *   <li>필수 동의는 optionalConsents에 포함해도 무시됩니다</li>
     *   <li>잘못된 enum 값 전달 시 400 Bad Request 반환</li>
     * </ul>
     *
     * <p>사용 시나리오:</p>
     * <ul>
     *   <li>마이페이지에서 특정 설정만 변경할 때</li>
     *   <li>알림 설정을 일괄 조정할 때</li>
     *   <li>AI 개인화 서비스 동의를 변경할 때</li>
     *   <li>프로필 정보를 업데이트할 때</li>
     * </ul>
     *
     * @param req 수정할 선호 설정 정보 (모든 필드 부분 수정 가능)
     * @return 수정된 사용자의 전체 선호 설정 정보
     * @throws jakarta.validation.ConstraintViolationException 잘못된 설정 값인 경우
     * @throws IllegalArgumentException                        잘못된 enum 값이 전달된 경우
     */
    @PatchMapping("/preferences")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "내 선호 설정 통합 부분 수정",
            description = "프로필, 캘린더, 알림 설정, 선택 동의 등 사용자의 개인화 설정을 부분적으로 수정합니다. null 값인 필드는 기존 설정을 유지합니다."
    )
    public ResponseEntity<UserPreferenceResponse> patchMyPreferences(
            @Valid @RequestBody UpdateUserPreferenceRequest req) {
        return ResponseEntity.ok(userPreferenceService.updateMyPreferences(req));
    }
}