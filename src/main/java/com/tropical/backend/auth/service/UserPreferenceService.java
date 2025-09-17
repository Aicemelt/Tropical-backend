package com.tropical.backend.auth.service;

import com.tropical.backend.auth.dto.request.UpdateUserPreferenceRequest;
import com.tropical.backend.auth.dto.response.UserPreferenceResponse;
import com.tropical.backend.auth.entity.User;
import com.tropical.backend.auth.entity.User.DateSystem;
import com.tropical.backend.auth.entity.User.WeekStart;
import com.tropical.backend.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 사용자 선호 설정 관리 서비스
 *
 * <p>
 * 로그인한 사용자의 개인화된 캘린더 및 알림 설정을 관리하는 비즈니스 로직을 처리합니다.
 * 주 시작 요일, 타임존, 공휴일 표시 여부, 양력/음력 날짜 체계 등의
 * 사용자별 선호 설정을 조회하고 수정하는 핵심 기능을 제공합니다.
 * </p>
 *
 * <p>주요 기능:</p>
 * <ul>
 *   <li>개인 선호 설정 조회 - 마이페이지 설정 화면 데이터 제공</li>
 *   <li>개인 선호 설정 부분 수정 - 사용자 맞춤 설정 변경</li>
 *   <li>캘린더 표시 옵션 관리 - UI 렌더링에 필요한 설정 제공</li>
 *   <li>현재 로그인 사용자 컨텍스트 관리</li>
 * </ul>
 *
 * <p>보안 특징:</p>
 * <ul>
 *   <li>SecurityContext를 통한 현재 로그인 사용자 식별</li>
 *   <li>본인의 설정만 조회/수정 가능</li>
 * </ul>
 *
 * @author 왕택준
 * @version 0.1
 * @since 2025.09.17
 */
@Service
@RequiredArgsConstructor
public class UserPreferenceService {

    private final UserRepository userRepository;

    /**
     * 현재 로그인 사용자 조회
     *
     * <p>
     * Spring Security의 SecurityContext에서 현재 인증된 사용자의 ID를 추출하여
     * 해당 사용자 엔티티를 조회합니다. JWT 토큰의 subject에 사용자 ID가 포함되어 있다고 가정합니다.
     * </p>
     *
     * @return 현재 로그인한 사용자 엔티티
     * @throws IllegalArgumentException 사용자 ID에 해당하는 사용자가 존재하지 않는 경우
     */
    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Long userId = Long.valueOf(auth.getName());
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
    }

    /**
     * 내 선호 설정 조회
     *
     * <p>
     * 현재 로그인한 사용자의 개인 선호 설정을 조회합니다.
     * 마이페이지의 설정 화면 표시나 캘린더 UI 초기화에 사용되며,
     * 사용자별 맞춤 설정이 반영된 완전한 정보를 제공합니다.
     * </p>
     *
     * <p>반환되는 설정 정보:</p>
     * <ul>
     *   <li>주 시작 요일 (WeekStart enum)</li>
     *   <li>타임존 (IANA 표준 문자열)</li>
     *   <li>공휴일 표시 여부 (boolean)</li>
     *   <li>날짜 체계 (SOLAR/LUNAR 문자열)</li>
     * </ul>
     *
     * @return 사용자의 현재 선호 설정 정보
     */
    @Transactional(readOnly = true)
    public UserPreferenceResponse getMyPreferences() {
        User user = getCurrentUser();
        return new UserPreferenceResponse(
                user.getWeekStart(),
                user.getTimezone(),
                Boolean.TRUE.equals(user.getShowHolidays()),
                user.getDateSystem().name()
        );
    }

    /**
     * 내 선호 설정 부분 수정
     *
     * <p>
     * 현재 로그인한 사용자의 개인 선호 설정을 부분적으로 수정합니다.
     * PATCH 방식으로 동작하며, 요청에 포함된 필드만 업데이트하고
     * null 값인 필드는 기존 설정을 유지합니다.
     * </p>
     *
     * <p>수정 가능한 설정과 처리 방식:</p>
     * <ul>
     *   <li>weekStart: 문자열을 WeekStart enum으로 변환 (대소문자 무관)</li>
     *   <li>timezone: 문자열 그대로 저장 (추후 IANA 유효성 검증 추가 예정)</li>
     *   <li>showHolidays: boolean 값 그대로 저장</li>
     *   <li>dateSystem: 문자열을 DateSystem enum으로 변환 (대소문자 무관)</li>
     * </ul>
     *
     * <p>주의사항:</p>
     * <ul>
     *   <li>잘못된 enum 값 전달 시 IllegalArgumentException 발생</li>
     *   <li>변경사항은 트랜잭션 종료 시 자동으로 영속화됩니다</li>
     * </ul>
     *
     * @param req 수정할 선호 설정 요청 (부분 수정 가능)
     * @return 수정 후 현재 선호 설정 정보
     * @throws IllegalArgumentException 잘못된 enum 값이 전달된 경우
     */
    @Transactional
    public UserPreferenceResponse updateMyPreferences(UpdateUserPreferenceRequest req) {
        User user = getCurrentUser();

        // 주 시작 요일 수정
        if (req.weekStart() != null) {
            user.setWeekStart(WeekStart.valueOf(req.weekStart().toUpperCase()));
        }

        // 타임존 수정
        if (req.timezone() != null) {
            // TODO: 필요시 타임존 값 유효성 검증 추가 (IANA 목록)
            user.setTimezone(req.timezone());
        }

        // 공휴일 표시 여부 수정
        if (req.showHolidays() != null) {
            user.setShowHolidays(req.showHolidays());
        }

        // 날짜 체계 수정
        if (req.dateSystem() != null) {
            user.setDateSystem(DateSystem.valueOf(req.dateSystem().toUpperCase()));
        }

        // 변경사항은 영속 컨텍스트에 의해 자동으로 저장됨
        // 수정된 설정 정보 반환
        return getMyPreferences();
    }
}