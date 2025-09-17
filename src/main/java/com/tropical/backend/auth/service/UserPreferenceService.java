package com.tropical.backend.auth.service;

import com.tropical.backend.auth.dto.request.UpdateUserPreferenceRequest;
import com.tropical.backend.auth.dto.response.UserPreferenceResponse;
import com.tropical.backend.auth.entity.User;
import com.tropical.backend.auth.entity.User.DateSystem;
import com.tropical.backend.auth.entity.User.WeekStart;
import com.tropical.backend.auth.entity.UserConsent.ConsentType;
import com.tropical.backend.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 사용자 선호 설정 통합 관리 서비스
 *
 * <p>
 * 로그인한 사용자의 전체 개인화 설정을 관리하는 통합 비즈니스 로직을 처리합니다.
 * 프로필 정보, 캘린더 설정, 알림 설정, 동의 상태 등 마이페이지에서 필요한
 * 모든 사용자 설정을 조회하고 수정하는 핵심 기능을 제공합니다.
 * </p>
 *
 * <p>주요 기능:</p>
 * <ul>
 *   <li>통합 선호 설정 조회 - 마이페이지 전체 데이터 제공</li>
 *   <li>부분 설정 수정 - 사용자가 원하는 항목만 선택적 변경</li>
 *   <li>프로필 정보 관리 - 닉네임, 생년월일 수정</li>
 *   <li>캘린더 표시 옵션 - UI 렌더링 설정</li>
 *   <li>알림 설정 관리 - AI 스몰토크, 일정, 투두 알림</li>
 *   <li>선택 동의 상태 관리 - AI 개인화 서비스 제어</li>
 * </ul>
 *
 * <p>보안 특징:</p>
 * <ul>
 *   <li>SecurityContext를 통한 현재 로그인 사용자 식별</li>
 *   <li>본인의 설정만 조회/수정 가능</li>
 *   <li>필수 동의는 조회만 가능, 선택 동의만 수정 가능</li>
 * </ul>
 *
 * @author 왕택준
 * @version 0.2
 * @since 2025.09.18
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class UserPreferenceService {

    private final UserRepository userRepository;
    private final UserConsentService userConsentService;

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
     * 내 선호 설정 통합 조회
     *
     * <p>
     * 현재 로그인한 사용자의 전체 개인화 설정을 조회합니다.
     * 프로필 정보, 캘린더 설정, 알림 설정, 동의 상태를 모두 포함하여
     * 마이페이지에서 필요한 모든 정보를 한 번에 제공합니다.
     * </p>
     *
     * <p>포함되는 설정 정보:</p>
     * <ul>
     *   <li>프로필: 닉네임, 생년월일</li>
     *   <li>캘린더: 주 시작 요일, 타임존, 공휴일 표시, 날짜 체계</li>
     *   <li>AI 스몰토크 알림: 활성화 여부, 시간, 요일</li>
     *   <li>일정 알림: 활성화 여부, 사전 알림 시간</li>
     *   <li>투두 알림: 활성화 여부, 알림 시간, 사전 알림 일수</li>
     *   <li>필수 동의: 읽기 전용 상태</li>
     *   <li>선택 동의: 수정 가능한 AI 개인화 설정</li>
     * </ul>
     *
     * @return 사용자의 전체 선호 설정 정보
     */
    public UserPreferenceResponse getMyPreferences() {
        log.debug("사용자 통합 선호 설정 조회 시작");

        User user = getCurrentUser();

        // 동의 상태를 필수/선택으로 구분하여 조회
        Map<ConsentType, Boolean> allConsents = userConsentService.getUserConsentStatus(user.getId());

        Map<ConsentType, Boolean> requiredConsents = Arrays.stream(ConsentType.getRequiredConsents())
                .collect(Collectors.toMap(
                        type -> type,
                        type -> allConsents.getOrDefault(type, false)
                ));

        Map<ConsentType, Boolean> optionalConsents = Arrays.stream(ConsentType.getOptionalConsents())
                .collect(Collectors.toMap(
                        type -> type,
                        type -> allConsents.getOrDefault(type, false)
                ));

        log.debug("사용자 통합 선호 설정 조회 완료 - 사용자 ID: {}", user.getId());

        return new UserPreferenceResponse(
                // 프로필 정보
                user.getNickname(),
                user.getBirthDate(),

                // 캘린더 설정
                user.getWeekStart(),
                user.getTimezone(),
                Boolean.TRUE.equals(user.getShowHolidays()),
                user.getDateSystem().name(),

                // AI 스몰토크 알림 설정
                Boolean.TRUE.equals(user.getSmalltalkNotificationEnabled()),
                user.getSmalltalkNotificationTime(),
                user.getSmalltalkNotificationDays(),

                // 일정 알림 설정
                Boolean.TRUE.equals(user.getScheduleNotificationEnabled()),
                user.getScheduleNotificationMinutes(),

                // 투두 알림 설정
                Boolean.TRUE.equals(user.getTodoNotificationEnabled()),
                user.getTodoNotificationTime(),
                user.getTodoNotificationDaysBefore(),

                // 동의 상태 (필수/선택 구분)
                requiredConsents,
                optionalConsents
        );
    }

    /**
     * 내 선호 설정 통합 부분 수정
     *
     * <p>
     * 현재 로그인한 사용자의 전체 개인화 설정을 부분적으로 수정합니다.
     * PATCH 방식으로 동작하며, 요청에 포함된 필드만 업데이트하고
     * null 값인 필드는 기존 설정을 유지합니다.
     * </p>
     *
     * <p>수정 가능한 설정 그룹:</p>
     * <ul>
     *   <li>프로필 정보: 닉네임, 생년월일</li>
     *   <li>캘린더 설정: 주 시작 요일, 타임존, 공휴일 표시, 날짜 체계</li>
     *   <li>알림 설정: AI 스몰토크, 일정, 투두 관련 모든 알림 옵션</li>
     *   <li>선택 동의: AI 개인화 서비스 동의 상태 (필수 동의는 수정 불가)</li>
     * </ul>
     *
     * <p>주의사항:</p>
     * <ul>
     *   <li>잘못된 enum 값 전달 시 IllegalArgumentException 발생</li>
     *   <li>필수 동의를 optionalConsents에 포함하면 무시됩니다</li>
     *   <li>변경사항은 트랜잭션 종료 시 자동으로 영속화됩니다</li>
     * </ul>
     *
     * @param req 수정할 선호 설정 요청 (모든 필드 부분 수정 가능)
     * @return 수정 후 전체 선호 설정 정보
     * @throws IllegalArgumentException 잘못된 enum 값이 전달된 경우
     */
    @Transactional
    public UserPreferenceResponse updateMyPreferences(UpdateUserPreferenceRequest req) {
        log.info("사용자 통합 선호 설정 수정 시작");

        User user = getCurrentUser();

        // ===============================
        // 프로필 정보 수정
        // ===============================

        if (req.nickname() != null) {
            log.debug("닉네임 수정: {} -> {}", user.getNickname(), req.nickname());
            user.changeNickname(req.nickname());
        }

        if (req.birthDate() != null) {
            log.debug("생년월일 수정: {} -> {}", user.getBirthDate(), req.birthDate());
            user.setBirthDate(req.birthDate());
        }

        // ===============================
        // 캘린더 설정 수정
        // ===============================

        if (req.weekStart() != null) {
            log.debug("주 시작 요일 수정: {} -> {}", user.getWeekStart(), req.weekStart());
            user.setWeekStart(WeekStart.valueOf(req.weekStart().toUpperCase()));
        }

        if (req.timezone() != null) {
            // TODO: 필요시 타임존 값 유효성 검증 추가 (IANA 목록)
            log.debug("타임존 수정: {} -> {}", user.getTimezone(), req.timezone());
            user.setTimezone(req.timezone());
        }

        if (req.showHolidays() != null) {
            log.debug("공휴일 표시 수정: {} -> {}", user.getShowHolidays(), req.showHolidays());
            user.setShowHolidays(req.showHolidays());
        }

        if (req.dateSystem() != null) {
            log.debug("날짜 체계 수정: {} -> {}", user.getDateSystem(), req.dateSystem());
            user.setDateSystem(DateSystem.valueOf(req.dateSystem().toUpperCase()));
        }

        // ===============================
        // AI 스몰토크 알림 설정 수정
        // ===============================

        if (req.smalltalkNotificationEnabled() != null) {
            log.debug("AI 스몰토크 알림 활성화 수정: {} -> {}",
                    user.getSmalltalkNotificationEnabled(), req.smalltalkNotificationEnabled());
            user.setSmalltalkNotificationEnabled(req.smalltalkNotificationEnabled());
        }

        if (req.smalltalkNotificationTime() != null) {
            log.debug("AI 스몰토크 알림 시간 수정: {} -> {}",
                    user.getSmalltalkNotificationTime(), req.smalltalkNotificationTime());
            user.setSmalltalkNotificationTime(req.smalltalkNotificationTime());
        }

        if (req.smalltalkNotificationDays() != null) {
            log.debug("AI 스몰토크 알림 요일 수정: {} -> {}",
                    user.getSmalltalkNotificationDays(), req.smalltalkNotificationDays());
            user.setSmalltalkNotificationDays(req.smalltalkNotificationDays());
        }

        // ===============================
        // 일정 알림 설정 수정
        // ===============================

        if (req.scheduleNotificationEnabled() != null) {
            log.debug("일정 알림 활성화 수정: {} -> {}",
                    user.getScheduleNotificationEnabled(), req.scheduleNotificationEnabled());
            user.setScheduleNotificationEnabled(req.scheduleNotificationEnabled());
        }

        if (req.scheduleNotificationMinutes() != null) {
            log.debug("일정 알림 시간 수정: {} -> {}",
                    user.getScheduleNotificationMinutes(), req.scheduleNotificationMinutes());
            user.setScheduleNotificationMinutes(req.scheduleNotificationMinutes());
        }

        // ===============================
        // 투두 알림 설정 수정
        // ===============================

        if (req.todoNotificationEnabled() != null) {
            log.debug("투두 알림 활성화 수정: {} -> {}",
                    user.getTodoNotificationEnabled(), req.todoNotificationEnabled());
            user.setTodoNotificationEnabled(req.todoNotificationEnabled());
        }

        if (req.todoNotificationTime() != null) {
            log.debug("투두 알림 시간 수정: {} -> {}",
                    user.getTodoNotificationTime(), req.todoNotificationTime());
            user.setTodoNotificationTime(req.todoNotificationTime());
        }

        if (req.todoNotificationDaysBefore() != null) {
            log.debug("투두 알림 사전 일수 수정: {} -> {}",
                    user.getTodoNotificationDaysBefore(), req.todoNotificationDaysBefore());
            user.setTodoNotificationDaysBefore(req.todoNotificationDaysBefore());
        }

        // ===============================
        // 선택 동의 상태 수정
        // ===============================

        if (req.optionalConsents() != null && !req.optionalConsents().isEmpty()) {
            log.debug("선택 동의 상태 수정 - 항목 수: {}", req.optionalConsents().size());

            for (Map.Entry<ConsentType, Boolean> entry : req.optionalConsents().entrySet()) {
                ConsentType consentType = entry.getKey();
                Boolean agreed = entry.getValue();

                // 필수 동의는 수정하지 않음 (보안 체크)
                if (consentType.isRequired()) {
                    log.warn("필수 동의 수정 시도 무시됨 - 사용자 ID: {}, 동의 타입: {}",
                            user.getId(), consentType);
                    continue;
                }

                // 선택 동의만 수정 처리
                boolean result = userConsentService.updateOptionalConsent(
                        user.getId(), consentType, agreed
                );

                if (result) {
                    log.debug("선택 동의 수정 완료 - 동의 타입: {}, 동의 여부: {}",
                            consentType, agreed);
                } else {
                    log.warn("선택 동의 수정 실패 - 동의 타입: {}, 동의 여부: {}",
                            consentType, agreed);
                }
            }
        }

        // 변경사항은 영속 컨텍스트에 의해 자동으로 저장됨
        log.info("사용자 통합 선호 설정 수정 완료 - 사용자 ID: {}", user.getId());

        // 수정된 설정 정보 반환
        return getMyPreferences();
    }
}