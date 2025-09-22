package com.tropical.backend.smalltalk.dto.request;

import java.time.LocalDateTime;

public record ActivityDto(
        Long id,
        String title,
        String content,
        LocalDateTime createdAt
) {
}
