package com.tropical.backend.smalltalk.dto.response;

import com.tropical.backend.smalltalk.entity.SmalltalkTopic;
import java.time.LocalDateTime;

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
        LocalDateTime createdAt,
        String exampleQuestion
) {

    public static SmallTalkTopicDto from(SmalltalkTopic topic) {
        return new SmallTalkTopicDto(
                topic.getId(),
                topic.getTopicType(),
                topic.getTopicContent(),
                topic.getCreatedAt(),
                topic.getExampleQuestion()
        );
    }
}
