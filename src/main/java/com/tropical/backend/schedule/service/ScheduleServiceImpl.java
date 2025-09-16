package com.tropical.backend.schedule.service;

import com.tropical.backend.auth.entity.User;
import com.tropical.backend.auth.service.UserService;
import com.tropical.backend.schedule.entity.Schedule;
import com.tropical.backend.schedule.repository.ScheduleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

/**
 * 일정 관리 서비스 구현체
 *
 * @author 신동준
 * @version 0.1
 * @since 2025.09.15
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class ScheduleServiceImpl implements ScheduleService {

    private final ScheduleRepository scheduleRepository;
    private final UserService userService;

    /**
     * User ID와 함께 새로운 일정을 생성합니다
     */
    @Override
    @Transactional
    public Schedule createScheduleWithUserId(Long userId, Schedule schedule) {
        log.info("일정 생성 요청: 사용자 ID = {}, 제목 = {}", userId, schedule.getTitle());

        // User 엔티티 조회 (기존 UserService 메서드 사용)
        User user = userService.findActiveUserById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + userId));

        // Schedule에 User 설정
        Schedule scheduleWithUser = schedule.toBuilder()
                .user(user)
                .build();

        Schedule savedSchedule = scheduleRepository.save(scheduleWithUser);

        log.info("일정 생성 완료: ID = {}", savedSchedule.getId());
        return savedSchedule;
    }

    /**
     * 일정 ID와 사용자 ID로 일정을 조회합니다 (권한 검증 포함)
     */
    @Override
    public Schedule getScheduleByIdAndUserId(Long scheduleId, Long userId) {
        log.info("일정 조회 요청: 일정 ID = {}, 사용자 ID = {}", scheduleId, userId);

        Schedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> {
                    log.error("일정을 찾을 수 없습니다: ID = {}", scheduleId);
                    return new RuntimeException("일정을 찾을 수 없습니다: " + scheduleId);
                });

        // 사용자 권한 검증
        if (!schedule.getUser().getId().equals(userId)) {
            log.warn("일정 조회 권한 없음: 일정 ID = {}, 요청 사용자 ID = {}, 소유자 ID = {}",
                    scheduleId, userId, schedule.getUser().getId());
            throw new IllegalArgumentException("해당 일정에 접근할 권한이 없습니다");
        }

        return schedule;
    }

    /**
     * 사용자 권한을 검증하여 일정을 수정합니다
     */
    @Override
    @Transactional
    public Schedule updateScheduleByUserId(Long scheduleId, Long userId, Schedule schedule) {
        log.info("일정 수정 요청: 일정 ID = {}, 사용자 ID = {}", scheduleId, userId);

        Schedule existingSchedule = getScheduleByIdAndUserId(scheduleId, userId);

        // 기존 일정 업데이트 (Builder 패턴 사용)
        Schedule.ScheduleBuilder updatedBuilder = existingSchedule.toBuilder();

        if (schedule.getTitle() != null) {
            updatedBuilder.title(schedule.getTitle());
        }
        if (schedule.getMemo() != null) {
            updatedBuilder.memo(schedule.getMemo());
        }
        if (schedule.getScheduleDate() != null) {
            updatedBuilder.scheduleDate(schedule.getScheduleDate());
        }
        if (schedule.getStartTime() != null) {
            updatedBuilder.startTime(schedule.getStartTime());
        }
        if (schedule.getEndTime() != null) {
            updatedBuilder.endTime(schedule.getEndTime());
        }
        if (schedule.getLocation() != null) {
            updatedBuilder.location(schedule.getLocation());
        }
        if (schedule.getAttendees() != null) {
            updatedBuilder.attendees(schedule.getAttendees());
        }
        if (schedule.getIsCompleted() != null) {
            updatedBuilder.isCompleted(schedule.getIsCompleted());
        }

        Schedule updatedSchedule = updatedBuilder.build();
        Schedule savedSchedule = scheduleRepository.save(updatedSchedule);

        log.info("일정 수정 완료: ID = {}", savedSchedule.getId());
        return savedSchedule;
    }

    /**
     * 사용자 권한을 검증하여 일정을 삭제합니다
     */
    @Override
    @Transactional
    public void deleteScheduleByUserId(Long scheduleId, Long userId) {
        log.info("일정 삭제 요청: 일정 ID = {}, 사용자 ID = {}", scheduleId, userId);

        Schedule schedule = getScheduleByIdAndUserId(scheduleId, userId);
        scheduleRepository.delete(schedule);

        log.info("일정 삭제 완료: ID = {}", scheduleId);
    }

    /**
     * 사용자 권한을 검증하여 일정의 완료 상태를 토글합니다
     */
    @Override
    @Transactional
    public Schedule toggleScheduleCompletionByUserId(Long scheduleId, Long userId) {
        log.info("일정 완료 상태 토글 요청: 일정 ID = {}, 사용자 ID = {}", scheduleId, userId);

        Schedule schedule = getScheduleByIdAndUserId(scheduleId, userId);
        boolean newCompletionStatus = !schedule.getIsCompleted();

        Schedule updatedSchedule = schedule.toBuilder()
                .isCompleted(newCompletionStatus)
                .build();

        Schedule savedSchedule = scheduleRepository.save(updatedSchedule);

        log.info("일정 완료 상태 토글 완료: ID = {}, 완료 상태 = {}",
                scheduleId, newCompletionStatus);
        return savedSchedule;
    }

    // === 조회용 메서드들 ===

    @Override
    public List<Schedule> getSchedulesByUserId(Long userId) {
        log.info("사용자 일정 조회 요청: 사용자 ID = {}", userId);
        List<Schedule> schedules = scheduleRepository.findByUser_IdOrderByScheduleDateAscStartTimeAsc(userId);
        log.info("사용자 일정 조회 완료: 사용자 ID = {}, 일정 수 = {}", userId, schedules.size());
        return schedules;
    }

    @Override
    public List<Schedule> getSchedulesByDate(Long userId, LocalDate date) {
        log.info("날짜별 일정 조회 요청: 사용자 ID = {}, 날짜 = {}", userId, date);
        List<Schedule> schedules = scheduleRepository.findByUser_IdAndScheduleDateOrderByStartTimeAsc(userId, date);
        log.info("날짜별 일정 조회 완료: 사용자 ID = {}, 날짜 = {}, 일정 수 = {}", userId, date, schedules.size());
        return schedules;
    }

    @Override
    public List<Schedule> getSchedulesByMonth(Long userId, int year, int month) {
        log.info("월별 일정 조회 요청: 사용자 ID = {}, 연도 = {}, 월 = {}", userId, year, month);
        YearMonth yearMonth = YearMonth.of(year, month);
        LocalDate startDate = yearMonth.atDay(1);
        LocalDate endDate = yearMonth.atEndOfMonth();
        List<Schedule> schedules = scheduleRepository.findByUserIdAndScheduleDateBetween(userId, startDate, endDate);
        log.info("월별 일정 조회 완료: 사용자 ID = {}, {}-{}, 일정 수 = {}", userId, year, month, schedules.size());
        return schedules;
    }

    // === 기존 메서드들 (호환성 유지용 - 사용 안함) ===

    @Override
    @Transactional
    public Schedule createSchedule(Schedule schedule) {
        throw new UnsupportedOperationException("createScheduleWithUserId를 사용하세요");
    }

    @Override
    public Schedule getScheduleById(Long scheduleId) {
        throw new UnsupportedOperationException("getScheduleByIdAndUserId를 사용하세요");
    }

    @Override
    @Transactional
    public Schedule updateSchedule(Long scheduleId, Schedule schedule) {
        throw new UnsupportedOperationException("updateScheduleByUserId를 사용하세요");
    }

    @Override
    @Transactional
    public void deleteSchedule(Long scheduleId) {
        throw new UnsupportedOperationException("deleteScheduleByUserId를 사용하세요");
    }

    @Override
    @Transactional
    public Schedule toggleScheduleCompletion(Long scheduleId) {
        throw new UnsupportedOperationException("toggleScheduleCompletionByUserId를 사용하세요");
    }
}
