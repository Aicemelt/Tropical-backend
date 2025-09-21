package com.tropical.backend.smalltalk.service;

import com.tropical.backend.auth.entity.User;
import com.tropical.backend.auth.repository.UserRepository;
import com.tropical.backend.schedule.entity.Schedule;
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

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = {
        "spring.mail.host=localhost"  // 간단한 더미값만
})
@Transactional
@Rollback(value = false)
class SmalltalkAIServiceTest {

    @Autowired
    SmalltalkAIService smalltalkAIService;

    @Autowired
    UserRepository userRepository;

    @BeforeEach
    void setUp() {
        /*User testUser = User.builder()
                .email("testuser@example.com")
                .passwordHash("testpasswordhash")
                .nickname("테스트유저")
                .accountType(User.AccountType.LOCAL)
                .emailVerified(true)
                .emailVerifiedAt(LocalDateTime.now())
                .onboardingCompleted(true)
                .build();

        userRepository.save(testUser);

        User testUser = userRepository.findById(1L).orElseThrow();

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
        ));*/
    }


    @Test
    @DisplayName("ai 주제 요청 테스트")
    void generateAITopics() {
        // given
        User user = userRepository.findById(1L).orElseThrow();
        // when
        String savedTopics = smalltalkAIService.generateSmallTalk(user);
        // then
        System.out.println("savedTopics = " + savedTopics);
    }

}