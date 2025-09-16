package com.tropical.backend.schedule.controller;

import com.tropical.backend.auth.entity.User;
import com.tropical.backend.schedule.dto.request.ScheduleCompleteRequest;
import com.tropical.backend.schedule.dto.request.ScheduleCreateRequest;
import com.tropical.backend.schedule.dto.request.ScheduleUpdateRequest;
import com.tropical.backend.schedule.dto.response.ScheduleResponse;
import com.tropical.backend.schedule.entity.Schedule;
import com.tropical.backend.schedule.service.ScheduleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * 일정 관리 컨트롤러
 *
 * @author 신동준
 * @since 2025.09.16
 */
@RestController
@RequestMapping("/api/schedules")
@RequiredArgsConstructor
public class ScheduleController {

    private final ScheduleService scheduleService;

    /**
     * 새 일정 생성
     */
    @PostMapping
    public ResponseEntity<ScheduleResponse> createSchedule(
            @Valid @RequestBody ScheduleCreateRequest request,
            @AuthenticationPrincipal User user) {

        Schedule schedule = Schedule.builder()
                .user(user)
                .title(request.getTitle())
                .memo(request.getMemo())
                .scheduleDate(request.getScheduleDate())
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .location(request.getLocation())
                .attendees(request.getAttendees())
                .build();

        Schedule savedSchedule = scheduleService.createSchedule(schedule);
        ScheduleResponse response = convertToResponse(savedSchedule);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * 특정 일정 상세 조회
     */
    @GetMapping("/{scheduleId}")
    public ResponseEntity<ScheduleResponse> getSchedule(
            @PathVariable Long scheduleId,
            @AuthenticationPrincipal User user) {

        Schedule schedule = scheduleService.getScheduleById(scheduleId);
        // TODO: 사용자 권한 검증 로직 추가 필요 (일정 소유자 확인)
        ScheduleResponse response = convertToResponse(schedule);

        return ResponseEntity.ok(response);
    }

    /**
     * 특정 일정 수정
     */
    @PutMapping("/{scheduleId}")
    public ResponseEntity<ScheduleResponse> updateSchedule(
            @PathVariable Long scheduleId,
            @Valid @RequestBody ScheduleUpdateRequest request,
            @AuthenticationPrincipal User user) {

        Schedule updateData = Schedule.builder()
                .title(request.getTitle())
                .memo(request.getMemo())
                .scheduleDate(request.getScheduleDate())
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .location(request.getLocation())
                .attendees(request.getAttendees())
                .build();

        Schedule updatedSchedule = scheduleService.updateSchedule(scheduleId, updateData);
        ScheduleResponse response = convertToResponse(updatedSchedule);

        return ResponseEntity.ok(response);
    }

    /**
     * 특정 일정 삭제
     */
    @DeleteMapping("/{scheduleId}")
    public ResponseEntity<Void> deleteSchedule(
            @PathVariable Long scheduleId,
            @AuthenticationPrincipal User user) {

        // TODO: 사용자 권한 검증 로직 추가 필요 (일정 소유자 확인)
        scheduleService.deleteSchedule(scheduleId);

        return ResponseEntity.noContent().build();
    }

    /**
     * 일정 완료/미완료 처리
     */
    @PutMapping("/{scheduleId}/complete")
    public ResponseEntity<ScheduleResponse> toggleScheduleComplete(
            @PathVariable Long scheduleId,
            @Valid @RequestBody ScheduleCompleteRequest request,
            @AuthenticationPrincipal User user) {

        // TODO: 사용자 권한 검증 로직 추가 필요 (일정 소유자 확인)
        Schedule updatedSchedule = scheduleService.toggleScheduleCompletion(scheduleId);
        ScheduleResponse response = convertToResponse(updatedSchedule);

        return ResponseEntity.ok(response);
    }

    /**
     * Schedule 엔티티를 ScheduleResponse DTO로 변환
     */
    private ScheduleResponse convertToResponse(Schedule schedule) {
        return ScheduleResponse.builder()
                .scheduleId(schedule.getId())
                .title(schedule.getTitle())
                .memo(schedule.getMemo())
                .scheduleDate(schedule.getScheduleDate())
                .startTime(schedule.getStartTime())
                .endTime(schedule.getEndTime())
                .location(schedule.getLocation())
                .attendees(schedule.getAttendees())
                .isCompleted(schedule.getIsCompleted())
                .createdAt(schedule.getCreatedAt())
                .updatedAt(schedule.getUpdatedAt())
                .build();
    }
}
