// BucketListCreateRequest.java
package com.tropical.backend.bucketList.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 버킷리스트 생성 요청 DTO
 *
 * @author 백승현
 * @version 1.0
 * @since 2025.09.14
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BucketListCreateRequest {

    /**
     * 버킷리스트 내용
     */
    @NotBlank(message = "버킷리스트 내용은 필수입니다.")
    @Size(max = 1000, message = "버킷리스트 내용은 1000자를 초과할 수 없습니다.")
    private String content;
}