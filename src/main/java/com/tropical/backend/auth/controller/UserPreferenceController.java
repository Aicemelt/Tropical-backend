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
 * 사용자 선호 설정 관리 컨트롤러
 *
 * <p>
 * 사용자의 개인화된 캘린더 및 알림 설정을 관리하는 REST API를 제공합니다.
 * 주 시작 요일, 타임존, 공휴일 표시 여부, 양력/음력 날짜 체계 등의
 * 사용자별 개인 선호 설정을 조회하고 수정할 수 있습니다.
 * </p>
 *
 * <p>주요 기능:</p>
 * <ul>
 *   <li>개인 선호 설정 조회 - 마이페이지 설정 화면 표시용</li>
 *   <li>개인 선호 설정 수정 - 사용자 맞춤 설정 변경</li>
 *   <li>캘린더 표시 옵션 관리 - 주 시작 요일, 공휴일 표시</li>
 *   <li>날짜 체계 설정 - 양력/음력 선택</li>
 *   <li>타임존 설정 - 지역별 시간대 관리</li>
 * </ul>
 *
 * <p>보안:</p>
 * <ul>
 *   <li>모든 API는 인증된 사용자만 접근 가능</li>
 *   <li>본인의 설정만 조회/수정 가능 (@PreAuthorize 적용)</li>
 * </ul>
 *
 * @author 왕택준
 * @version 0.1
 * @since 2025.09.17
 */
@Tag(name = "User Preferences", description = "사용자 선호 설정 API")
@RestController
@RequestMapping("/api/users/me")
@RequiredArgsConstructor
public class UserPreferenceController {

    private final UserPreferenceService userPreferenceService;

    /**
     * 내 선호 설정 조회
     *
     * <p>
     * 로그인한 사용자의 개인 선호 설정을 조회합니다.
     * 마이페이지의 설정 화면에서 현재 설정 상태를 표시하거나,
     * 캘린더 화면에서 사용자 맞춤 표시 옵션을 적용할 때 사용됩니다.
     * </p>
     *
     * <p>조회되는 설정:</p>
     * <ul>
     *   <li>주 시작 요일 (일요일/월요일)</li>
     *   <li>타임존 (Asia/Seoul 등)</li>
     *   <li>공휴일 표시 여부</li>
     *   <li>날짜 체계 (양력/음력)</li>
     * </ul>
     *
     * @return 사용자의 현재 선호 설정 정보
     */
    @GetMapping("/preferences")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "내 선호 설정 조회",
            description = "로그인 사용자의 캘린더/알림/표시 선호 설정을 반환합니다.")
    public ResponseEntity<UserPreferenceResponse> getMyPreferences() {
        return ResponseEntity.ok(userPreferenceService.getMyPreferences());
    }

    /**
     * 내 선호 설정 수정
     *
     * <p>
     * 로그인한 사용자의 개인 선호 설정을 부분적으로 수정합니다.
     * 마이페이지의 설정 화면에서 사용자가 원하는 옵션을 변경할 때 사용되며,
     * 변경된 설정은 즉시 캘린더와 알림 시스템에 반영됩니다.
     * </p>
     *
     * <p>수정 가능한 설정:</p>
     * <ul>
     *   <li>weekStartDay: 주 시작 요일 (SUN|MON)</li>
     *   <li>timeZone: 타임존 (예: Asia/Seoul, America/New_York)</li>
     *   <li>showHolidays: 공휴일 표시 여부 (true|false)</li>
     *   <li>dateSystem: 날짜 체계 (SOLAR|LUNAR)</li>
     * </ul>
     *
     * <p>부분 수정 지원:</p>
     * <ul>
     *   <li>요청 본문에 포함된 필드만 수정됩니다</li>
     *   <li>null 값인 필드는 기존 값을 유지합니다</li>
     * </ul>
     *
     * @param req 수정할 선호 설정 정보 (부분 수정 가능)
     * @return 수정된 사용자 선호 설정 정보
     * @throws jakarta.validation.ConstraintViolationException 잘못된 설정 값인 경우
     */
    @PatchMapping("/preferences")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "내 선호 설정 수정",
            description = "주 시작 요일(SUN|MON), 타임존, 공휴일 표시 여부, 날짜 체계(SOLAR|LUNAR)를 부분 수정합니다.")
    public ResponseEntity<UserPreferenceResponse> patchMyPreferences(
            @Valid @RequestBody UpdateUserPreferenceRequest req) {
        return ResponseEntity.ok(userPreferenceService.updateMyPreferences(req));
    }
}