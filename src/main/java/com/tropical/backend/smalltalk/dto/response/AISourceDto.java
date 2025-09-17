package com.tropical.backend.smalltalk.dto.response;

import com.tropical.backend.smalltalk.SourceType;

public record AISourceDto(
        Long sourceId,
        SourceType sourceType
) {
}
