package com.tropical.backend.smalltalk.service;

import com.tropical.backend.auth.entity.User;
import groovy.transform.CompileDynamic;
import groovy.util.logging.Slf4j;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

@lombok.extern.slf4j.Slf4j
@CompileDynamic
@Component
@RequiredArgsConstructor
@Slf4j
public class SmallTalkScheduler {

    private final UserReadService userReadService;
    private final SmalltalkAIService smalltalkAIService;

    private final AtomicBoolean running = new AtomicBoolean(false);

    // cron = "초 분 시간 일 월 요일"
     @Scheduled(cron = "0 0/3 * * * *", zone = "Asia/Seoul")
   //  @Scheduled(cron = "${schedules.cron.reward.publish:0 0 9 * * *}", zone = "Asia/Seoul")
    // @Scheduled(cron = "${schedules.cron.reward.publish:0 0 7 * * *}", zone = "Asia/Seoul")
    public void generateDailyTopics() {
        log.info("[SCHED] tick");

        // 이전 작업 진행 중 → 스킵
        if (!running.compareAndSet(false, true)) return;
        try {
            // 모든 유저 조회 -> 추후 활동 유저만 조회로 리펙토링
            List<User> users = userReadService.getAllUser();
            log.info("유저 조회 성공: {}", users);
            // AI 서비스에 모든 유저 전달
            smalltalkAIService.generateTopicsForMultipleUsers(users);
            log.info("AI 요청 시작");
        } finally {
            running.set(false);
        }

    }

}
