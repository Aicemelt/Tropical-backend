package com.tropical.backend.smalltalk.dto.response;

import java.util.List;

public record AISmallTalkResponse(
    String topicType,
    String topicContent,
    String exampleQuestion,
    List<Double> embedding,
    List<AISourceDto> sources

) {
}

