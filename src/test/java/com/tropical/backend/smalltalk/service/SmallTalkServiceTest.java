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
import com.tropical.backend.smalltalk.dto.response.TopicResponse;
import com.tropical.backend.smalltalk.enums.SourceType;
import com.tropical.backend.todo.entity.Todo;
import com.tropical.backend.todo.repository.TodoRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.mail.MailSenderAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import javax.swing.text.html.parser.Entity;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = {
        "spring.mail.host=localhost"  // 간단한 더미값만
})
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

    @Autowired
    EntityManager em;

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
       /* UserConsent termsConsent = UserConsent.createUserConsent(testUser, UserConsent.ConsentType.TERMS_OF_SERVICE, true);
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
                .title("뮤지컬 관극")
                .memo("한복입은남자, 전동석, 카이")
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
                .title("정보처리기사 실기 시험")
                .memo("정보처리기사 실기 시험")
                .scheduleDate(LocalDate.now())
                .startTime(LocalTime.of(10, 0))
                .endTime(LocalTime.of(11, 0))
                .location("테스트 장소")
                .isCompleted(false)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        Schedule testSchedule3 = Schedule.builder()
                .user(testUser)
                .title("도쿄 여행")
                .memo("친구랑 도쿄 여행감")
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
                testSchedule, testSchedule2, testSchedule3
        ));

        // 6. todo 생성
        Todo todo = Todo.builder()
                .user(testUser)
                .content("백엔드 개발 완료")
                .dueDate(LocalDate.now())
                .isCompleted(false)
                .build();

        testUser.getTodos().add(todo);

        // 7. 버킷생성
        BucketList bucket = BucketList.builder()
                .user(testUser)
                .content("뉴욕 여행가기")
                .build();

        testUser.getBucketLists().add(bucket);

        // 8. 다이어리 생성
        Diary diary = Diary.builder()
                .user(testUser)
                .title("힘들다")
                .content("어제 진짜 알바 너무 힘들었다. 한시간도 앉질 못했어 사람들 왜 이렇게 베라를 좋아함? 아 개빡쳐")
                .diaryDate(LocalDate.now())
                .build();

        testUser.getDiaries().add(diary);

        // 확인용 출력
        System.out.println("테스트 유저: " + testUser);
        System.out.println("테스트 일정: " + testSchedule);
        System.out.println("테스트 동의: " + List.of(termsConsent, privacyConsent, calendarConsent, diaryConsent));


          */
        em.flush();
        em.clear();
    }

    @Test
    @DisplayName("회원가입 후 웰컴 주제 반환 테스트")
    void returnWelcomeTopic() {
        // given
        String email = "testuser@example.com";
        // when
        // TopicResponse smallTalks = smallTalkService.getSmallTalks(email);
        // then
        // System.out.println("smallTalks = " + smallTalks);
    }
    
    @Test
    @DisplayName("ai 요청 dto 생성 테스트")
    void makeRequestDto() {
        // given
        String email = "testuser@example.com";
        // when
        // smallTalkService.makeAIRequest(email);
        // em.flush();
        // em.clear();
        // then
        // System.out.println("topic = " + topic);
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
        // String topic = smallTalkService.getTopic(dto);
        // then
        // System.out.println("topic = " + topic);
    }

    @Test
    @DisplayName("ai 주제 생성 통합 테스트")
    void aiGenerateTopicTest() {
        // given
        String email = "testuser@example.com";
        // when
         // smallTalkService.generateSmallTalk(email);
        // then
    }

    @Test
    @DisplayName("유저생성테스트")
    void createUser() {
        // given
        // when
        // smallTalkService.generateSmallTalk(email);
        // then
    }
    
}
