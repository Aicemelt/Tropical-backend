package com.tropical.backend.smalltalk.dto.response;

import com.tropical.backend.smalltalk.entity.SmalltalkSources;

public record SmalltalkSourcesDto(
    String sourceType
) {
    public static SmalltalkSourcesDto from(SmalltalkSources sources) {
        return new SmalltalkSourcesDto(
                sources.getSourceType().name()
        );
    }
}
