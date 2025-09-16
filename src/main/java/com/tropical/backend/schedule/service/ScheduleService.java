package com.tropical.backend.schedule.service;

import com.tropical.backend.schedule.entity.Schedule;

import java.time.LocalDate;
import java.util.List;

/**
 * 일정 관리 서비스 인터페이스
 *
 * @author 신동준
 * @version 0.1
 * @since 2025.09.15
 */
public interface ScheduleService {

    // === 인증된 사용자용 메서드들 (주로 사용) ===

    /**
     * User ID와 함께 새로운 일정을 생성합니다
     */
    Schedule createScheduleWithUserId(Long userId, Schedule schedule);

    /**
     * 일정 ID와 사용자 ID로 일정을 조회합니다 (권한 검증 포함)
     */
    Schedule getScheduleByIdAndUserId(Long scheduleId, Long userId);

    /**
     * 사용자 권한을 검증하여 일정을 수정합니다
     */
    Schedule updateScheduleByUserId(Long scheduleId, Long userId, Schedule schedule);

    /**
     * 사용자 권한을 검증하여 일정을 삭제합니다
     */
    void deleteScheduleByUserId(Long scheduleId, Long userId);

    /**
     * 사용자 권한을 검증하여 일정의 완료 상태를 토글합니다
     */
    Schedule toggleScheduleCompletionByUserId(Long scheduleId, Long userId);

    // === 조회용 메서드들 ===

    /**
     * 사용자의 모든 일정을 조회합니다
     */
    List<Schedule> getSchedulesByUserId(Long userId);

    /**
     * 특정 날짜의 일정을 조회합니다
     */
    List<Schedule> getSchedulesByDate(Long userId, LocalDate date);

    /**
     * 월별 일정을 조회합니다
     */
    List<Schedule> getSchedulesByMonth(Long userId, int year, int month);

    // === 기존 메서드들 (호환성 유지용 - 사용 안함) ===

    Schedule createSchedule(Schedule schedule);
    Schedule getScheduleById(Long scheduleId);
    Schedule updateSchedule(Long scheduleId, Schedule schedule);
    void deleteSchedule(Long scheduleId);
    Schedule toggleScheduleCompletion(Long scheduleId);
}
