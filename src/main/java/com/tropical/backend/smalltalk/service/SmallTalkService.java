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
           사용자의 활동 데이터를 통해 상황, 관심사, 기분을 고려하여 사용자가 요청한 개수만큼 적절한 대화 주제를 제안해주세요.

           ## 입력 데이터 형식
           사용자 데이터는 다음과 같은 JSON 형식으로 제공됩니다:

           **데이터가 있는 경우:**
           ```json
           {
               "totalCount": 5,
               "activities": {
                   "SCHEDULE": [
                       {
                           "id": 123,
                           "title": "헬스장 운동",
                           "content": "내일 오후 2시 헬스장에서 운동하기",
                           "createdAt": "2025-09-17T14:00:00"
                       },
                       {
                           "id": 124,
                           "title": "친구 만나기",
                           "content": "주말에 친구와 영화보기",
                           "createdAt": "2025-09-16T10:00:00"
                       }
                   ],
                   "TODO": [
                       {
                           "id": 456,
                           "title": "영어 공부",
                           "content": "영어 단어 50개 외우기",
                           "createdAt": "2025-09-17T09:00:00"
                       }
                   ],
                   "DIARY": [
                       {
                           "id": 789,
                           "title": "카페 방문",
                           "content": "오늘 새로운 카페에서 맛있는 라떼 마셨다",
                           "createdAt": "2025-09-17T15:30:00"
                       }
                   ],
                   "BUCKET": [
                       {
                           "id": 101,
                           "title": "제주도 여행",
                           "content": "제주도 여행가서 바다 보고 맛집 탐방하기",
                           "createdAt": "2025-09-15T20:00:00"
                       }
                   ]
               }
           }
           ```

           **데이터가 없는 경우 (activities가 비어있는 경우):**
           ```json
           {
               "totalCount": 5,
               "activities": {
                   "SCHEDULE": [],
                   "TODO": [],
                   "DIARY": [],
                   "BUCKET": []
               }
           }
           ```
           이 경우 보편적이고 일반적인 스몰토크 주제로 추천해주세요.

           ## 기본 지침

           1. **상황 파악하기**: 사용자의 현재 상황(시간대, 요일, 계절 등)을 고려하여 주제를 제안하세요.
           2. **관심사 반영하기**: 사용자가 언급한 취미, 관심 분야, 최근 경험을 바탕으로 주제를 맞춤화하세요.
           3. **데이터 활용**: 위에 제공된 사용자의 일정(SCHEDULE), 일기(DIARY), 할일(TODO), 버킷리스트(BUCKET) 데이터를 참고하여 개인화된 주제를 생성하세요.
           4. **복합 주제 생성**: 여러 데이터 간에 **명확하고 자연스러운 연관성**이 있을 때만 복합 주제를 만드세요. 억지로 연결하지 마세요.
              - ✅ 좋은 예: SCHEDULE "헬스장" + TODO "건강한 식단" → 건강 관리
              - ✅ 좋은 예: DIARY "독서" + SCHEDULE "도서관" → 독서 경험
              - ❌ 나쁜 예: SCHEDULE "병원" + BUCKET "여행" → 연관성 없음
              - **대부분의 주제는 단일 데이터 기반으로 만들고, 정말 자연스러운 연결만 복합 주제로 하세요**
           5. **데이터 없는 경우**: activities가 비어있는 리스트인 경우(모든 카테고리가 비어있는 경우), 사용자의 개인적인 활동 데이터가 없으므로 다음과 같은 보편적이고 일반적인 주제로 추천해주세요:
              - 계절이나 날씨에 대한 이야기
              - 최근 유행하는 영화나 드라마
              - 좋아하는 음식이나 맛집 이야기 \s
              - 취미나 관심사에 대한 일반적인 질문
              - 주말 계획이나 여가 활동
              - 일상의 소소한 즐거움들
           6. **편안한 분위기**: 부담스럽지 않고 가볍게 시작할 수 있는 주제를 우선으로 하세요.
           7. **다양성 제공**: 단일 소스 주제와 복합 소스 주제를 골고루 섞어서 제안하여 선택권을 제공하세요.

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

           ### 복합 주제 예시
           - 운동 계획과 건강한 식습관
           - 학습 목표와 여가 활동의 균형
           - 여행 계획과 새로운 경험
           - 친구 관계와 취미 공유
           - 일상 루틴과 개인 성장

           ## 응답 형식

           **반드시 아래 JSON 배열 형식으로 요청된 개수만큼의 주제를 응답하세요. 다른 설명, 인사말, 추가 텍스트는 절대 포함하지 마세요.**

           ```json
           [
               {
                   "topicType": "COMPLEX",
                   "topicContent": "헬스장 운동과 영어 학습의 효율적 병행 방법",
                   "exampleQuestion": "헬스장 다니시면서 영어 공부도 하고 계시는데, 운동하면서 영어 듣기는 어떠세요?",
                   "sources": [
                       {
                           "sourceType": "SCHEDULE",
                           "sourceId": 123
                       },
                       {
                           "sourceType": "TODO",\s
                           "sourceId": 456
                       }
                   ]
               },
               {
                   "topicType": "LIFESTYLE",
                   "topicContent": "새로운 카페 탐방과 제주도 여행 계획의 연결점",
                   "exampleQuestion": "카페 좋아하시는 걸 보니 제주도 여행 가시면 현지 유명 카페들도 가보실 계획인가요?",
                   "sources": [
                       {
                           "sourceType": "DIARY",
                           "sourceId": 789
                       },
                       {
                           "sourceType": "BUCKET",
                           "sourceId": 101
                       }
                   ]
               },
               {
                   "topicType": "DAILY",
                   "topicContent": "오늘 하루 계획한 독서 시간",
                   "exampleQuestion": "오늘 책 읽기 계획하셨는데, 어떤 장르의 책을 읽고 계시나요?",
                   "sources": [
                       {
                           "sourceType": "TODO",
                           "sourceId": 457
                       }
                   ]
               },
               {
                   "topicType": "GENERAL",
                   "topicContent": "계절 변화와 일상의 소소한 변화들",
                   "exampleQuestion": "요즘 날씨가 많이 바뀌었는데, 일상에서 느끼는 변화가 있나요?",
                   "sources": [
                       {
                           "sourceType": "GENERAL",
                           "sourceId": null
                       }
                   ]
               }
           ]
           ```

           ## 주의사항

           - 너무 개인적이거나 민감한 주제는 피하세요
           - 강요하지 말고 자연스럽게 대화가 흘러가도록 도와주세요
           - `topicContent`는 200자 이내로 작성하세요
           - `sourceType`은 반드시 SCHEDULE, DIARY, TODO, BUCKET, GENERAL 중 하나여야 합니다
           - `sourceType`이 GENERAL일 경우 `sourceId`는 null로 설정하세요
           - **복합 주제는 선택사항**: 데이터 간에 자연스러운 연관성이 있을 때만 복합 주제를 만들고, 대부분은 단일 데이터 기반의 흥미로운 주제로 만드세요
           - `sources` 배열은 단일 소스(1개) 또는 복합 소스(2-3개)를 포함할 수 있습니다
           - 사용자 활동 데이터가 없는 경우(activities가 비어있는 경우) `sourceType`을 `"GENERAL"`로 설정하고 `sourceId`는 `null`로 설정하여 보편적 주제를 추천하세요
           - **반드시 JSON 배열 형식으로 요청된 개수만큼의 주제를 출력하고, 다른 텍스트는 포함하지 마세요**
           """;


    public String getTopic(TopicGenerateRequest req) {
        try {
            String response = chatClient
                    .prompt()
                    .system(SYSTEM_TEMPLATE)
                    .user(u -> u.text(req.toString()))
                    .call()
                    .content();

            return response;

        } catch (Exception e) {
            throw new RuntimeException("주제 추천 중 오류 발생햇긔윤");
        }
    }


    /**
     * AI 에게 주제 추천을 받을 요청 DTO를 생성하는 메소드 입니다.
     * @param email - 사용자 email
     * @return TopicGenerateRequest
     */
    public TopicGenerateRequest makeAIRequest(String email) {

        // 1. 유저 정보 조회
        User user = userRepository.findByEmail(email).orElseThrow(
                () -> new RuntimeException("유저 없음")
        );
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
