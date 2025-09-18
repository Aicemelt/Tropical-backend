package com.tropical.backend.smalltalk.service;

import com.tropical.backend.auth.entity.User;
import com.tropical.backend.auth.entity.UserConsent;
import com.tropical.backend.auth.repository.UserRepository;
import com.tropical.backend.bucketList.entity.BucketList;
import com.tropical.backend.bucketList.repository.BucketListRepository;
import com.tropical.backend.diary.entity.Diary;
import com.tropical.backend.diary.repository.DiaryRepository;
import com.tropical.backend.schedule.entity.Schedule;
import com.tropical.backend.schedule.repository.ScheduleRepository;
import com.tropical.backend.smalltalk.dto.request.ActivityDto;
import com.tropical.backend.smalltalk.dto.request.TopicGenerateRequest;
import com.tropical.backend.smalltalk.enums.SourceType;
import com.tropical.backend.todo.entity.Todo;
import com.tropical.backend.todo.repository.TodoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
@Rollback(value = false)
class SmallTalkServiceTest {

    @Autowired
    SmallTalkService smallTalkService;

    @Autowired
    UserRepository userRepository;

    @Autowired
    TodoRepository todoRepository;

    @Autowired
    DiaryRepository diaryRepository;

    @Autowired
    BucketListRepository bucketListRepository;

    @Autowired
    ScheduleRepository scheduleRepository;

    @BeforeEach
    void setUp() {
        
        User testUser = User.builder()
                .email("testuser@example.com")
                .passwordHash("testpasswordhash")
                .nickname("테스트유저")
                .accountType(User.AccountType.LOCAL)
                .emailVerified(true)
                .emailVerifiedAt(LocalDateTime.now())
                .onboardingCompleted(true)
                .build();

        userRepository.save(testUser);

        // 2. 필수 동의 생성
        UserConsent termsConsent = UserConsent.createUserConsent(testUser, UserConsent.ConsentType.TERMS_OF_SERVICE, true);
        UserConsent privacyConsent = UserConsent.createUserConsent(testUser, UserConsent.ConsentType.PRIVACY_POLICY, true);
        UserConsent calendarConsent = UserConsent.createUserConsent(testUser, UserConsent.ConsentType.CALENDAR_PERSONALIZATION, true);

        // 양방향 연결
        termsConsent.setUser(testUser);
        privacyConsent.setUser(testUser);
        calendarConsent.setUser(testUser);

        // 3. 선택 동의 생성
        UserConsent diaryConsent = UserConsent.createUserConsent(testUser, UserConsent.ConsentType.DIARY_PERSONALIZATION, true);
        UserConsent todoConsent = UserConsent.createUserConsent(testUser, UserConsent.ConsentType.TODO_PERSONALIZATION, true);
        UserConsent bucketConsent = UserConsent.createUserConsent(testUser, UserConsent.ConsentType.BUCKET_PERSONALIZATION, true);
        diaryConsent.setUser(testUser);

        // 4. 일정 생성
        Schedule testSchedule = Schedule.builder()
                .user(testUser)
                .title("테스트 일정")
                .memo("테스트 일정 메모")
                .scheduleDate(LocalDate.now())
                .startTime(LocalTime.of(10, 0))
                .endTime(LocalTime.of(11, 0))
                .location("테스트 장소")
                .isCompleted(false)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        Schedule testSchedule2 = Schedule.builder()
                .user(testUser)
                .title("테스트 일정ddd")
                .memo("테스트 일정 메모")
                .scheduleDate(LocalDate.now())
                .startTime(LocalTime.of(10, 0))
                .endTime(LocalTime.of(11, 0))
                .location("테스트 장소")
                .isCompleted(false)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        // 5. 유저에 일정 연결
        testUser.getSchedules().addAll(List.of(
                testSchedule, testSchedule2
        ));

        // 6. todo 생성
        Todo todo = Todo.builder()
                .user(testUser)
                .content("귀찮긔윤")
                .dueDate(LocalDate.now())
                .isCompleted(false)
                .build();

        testUser.getTodos().add(todo);

        // 7. 버킷생성
        BucketList bucket = BucketList.builder()
                .user(testUser)
                .content("AI 되기")
                .build();

        testUser.getBucketLists().add(bucket);

        // 8. 다이어리 생성
        Diary diary = Diary.builder()
                .user(testUser)
                .title("제목")
                .content("어쩔")
                .diaryDate(LocalDate.now())
                .build();

        testUser.getDiaries().add(diary);

        // 확인용 출력
        System.out.println("테스트 유저: " + testUser);
        System.out.println("테스트 일정: " + testSchedule);
        System.out.println("테스트 동의: " + List.of(termsConsent, privacyConsent, calendarConsent, diaryConsent));
    }
    
    @Test
    @DisplayName("ai 요청 dto 생성 테스트")
    void makeRequestDto() {
        // given
        String email = "testuser@example.com";
        // when
        TopicGenerateRequest topic = smallTalkService.makeAIRequest(email);
        // then
        System.out.println("topic = " + topic);
    }

    @Test
    @DisplayName("ai 주제 추천 테스트")
    void aiGetTopic() {
        // given
        TopicGenerateRequest dto = new TopicGenerateRequest(5, Map.of(
                SourceType.BUCKET, List.of(
                        new ActivityDto(1L, "", "ai 되기", LocalDateTime.now())
                ),
                SourceType.TODO, List.of(
                        new ActivityDto(1L, "", "비타민 먹기", LocalDateTime.now())
                ),
                SourceType.SCHEDULE, List.of(
                        new ActivityDto(1L, "집가기", "집에가고싶다", LocalDateTime.now())
                )
        ));
        // when
        String topic = smallTalkService.getTopic(dto);
        // then
        System.out.println("topic = " + topic);
    }
    
}