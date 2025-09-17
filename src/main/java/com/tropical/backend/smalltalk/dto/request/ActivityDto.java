package com.tropical.backend.smalltalk.dto.request;

import java.time.LocalDateTime;

public record ActivityDto(
        String title,
        String content,
        LocalDateTime createdAt
) {
}
