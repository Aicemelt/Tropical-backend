package com.tropical.backend.schedule.controller;

import com.tropical.backend.schedule.entity.Schedule;
import com.tropical.backend.schedule.service.ScheduleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * 일정 관리 컨트롤러 (임시 테스트용)
 *
 * <p>
 * ScheduleService를 테스트하기 위한 임시 컨트롤러입니다.
 * 기본적인 CRUD 기능과 조회 기능을 제공하여
 * 서비스 계층의 동작을 확인할 수 있습니다.
 * </p>
 *
 * @author 신동준
 * @version 0.1
 * @since 2025.09.15
 */
@RestController
@RequestMapping("/api/test/schedules")
@RequiredArgsConstructor
@Slf4j
public class ScheduleTestController {

    private final ScheduleService scheduleService;

    /**
     * 사용자의 모든 일정 조회
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Schedule>> getSchedulesByUserId(@PathVariable Long userId) {
        log.info("사용자 일정 조회 API 호출: userId = {}", userId);
        List<Schedule> schedules = scheduleService.getSchedulesByUserId(userId);
        return ResponseEntity.ok(schedules);
    }

    /**
     * 특정 날짜의 일정 조회
     */
    @GetMapping("/user/{userId}/date/{date}")
    public ResponseEntity<List<Schedule>> getSchedulesByDate(
            @PathVariable Long userId,
            @PathVariable String date) {
        log.info("날짜별 일정 조회 API 호출: userId = {}, date = {}", userId, date);
        LocalDate localDate = LocalDate.parse(date);
        List<Schedule> schedules = scheduleService.getSchedulesByDate(userId, localDate);
        return ResponseEntity.ok(schedules);
    }

    /**
     * 월별 일정 조회
     */
    @GetMapping("/user/{userId}/month/{year}/{month}")
    public ResponseEntity<List<Schedule>> getSchedulesByMonth(
            @PathVariable Long userId,
            @PathVariable int year,
            @PathVariable int month) {
        log.info("월별 일정 조회 API 호출: userId = {}, year = {}, month = {}", userId, year, month);
        List<Schedule> schedules = scheduleService.getSchedulesByMonth(userId, year, month);
        return ResponseEntity.ok(schedules);
    }

    /**
     * 일정 상세 조회
     */
    @GetMapping("/{scheduleId}")
    public ResponseEntity<Schedule> getScheduleById(@PathVariable Long scheduleId) {
        log.info("일정 상세 조회 API 호출: scheduleId = {}", scheduleId);
        Schedule schedule = scheduleService.getScheduleById(scheduleId);
        return ResponseEntity.ok(schedule);
    }

    /**
     * 일정 완료 상태 토글
     */
    @PutMapping("/{scheduleId}/toggle")
    public ResponseEntity<Schedule> toggleScheduleCompletion(@PathVariable Long scheduleId) {
        log.info("일정 완료 토글 API 호출: scheduleId = {}", scheduleId);
        Schedule schedule = scheduleService.toggleScheduleCompletion(scheduleId);
        return ResponseEntity.ok(schedule);
    }

    /**
     * 테스트용 응답 확인
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Schedule API is working!");
    }
}
