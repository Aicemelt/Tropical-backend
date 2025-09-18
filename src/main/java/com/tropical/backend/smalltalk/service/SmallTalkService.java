package com.tropical.backend.smalltalk.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import com.tropical.backend.smalltalk.entity.SmalltalkTopic;
import com.tropical.backend.smalltalk.enums.SourceType;
import com.tropical.backend.smalltalk.repository.SmalltalkTopicRepository;
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
import java.util.regex.Pattern;
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
    private final SmalltalkTopicRepository smalltalkTopicRepository;
    private final ObjectMapper objectMapper;

    private final ChatClient chatClient;

    private String USER_PROMPT;
    private static final String SYSTEM_TEMPLATE = """
            ## ROLE & GOAL
            당신은 Tropical 사용자를 위한 스몰토크 주제 추천 AI입니다.
            사용자가 제공한 활동 데이터(SCHEDULE, TODO, DIARY, BUCKET)나 정보가 없으면 일반적 관심사(GENERAL)를 참고하여,\s
            **사용자가 타인과 자연스럽게 나눌 수 있는 대화형 질문 주제를 추천**해야 합니다.
            대화 주제는 요청한 개수만큼 제공하면 됩니다. 
            
            ## 입력 데이터 형식
            사용자 데이터는 다음 JSON 형식으로 제공됩니다:
            
            **데이터가 있는 경우:**
            {
                "totalCount": 5,
                "activities": {
                    "SCHEDULE": [ ... ],
                    "TODO": [ ... ],
                    "DIARY": [ ... ],
                    "BUCKET": [ ... ]
                }
            }
            
            **데이터가 없는 경우:**
            {
                "totalCount": 5,
                "activities": {
                    "SCHEDULE": [],
                    "TODO": [],
                    "DIARY": [],
                    "BUCKET": []
                }
            }
            
            ## 기본 지침
            1. **상황 파악**: 시간대, 요일, 계절 등 고려
            2. **관심사 반영**: 취미, 관심 분야, 최근 경험 기반
            3. **데이터 활용**: 일정(SCHEDULE), 할일(TODO), 일기(DIARY), 버킷리스트(BUCKET) 활용
            4. **복합 주제 생성**: 자연스럽게 연결되는 경우만
            5. **데이터 없는 경우**: 보편적 주제로 추천
            6. **편안한 분위기**: 부담 없는 가벼운 스몰토크
            7. **대화형 의문문 강조**: exampleQuestion은 항상 타인과 나눌 수 있는 의문문 형태
            8. **일반화**: 특정 사용자 전용 활동은 타인과 공유할 수 있도록 일반화
            
            ## 주제 카테고리
            - 일상적: 오늘 하루, 영화/드라마, 음식, 날씨, 주말 계획
            - 관심사: 취미, 배우고 있는 것, 음악/책, 여행 경험, 동물
            - 생각거리: 재미있는 사실, 추억, 꿈/목표, 소소한 깨달음, 감사
            - 창의적: "만약에…" 상상 질문, 딜레마/선택 질문, 미래 상상, 아이디어 나누기
            - 복합 예시: 운동+건강식, 학습+여가, 여행+새 경험, 친구+취미 공유, 루틴+성장
            
            ## 응답 형식
            **반드시 JSON 배열로 요청된 개수만큼 주제 반환, 다른 텍스트 금지**
            
            [
                {
                    "topicType": "COMPLEX",
                    "topicContent": "헬스장 운동과 영어 학습 병행",
                    "exampleQuestion": "운동과 공부를 병행할 때 어떤 방법을 사용하시나요?",
                    "sources": [
                        { "sourceType": "SCHEDULE", "sourceId": 123 },
                        { "sourceType": "TODO", "sourceId": 456 }
                    ]
                },
                {
                    "topicType": "LIFESTYLE",
                    "topicContent": "카페 방문과 여행 계획",
                    "exampleQuestion": "최근 방문한 카페나 여행에서 추천할 만한 경험이 있나요?",
                    "sources": [
                        { "sourceType": "DIARY", "sourceId": 789 },
                        { "sourceType": "BUCKET", "sourceId": 101 }
                    ]
                },
                {
                    "topicType": "DAILY",
                    "topicContent": "독서 시간",
                    "exampleQuestion": "최근 읽은 책 중 추천할 만한 책이 있나요?",
                    "sources": [
                        { "sourceType": "TODO", "sourceId": 457 }
                    ]
                },
                {
                    "topicType": "GENERAL",
                    "topicContent": "계절 변화와 일상",
                    "exampleQuestion": "요즘 날씨 변화에 따라 즐기는 활동이 있나요?",
                    "sources": [
                        { "sourceType": "GENERAL", "sourceId": null }
                    ]
                }
            ]
           """;
    
    /**
     * AI 에게 주제 추천을 받을 요청 DTO를 생성하는 메소드 입니다.
     * @param email - 사용자 email
     * @return TopicGenerateRequest
     */
    public void makeAIRequest(String email) {

        User user = getUserByEmail(email);
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

        TopicGenerateRequest req = new TopicGenerateRequest(5, activities);

        String rawResponse = getTopic(req, userId);

        // 4. 생성된 주제를 db에 저장
        log.info("AI Raw Response: {}", rawResponse);

       try {
           List<AISmallTalkResponse> responses = objectMapper.readValue(rawResponse, new TypeReference<List<AISmallTalkResponse>>() {
           });

           log.info("json parse log: {}", responses);
           List<SmalltalkTopic> topics = responses.stream().map(
                           aiTopic -> SmalltalkTopic.toEntity(aiTopic, user)
                   )
                   .collect(Collectors.toList());

           smalltalkTopicRepository.saveAll(topics);

       } catch (Exception e) {
           throw new RuntimeException("json 파싱 실패");
       }

    }

    /**
     * 약관 동의 enum과 SourceType enum 을 매핑하는 기능입니다.
     * @param consent
     * @return
     */
    // 약관 동의 enum과 소스 타입 enum을 매핑하는 메소드
    private SourceType mapConsentToSourceType(UserConsent.ConsentType consent) {
        return switch (consent) {
            case DIARY_PERSONALIZATION -> DIARY;
            case TODO_PERSONALIZATION -> TODO;
            case BUCKET_PERSONALIZATION -> BUCKET;
            default -> throw new RuntimeException("");
        };
    }

    /**
     * AI 에게 스몰토크 주제를 요청하는 기능입니다.
     * @param req
     * @return response - AI 가 생성한 스몰토크 주제를 js
     */
    private String getTopic(TopicGenerateRequest req, Long userId) {

        // 1. 사용자가 받은 스몰톡 주제가 있는지 확인
        List<String> topics = smalltalkTopicRepository.findSmalltalkTopicsByUserId(userId).stream()
                .map(topic -> topic.getTopicContent())
                .collect(Collectors.toList());

        USER_PROMPT = "";
        if(!topics.isEmpty()) {
            USER_PROMPT = "\n\n이미 추천된 주제는 다음과 같습니다: "
                    + topics
                    + "\n이 주제들은 추천하지 않고 새로운 주제로 제시해주세요.";
        }

        try {
            String response = chatClient
                    .prompt()
                    .system(SYSTEM_TEMPLATE)
                    .user(u -> u.text(req.toString() + USER_PROMPT))
                    .call()
                    .content();
            ;

            return response;

        } catch (Exception e) {
            throw new RuntimeException("주제 추천 중 오류 발생");
        }
    }

    /**
     *
     * @param email
     * @return
     */
    private User getUserByEmail(String email) {
        return userRepository.findByEmail(email).orElseThrow(
                () -> new RuntimeException("유저 없음")
        );
    }

}
