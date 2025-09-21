package com.tropical.backend.smalltalk.provider;

import com.tropical.backend.smalltalk.dto.response.SmallTalkTopicDto;

import java.util.List;

public final class WelcomeTopicProvider {

    // new 로 호출 될 수 없게 막음
    private WelcomeTopicProvider() {}

    public static List<WelcomeTopicSeed> seeds() {
        return List.of(
                new WelcomeTopicSeed("LIFESTYLE", "주말 루틴", "주말에는 보통 뭘 하면서 쉬세요?"),
                new WelcomeTopicSeed("FOOD", "요즘 즐겨먹는 음식", "최근에 자주 먹는 음식이나 간식 있으세요?"),
                new WelcomeTopicSeed("CREATIVE", "만약 여행권을 받는다면", "비행시간 제한 없이 어디로 가고 싶으세요?"),
                new WelcomeTopicSeed("THOUGHT", "소소한 감사", "최근에 감사하다고 느낀 순간이 있으세요?"),
                new WelcomeTopicSeed("THOUGHT", "소소한 감사", "최근에 감사하다고 느낀 순간이 있으세요?")
        );
    }

    public record WelcomeTopicSeed(
            String topicType,
            String topicContent,
            String exampleQuestion
    ) {};
}
