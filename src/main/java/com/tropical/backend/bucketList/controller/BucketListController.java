package com.tropical.backend.bucketList.controller;

import com.tropical.backend.auth.entity.User;
import com.tropical.backend.bucketList.dto.request.BucketListCompleteRequest;
import com.tropical.backend.bucketList.dto.request.BucketListCreateRequest;
import com.tropical.backend.bucketList.dto.request.BucketListUpdateRequest;
import com.tropical.backend.bucketList.dto.response.BucketListResponse;
import com.tropical.backend.bucketList.service.BucketListService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 버킷리스트 관리 컨트롤러
 *
 *
 * 사용자의 버킷리스트 생성, 조회, 수정, 삭제, 완료 처리 기능을 제공합니다.
 * API 명세서의 /buckets 엔드포인트를 구현합니다.
 *
 * @author 백승현
 * @version 1.0
 * @since 2025.09.14
 */
@RestController
@RequestMapping("/api/buckets")
@RequiredArgsConstructor
public class BucketListController {

    private final BucketListService bucketListService;

    /**
     * 새 버킷리스트 생성
     *
     * @param user 인증된 사용자 정보
     * @param request 버킷리스트 생성 요청 데이터
     * @return 생성된 버킷리스트 정보
     */
    @PostMapping
    public ResponseEntity<BucketListResponse> createBucketList(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody BucketListCreateRequest request) {

        BucketListResponse response = bucketListService.createBucketList(user, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * 내 모든 버킷리스트 조회
     *
     * @param user 인증된 사용자 정보
     * @return 사용자의 모든 버킷리스트 목록
     */
    @GetMapping
    public ResponseEntity<List<BucketListResponse>> getAllBucketLists(
            @AuthenticationPrincipal User user) {

        List<BucketListResponse> bucketLists = bucketListService.getAllBucketLists(user);
        return ResponseEntity.ok(bucketLists);
    }

    /**
     * 특정 버킷리스트 조회
     *
     * @param user 인증된 사용자 정보
     * @param bucketId 조회할 버킷리스트 ID
     * @return 해당 버킷리스트 정보
     */
    @GetMapping("/{bucketId}")
    public ResponseEntity<BucketListResponse> getBucketList(
            @AuthenticationPrincipal User user,
            @PathVariable Long bucketId) {

        BucketListResponse response = bucketListService.getBucketList(user, bucketId);
        return ResponseEntity.ok(response);
    }

    /**
     * 특정 버킷리스트 수정
     *
     * @param user 인증된 사용자 정보
     * @param bucketId 수정할 버킷리스트 ID
     * @param request 수정 요청 데이터
     * @return 수정된 버킷리스트 정보
     */
    @PutMapping("/{bucketId}")
    public ResponseEntity<BucketListResponse> updateBucketList(
            @AuthenticationPrincipal User user,
            @PathVariable Long bucketId,
            @Valid @RequestBody BucketListUpdateRequest request) {

        BucketListResponse response = bucketListService.updateBucketList(user, bucketId, request);
        return ResponseEntity.ok(response);
    }

    /**
     * 특정 버킷리스트 삭제
     *
     * @param user 인증된 사용자 정보
     * @param bucketId 삭제할 버킷리스트 ID
     * @return 삭제 완료 응답
     */
    @DeleteMapping("/{bucketId}")
    public ResponseEntity<Void> deleteBucketList(
            @AuthenticationPrincipal User user,
            @PathVariable Long bucketId) {

        bucketListService.deleteBucketList(user, bucketId);
        return ResponseEntity.noContent().build();
    }

    /**
     * 버킷리스트 완료/미완료 처리
     *
     * @param user 인증된 사용자 정보
     * @param bucketId 완료 처리할 버킷리스트 ID
     * @param request 완료 상태 변경 요청 데이터
     * @return 수정된 버킷리스트 정보
     */
    @PutMapping("/{bucketId}/complete")
    public ResponseEntity<BucketListResponse> updateBucketListCompletion(
            @AuthenticationPrincipal User user,
            @PathVariable Long bucketId,
            @Valid @RequestBody BucketListCompleteRequest request) {

        BucketListResponse response = bucketListService.updateBucketListCompletion(user, bucketId, request);
        return ResponseEntity.ok(response);
    }
}