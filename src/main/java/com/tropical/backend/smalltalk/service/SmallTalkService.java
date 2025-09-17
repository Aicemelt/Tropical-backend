package com.tropical.backend.smalltalk.service;

import com.tropical.backend.auth.entity.User;
import com.tropical.backend.auth.entity.UserConsent;
import com.tropical.backend.auth.repository.UserConsentRepository;
import com.tropical.backend.auth.repository.UserRepository;
import com.tropical.backend.bucketList.repository.BucketListRepository;
import com.tropical.backend.diary.entity.Diary;
import com.tropical.backend.diary.repository.DiaryRepository;
import com.tropical.backend.schedule.entity.Schedule;
import com.tropical.backend.schedule.repository.ScheduleRepository;
import com.tropical.backend.smalltalk.dto.request.ActivityDto;
import com.tropical.backend.smalltalk.dto.request.TopicGenerateRequest;
import com.tropical.backend.smalltalk.dto.response.AISmallTalkResponse;
import com.tropical.backend.smalltalk.enums.SourceType;
import com.tropical.backend.todo.repository.TodoRepository;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static com.tropical.backend.smalltalk.enums.SourceType.*;

@Transactional
@Service
@RequiredArgsConstructor
@Slf4j
public class SmallTalkService {

    private final UserRepository userRepository;
    private final TodoRepository todoRepository;
    private final ScheduleRepository scheduleRepository;
    private final DiaryRepository diaryRepository;
    private final BucketListRepository bucketListRepository;
    private final UserConsentRepository userConsentRepository;

    private final ChatClient chatClient;

    private static final String SYSTEM_TEMPLATE = """
            당신은 사용자와 자연스럽고 편안한 대화를 이어갈 수 있는 스몰토크 주제를 추천하는 어시스턴트입니다.
            사용자의 상황, 관심사, 기분을 고려하여 적절한 대화 주제를 제안해주세요.

            ## 기본 지침

            1. **상황 파악하기**: 사용자의 현재 상황(시간대, 요일, 계절 등)을 고려하여 주제를 제안하세요.
            2. **관심사 반영하기**: 사용자가 언급한 취미, 관심 분야, 최근 경험을 바탕으로 주제를 맞춤화하세요.
            3. **데이터 활용**: 제공된 사용자의 일정(SCHEDULE), 일기(DIARY), 할일(TODO), 버킷리스트(BUCKET) 데이터를 참고하여 개인화된 주제를 생성하세요.
            4. **편안한 분위기**: 부담스럽지 않고 가볍게 시작할 수 있는 주제를 우선으로 하세요.
            5. **다양성 제공**: 여러 카테고리의 주제를 제안하여 선택권을 제공하세요.

            ## 주제 카테고리

            ### 일상적 주제
            - 오늘 하루 어땠는지
            - 최근 본 영화나 드라마
            - 좋아하는 음식이나 최근 먹어본 것
            - 날씨나 계절에 대한 이야기
            - 주말 계획이나 여가 활동

            ### 가벼운 관심사
            - 취미나 관심 분야
            - 최근 배우고 있는 것
            - 좋아하는 음악이나 책
            - 여행 경험이나 가고 싶은 곳
            - 펫이나 동물에 대한 이야기

            ### 생각거리 주제
            - 재미있는 사실이나 지식
            - 어린 시절 추억
            - 꿈이나 목표
            - 최근 느낀 소소한 깨달음
            - 인생에서 감사한 것들

            ### 창의적 주제
            - "만약에..." 상상 질문
            - 재미있는 딜레마나 선택 질문
            - 미래에 대한 상상
            - 창의적인 아이디어 나누기

            ## 응답 형식

            **반드시 아래 JSON 형식으로만 응답하세요. 다른 설명, 인사말, 추가 텍스트는 절대 포함하지 마세요.**

            ```json
            {
                "topicType": "주제_카테고리",
                "topicContent": "대화_주제_내용",
                "exampleQuestion": "구체적인_질문_예시",
                "sources": [
                    {
                        "sourceType": "SCHEDULE",
                        "sourceId": 123
                    }
                ]
            }
            ```

            ## 주의사항

            - 너무 개인적이거나 민감한 주제는 피하세요
            - `topicContent`는 200자 이내로 작성하세요
            - `sourceType`은 반드시 SCHEDULE, DIARY, TODO, BUCKET 중 하나여야 합니다
            - **오직 JSON 데이터만 출력하고 다른 텍스트는 포함하지 마세요**
            """;

    /**
     * AI 에게 주제 추천을 받을 요청 DTO를 생성하는 메소드 입니다.
     * @param email - 사용자 email
     * @return TopicGenerateRequest
     */
    public TopicGenerateRequest getTopic(String email) {

        // 1. 유저 정보 조회
        User user = userRepository.findByEmail(email).orElseThrow();
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
                        new ActivityDto(schedule.getTitle(), schedule.getMemo(), schedule.getCreatedAt()))
                .collect(Collectors.toList());


        activities.put(SCHEDULE, schedules);

        // 3-2. 동의한 db로 내용 조회, ActivityDto 매핑
        Map<SourceType, Supplier<List<ActivityDto>>> suppliers = Map.of(
                DIARY, () -> diaryRepository.findByUserIdAndDiaryDateBetween(userId, startDate, endDate)
                        .stream()
                        .map(diary -> new ActivityDto(diary.getTitle(), diary.getContent(), diary.getCreatedAt()))
                        .collect(Collectors.toList()),

                TODO, () -> todoRepository.findByUserAndDueDateBetween(user, startDate, endDate)
                        .stream()
                        .map(todo -> new ActivityDto("", todo.getContent(), todo.getCreatedAt()))
                        .collect(Collectors.toList()),

                BUCKET, () -> bucketListRepository.findByUserOrderByCreatedAtDesc(user)
                        .stream()
                        .map(bucket -> new ActivityDto("", bucket.getContent(), bucket.getCreatedAt()))
                        .collect(Collectors.toList())
        );


        agreeConsentList.forEach(consent -> {
            Supplier<List<ActivityDto>> supplier = suppliers.get(mapConsentToSourceType(consent));
            if (supplier != null) {
                activities.put(mapConsentToSourceType(consent), supplier.get());
            }
        });

        return new TopicGenerateRequest(7, activities);
    }

    // 약관 동의 enum과 소스 타입 enum을 매핑하는 메소드
    private SourceType mapConsentToSourceType(UserConsent.ConsentType consent) {
        return switch (consent) {
            case DIARY_PERSONALIZATION -> DIARY;
            case TODO_PERSONALIZATION -> TODO;
            case BUCKET_PERSONALIZATION -> BUCKET;
            default -> throw new RuntimeException("");
        };
    }

}
