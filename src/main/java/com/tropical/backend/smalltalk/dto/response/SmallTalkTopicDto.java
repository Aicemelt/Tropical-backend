package com.tropical.backend.smalltalk.dto.response;

import com.tropical.backend.smalltalk.entity.SmalltalkSources;
import com.tropical.backend.smalltalk.entity.SmalltalkTopic;
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
}
