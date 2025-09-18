package com.tropical.backend.schedule.controller;

import com.tropical.backend.schedule.dto.request.ScheduleCreateRequest;
import com.tropical.backend.schedule.dto.request.ScheduleUpdateRequest;
import com.tropical.backend.schedule.dto.response.ScheduleResponse;
import com.tropical.backend.schedule.entity.Schedule;
import com.tropical.backend.schedule.service.ScheduleService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
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
@Tag(name = "Schedule", description = "일정 API")
public class ScheduleController {

    private final ScheduleService scheduleService;

    /**
     * 새 일정 생성
     */
    @PostMapping
    public ResponseEntity<ScheduleResponse> createSchedule(
            @Valid @RequestBody ScheduleCreateRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        // 인증된 사용자 정보 검증
        if (userDetails == null) {
            throw new IllegalArgumentException("인증된 사용자 정보가 필요합니다");
        }

        // UserDetails에서 User ID 추출
        Long userId = Long.valueOf(userDetails.getUsername());

        // User ID를 가지고 Schedule 생성
        Schedule schedule = Schedule.builder()
                .title(request.getTitle())
                .memo(request.getMemo())
                .scheduleDate(request.getScheduleDate())
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .location(request.getLocation())
                .attendees(request.getAttendees())
                .build();

        // 서비스에서 User ID로 Schedule 생성
        Schedule savedSchedule = scheduleService.createScheduleWithUserId(userId, schedule);
        ScheduleResponse response = convertToResponse(savedSchedule);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * 특정 일정 상세 조회
     */
    @GetMapping("/{scheduleId}")
    public ResponseEntity<ScheduleResponse> getSchedule(
            @PathVariable Long scheduleId,
            @AuthenticationPrincipal UserDetails userDetails) {

        if (userDetails == null) {
            throw new IllegalArgumentException("인증된 사용자 정보가 필요합니다");
        }

        Long userId = Long.valueOf(userDetails.getUsername());
        Schedule schedule = scheduleService.getScheduleByIdAndUserId(scheduleId, userId);

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
            @AuthenticationPrincipal UserDetails userDetails) {

        if (userDetails == null) {
            throw new IllegalArgumentException("인증된 사용자 정보가 필요합니다");
        }

        Long userId = Long.valueOf(userDetails.getUsername());

        Schedule updateData = Schedule.builder()
                .title(request.getTitle())
                .memo(request.getMemo())
                .scheduleDate(request.getScheduleDate())
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .location(request.getLocation())
                .attendees(request.getAttendees())
                .build();

        Schedule updatedSchedule = scheduleService.updateScheduleByUserId(scheduleId, userId, updateData);
        ScheduleResponse response = convertToResponse(updatedSchedule);

        return ResponseEntity.ok(response);
    }

    /**
     * 특정 일정 삭제
     */
    @DeleteMapping("/{scheduleId}")
    public ResponseEntity<Void> deleteSchedule(
            @PathVariable Long scheduleId,
            @AuthenticationPrincipal UserDetails userDetails) {

        if (userDetails == null) {
            throw new IllegalArgumentException("인증된 사용자 정보가 필요합니다");
        }

        Long userId = Long.valueOf(userDetails.getUsername());
        scheduleService.deleteScheduleByUserId(scheduleId, userId);

        return ResponseEntity.noContent().build();
    }

    /**
     * 일정 완료/미완료 처리
     */
    @PutMapping("/{scheduleId}/complete")
    public ResponseEntity<ScheduleResponse> toggleScheduleComplete(
            @PathVariable Long scheduleId,
            @AuthenticationPrincipal UserDetails userDetails) {

        if (userDetails == null) {
            throw new IllegalArgumentException("인증된 사용자 정보가 필요합니다");
        }

        Long userId = Long.valueOf(userDetails.getUsername());
        Schedule updatedSchedule = scheduleService.toggleScheduleCompletionByUserId(scheduleId, userId);
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
                .build();
    }
}
