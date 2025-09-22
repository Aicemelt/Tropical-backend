package com.tropical.backend.smalltalk.dto.response;

import com.tropical.backend.smalltalk.entity.SmalltalkSources;
import com.tropical.backend.smalltalk.entity.SmalltalkTopic;
import com.tropical.backend.smalltalk.provider.WelcomeTopicProvider;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 스몰토크 주제 응답 DTO
 *
 * @author 진도희
 * @since 2025.09.17
 */
public record SmallTalkTopicDto(
        Long id,
        String topicType,
        String topicContent,
        String exampleQuestion,
        List<SmalltalkSourcesDto> sources
) {

    // ai가 생성한 주제 매핑 메소드
    public static SmallTalkTopicDto from(SmalltalkTopic topic,
                                         List<SmalltalkSourcesDto> sources) {
        return new SmallTalkTopicDto(
                topic.getId(),
                topic.getTopicType(),
                topic.getTopicContent(),
                topic.getExampleQuestion(),
                sources
        );
    }

    // 웰컴 주제 매핑 메소드
    public static SmallTalkTopicDto from(WelcomeTopicProvider.WelcomeTopicSeed seed, Long index) {
        return  new SmallTalkTopicDto(
                index,
                seed.topicType(),
                seed.topicContent(),
                seed.exampleQuestion(),
                List.of()
        );
    }
}
