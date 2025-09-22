package com.tropical.backend.smalltalk.dto.response;

import com.tropical.backend.smalltalk.entity.SmalltalkSources;

public record SmalltalkSourcesDto(
    Long sourceId,
    String sourceType
) {
    public static SmalltalkSourcesDto from(SmalltalkSources source) {
        return new SmalltalkSourcesDto(
                source.getSourceId(),
                source.getSourceType().name()
        );
    }
}
