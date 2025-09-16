package com.tropical.backend.bucketList.dto.response;

import com.tropical.backend.bucketList.entity.BucketList;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 버킷리스트 응답 DTO
 *
 * @author 백승현
 * @version 1.0
 * @since 2025.09.15
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BucketListResponse {

    /**
     * 버킷리스트 ID
     */
    private Long bucketId;

    /**
     * 버킷리스트 내용
     */
    private String content;

    /**
     * 완료 여부
     */
    private Boolean isCompleted;

    /**
     * 생성일시
     */
    private LocalDateTime createdAt;

    /**
     * 수정일시
     */
    private LocalDateTime updatedAt;

    /**
     * BucketList 엔티티로부터 BucketListResponse 생성
     *
     * @param bucketList 버킷리스트 엔티티
     * @return BucketListResponse 객체
     */
    public static BucketListResponse from(BucketList bucketList) {
        return BucketListResponse.builder()
                .bucketId(bucketList.getBucketId())
                .content(bucketList.getContent())
                .isCompleted(bucketList.getIsCompleted())
                .createdAt(bucketList.getCreatedAt())
                .updatedAt(bucketList.getUpdatedAt())
                .build();
    }
}