package com.tropical.backend.smalltalk.service;

import com.tropical.backend.auth.entity.User;
import com.tropical.backend.auth.entity.UserConsent;
import com.tropical.backend.auth.repository.UserConsentRepository;
import com.tropical.backend.auth.repository.UserRepository;
import com.tropical.backend.bucketList.repository.BucketListRepository;
import com.tropical.backend.diary.repository.DiaryRepository;
import com.tropical.backend.schedule.entity.Schedule;
import com.tropical.backend.schedule.repository.ScheduleRepository;
import com.tropical.backend.smalltalk.dto.request.ActivityDto;
import com.tropical.backend.smalltalk.dto.request.TopicGenerateRequest;
import com.tropical.backend.smalltalk.enums.SourceType;
import com.tropical.backend.todo.repository.TodoRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static com.tropical.backend.smalltalk.enums.SourceType.*;

@Service
@Transactional
@RequiredArgsConstructor
public class UserActivityService {

    private final UserRepository userRepository;
    private final TodoRepository todoRepository;
    private final ScheduleRepository scheduleRepository;
    private final DiaryRepository diaryRepository;
    private final BucketListRepository bucketListRepository;
    private final UserConsentRepository userConsentRepository;

    /**
     * AI 에게 주제 추천을 요청하는 DTO 를 만드는 메소드 입니다.
     * @param user
     * @param count
     * @return TopicGenerateRequest dto
     */
    public TopicGenerateRequest makeAIRequestDto(User user, int count) {

        // 1. 유저 id 획득
        Long userId = user.getId();

        // 2. 유저 id로 약관동의한 내용 조회
        List<UserConsent.ConsentType> agreeConsentList = userConsentRepository.findAgreedOptionalConsentTypes(userId);
        /*
         * [일기, 투두, 버킷]
         */

        // 3. 동의한 db & 일정 내용을 조회, dto 생성
        // 3-0. 날짜 범위 설정 (startDate: 현재 날짜 기준 7일 전 / EndDate: 현재 날짜)
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(7);

        Map<SourceType, List<ActivityDto>> activities = new HashMap<>();

        // 3-1. 일정 조회, ActivityDto 매핑
        List<Schedule> scheduleList = scheduleRepository.findByUserIdAndScheduleDateBetween(userId, startDate, endDate);
        List<ActivityDto> schedules = scheduleList.stream()
                .map(schedule ->
                        new ActivityDto(schedule.getId(), schedule.getTitle(), schedule.getMemo(), schedule.getCreatedAt()))
                .collect(Collectors.toList());


        activities.put(SCHEDULE, schedules);

        // 3-2. 동의한 db로 내용 조회, ActivityDto 매핑
        Map<SourceType, Supplier<List<ActivityDto>>> suppliers = Map.of(
                DIARY, () -> diaryRepository.findByUserIdAndDiaryDateBetween(userId, startDate, endDate)
                        .stream()
                        .map(diary -> new ActivityDto(diary.getId(), diary.getTitle(), diary.getContent(), diary.getCreatedAt()))
                        .collect(Collectors.toList()),

                TODO, () -> todoRepository.findByUserAndDueDateBetween(user, startDate, endDate)
                        .stream()
                        .map(todo -> new ActivityDto(todo.getTodoId(), "", todo.getContent(), todo.getCreatedAt()))
                        .collect(Collectors.toList()),

                BUCKET, () -> bucketListRepository.findByUserOrderByCreatedAtDesc(user)
                        .stream()
                        .map(bucket -> new ActivityDto(bucket.getBucketId(), "", bucket.getContent(), bucket.getCreatedAt()))
                        .collect(Collectors.toList())
        );


        agreeConsentList.forEach(consent -> {
            Supplier<List<ActivityDto>> supplier = suppliers.get(mapConsentToSourceType(consent));
            if (supplier != null) {
                activities.put(mapConsentToSourceType(consent), supplier.get());
            }
        });

        return new TopicGenerateRequest(count, activities);

    }

    /**
     * 사용자의 활동기록을 확인하여 하나라도 있으면 true 를 반환,
     * 활동기록이 모두 없으면 false 를 반환하는 메소드 입니다.
     * @param userId
     * @param user
     * @return boolean
     */
    public boolean hasActivity(Long userId, User user) {

        boolean todo = todoRepository.existsById(userId);
        boolean bucket = bucketListRepository.existsByUser(user);
        boolean diary = diaryRepository.existsById(userId);
        boolean schedule = scheduleRepository.existsById(userId);

        return todo || bucket || diary || schedule;
    }

    /**
     * 약관 동의 enum 과 SourceType enum 을 매핑하는 기능입니다.
     * @param consent
     * @return SourceType
     */
    // 약관 동의 enum 과 소스 타입 enum 을 매핑하는 메소드
    public SourceType mapConsentToSourceType(UserConsent.ConsentType consent) {
        return switch (consent) {
            case DIARY_PERSONALIZATION -> DIARY;
            case TODO_PERSONALIZATION -> TODO;
            case BUCKET_PERSONALIZATION -> BUCKET;
            default -> throw new RuntimeException("");
        };
    }
}
