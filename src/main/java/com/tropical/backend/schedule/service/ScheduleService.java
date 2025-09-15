package com.tropical.backend.schedule.service;

import com.tropical.backend.schedule.entity.Schedule;

import java.time.LocalDate;
import java.util.List;

/**
 * 일정 관리 서비스 인터페이스
 *
 * <p>
 * Schedule 엔티티에 대한 비즈니스 로직을 정의하는 서비스 계층 인터페이스입니다.
 * CRUD 기본 기능과 일정 완료 토글, 날짜별 조회 등의 비즈니스 기능을 제공합니다.
 * </p>
 *
 * @author 신동준
 * @version 0.1
 * @since 2025.09.15
 */
public interface ScheduleService {

    /**
     * 새로운 일정을 생성합니다
     *
     * @param schedule 생성할 일정 정보
     * @return 생성된 일정
     */
    Schedule createSchedule(Schedule schedule);

    /**
     * ID로 일정을 조회합니다
     *
     * @param scheduleId 조회할 일정 ID
     * @return 조회된 일정
     */
    Schedule getScheduleById(Long scheduleId);

    /**
     * 사용자의 모든 일정을 조회합니다
     *
     * @param userId 사용자 ID
     * @return 사용자의 일정 목록
     */
    List<Schedule> getSchedulesByUserId(Long userId);

    /**
     * 특정 날짜의 일정을 조회합니다
     *
     * @param userId 사용자 ID
     * @param date 조회할 날짜
     * @return 해당 날짜의 일정 목록
     */
    List<Schedule> getSchedulesByDate(Long userId, LocalDate date);

    /**
     * 월별 일정을 조회합니다
     *
     * @param userId 사용자 ID
     * @param year 연도
     * @param month 월
     * @return 해당 월의 일정 목록
     */
    List<Schedule> getSchedulesByMonth(Long userId, int year, int month);

    /**
     * 일정 정보를 수정합니다
     *
     * @param scheduleId 수정할 일정 ID
     * @param schedule 수정할 일정 정보
     * @return 수정된 일정
     */
    Schedule updateSchedule(Long scheduleId, Schedule schedule);

    /**
     * 일정을 삭제합니다
     *
     * @param scheduleId 삭제할 일정 ID
     */
    void deleteSchedule(Long scheduleId);

    /**
     * 일정의 완료 상태를 토글합니다
     *
     * @param scheduleId 토글할 일정 ID
     * @return 토글된 일정
     */
    Schedule toggleScheduleCompletion(Long scheduleId);
}
