package com.tropical.backend.smalltalk.dto.request;

import com.tropical.backend.smalltalk.enums.SourceType;

import java.util.List;
import java.util.Map;

public record TopicGenerateRequest (
        int topicCount,
        Map<SourceType, List<ActivityDto>> activities //
) {
}
