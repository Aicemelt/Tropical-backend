package com.tropical.backend.bucketList.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 버킷리스트 완료 상태 변경 요청 DTO
 *
 * @author 백승현
 * @version 1.0
 * @since 2025.09.14
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BucketListCompleteRequest {

    /**
     * 완료 여부
     */
    @NotNull(message = "완료 상태는 필수입니다.")
    private Boolean isCompleted;
}