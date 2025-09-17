package com.tropical.backend.smalltalk.dto.response;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 스몰토크 주제 응답 DTO
 *
 * @author 진도희
 * @since 2025.09.17
 */
public record TopicResponse(
     List<SmallTalkTopicDto> topics
) {

}
