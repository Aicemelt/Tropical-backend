package com.tropical.backend.smalltalk.service;

import com.tropical.backend.auth.entity.User;
import com.tropical.backend.auth.repository.UserRepository;
import com.tropical.backend.smalltalk.entity.SmalltalkTopic;
import com.tropical.backend.smalltalk.repository.SmalltalkTopicRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import javax.swing.text.html.parser.Entity;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest(properties = {
        "schedules.cron.reward.publish=*/5 * * * * *", // 5초마다
        "spring.task.scheduling.enabled=true",
        "spring.task.scheduling.pool.size=1",
        "schedules.delay.ms=2000"
})
class SmallTalkSchedulerTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SmalltalkTopicRepository smalltalkTopicRepository;

    @Autowired
    private SmallTalkScheduler scheduler;


    private User testUser;

    @BeforeEach
    @Transactional
    @Rollback(false)
    void setUp() {
        //userRepository.deleteAll(); // 매번 DB 초기화
        // smalltalkTopicRepository.deleteAll();

        testUser = userRepository.saveAndFlush(
                User.builder()
                    .email("testuser@example.com")
                    .passwordHash("testpasswordhash")
                    .nickname("테스트유저")
                    .accountType(User.AccountType.LOCAL)
                    .emailVerified(true)
                    .emailVerifiedAt(LocalDateTime.now())
                    .onboardingCompleted(true)
                    .build()
        );


        smalltalkTopicRepository.deleteAll();
    }

    @AfterEach
    @Transactional
    @Rollback(false)
    /*void tearDown() {
        // 테스트 데이터 정리
        smalltalkTopicRepository.deleteAll();
        userRepository.deleteAll();
    }*/

    @Test
    @DisplayName("스케줄러 실행 시 사용자별로 주제가 생성된다")
    void scheduler_creates_topic_for_user() {
        int before = smalltalkTopicRepository.findSmalltalkTopicsByUserId(testUser.getId(), 100).size();
        scheduler.generateDailyTopics();
        int after = smalltalkTopicRepository.findSmalltalkTopicsByUserId(testUser.getId(), 100).size();

        assertThat(after).isGreaterThan(before);
    }

    @Test
    @DisplayName("여러 사용자 모두에게 주제가 생성된다")
    void scheduler_creates_topics_for_multiple_users() {
        User user2 = userRepository.save(
                User.builder()
                        .email("test2@example.com")
                        .passwordHash("testpasswordhash")
                        .nickname("테스트유저2")
                        .accountType(User.AccountType.LOCAL)
                        .emailVerified(true)
                        .emailVerifiedAt(LocalDateTime.now())
                        .onboardingCompleted(true)
                        .build()
        );

        int before1 = smalltalkTopicRepository.findSmalltalkTopicsByUserId(testUser.getId(), 100).size();
        int before2 = smalltalkTopicRepository.findSmalltalkTopicsByUserId(user2.getId(), 100).size();

        scheduler.generateDailyTopics();

        assertThat(smalltalkTopicRepository.findSmalltalkTopicsByUserId(testUser.getId(), 100))
                .hasSizeGreaterThan(before1);
        assertThat(smalltalkTopicRepository.findSmalltalkTopicsByUserId(user2.getId(), 100))
                .hasSizeGreaterThan(before2);
    }

    @Test
    @DisplayName("예외 발생해도 스케줄러는 중단되지 않는다")
    void scheduler_handles_exceptions() {
        userRepository.save(
                User.builder()
                        .email("testuser@example.com")
                        .passwordHash("testpasswordhash")
                        .nickname("테스트유저")
                        .accountType(User.AccountType.LOCAL)
                        .emailVerified(true)
                        .emailVerifiedAt(LocalDateTime.now())
                        .onboardingCompleted(true)
                        .build()
        );
        assertThatCode(() -> scheduler.generateDailyTopics()).doesNotThrowAnyException();
    }

    @Test
    @org.springframework.transaction.annotation.Transactional(propagation = org.springframework.transaction.annotation.Propagation.NOT_SUPPORTED)
    @DisplayName("실제 스케줄링이 돌아 DB에 주제가 늘어난다")
    void scheduler_runs_and_increases_topics() throws Exception {
        // 1) 사전 데이터 커밋
        User user = userRepository.findByEmail("testuser@example.com").orElseThrow();
        int before = smalltalkTopicRepository.findAllSmalltalkTopicsByUserId(user.getId()).size();

        // 2) 12초 동안 매초 변화 관찰
        long deadline = System.currentTimeMillis() + 12_000;
        boolean increased = false;
        while (System.currentTimeMillis() < deadline) {
            Thread.sleep(1_000);
            int now = smalltalkTopicRepository.findAllSmalltalkTopicsByUserId(user.getId()).size();
            if (now > before) { increased = true; break; }
        }

        org.assertj.core.api.Assertions.assertThat(increased).isTrue();
    }
}
