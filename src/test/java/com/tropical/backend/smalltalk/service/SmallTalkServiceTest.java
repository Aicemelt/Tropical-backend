package com.tropical.backend.smalltalk.service;

import com.tropical.backend.auth.entity.User;
import com.tropical.backend.auth.repository.UserRepository;
import com.tropical.backend.bucketList.repository.BucketListRepository;
import com.tropical.backend.diary.repository.DiaryRepository;
import com.tropical.backend.schedule.repository.ScheduleRepository;
import com.tropical.backend.todo.repository.TodoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

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
    void insertBulk() {
        // 유저정보 만들기 (임시 테스트용)
        User.builder()

                .build();
    }
}