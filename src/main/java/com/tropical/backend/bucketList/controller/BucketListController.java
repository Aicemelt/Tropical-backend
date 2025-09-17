package com.tropical.backend.bucketList.controller;

import com.tropical.backend.auth.entity.User;
import com.tropical.backend.auth.repository.UserRepository;
import com.tropical.backend.bucketList.dto.request.BucketListCompleteRequest;
import com.tropical.backend.bucketList.dto.request.BucketListCreateRequest;
import com.tropical.backend.bucketList.dto.request.BucketListUpdateRequest;
import com.tropical.backend.bucketList.dto.response.BucketListResponse;
import com.tropical.backend.bucketList.service.BucketListService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 버킷리스트 관리 컨트롤러
 *
 * <p>
 * 사용자의 버킷리스트 생성, 조회, 수정, 삭제, 완료 처리 기능을 제공합니다.
 * API 명세서의 /buckets 엔드포인트를 구현합니다.
 * JWT 토큰을 통해 인증된 사용자만 접근 가능합니다.
 * </p>
 *
 * @author 백승현
 * @version 2.2
 * @since 2025.09.16
 */
@RestController
@RequestMapping("/api/buckets")
@RequiredArgsConstructor
@Tag(name = "BucketList", description = "버킷리스트 API")
public class BucketListController {

    private final BucketListService bucketListService;
    private final UserRepository userRepository;

    /**
     * 인증된 사용자 엔터티 조회 헬퍼 메서드
     *
     * @param userDetails Spring Security에서 제공하는 사용자 인증 정보
     * @return User 엔터티
     * @throws RuntimeException 사용자를 찾을 수 없는 경우
     */
    private User getCurrentUser(UserDetails userDetails) {
        Long userId = Long.valueOf(userDetails.getUsername());
        return userRepository.findByIdAndActive(userId)
                .orElseThrow(() -> new RuntimeException("활성 사용자를 찾을 수 없습니다."));
    }

    /**
     * 새 버킷리스트 생성
     *
     * @param userDetails 인증된 사용자 정보
     * @param request 버킷리스트 생성 요청 데이터
     * @return 생성된 버킷리스트 정보
     */
    @PostMapping
    public ResponseEntity<BucketListResponse> createBucketList(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody BucketListCreateRequest request) {

        User currentUser = getCurrentUser(userDetails);
        BucketListResponse response = bucketListService.createBucketList(currentUser, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * 내 모든 버킷리스트 조회
     *
     * @param userDetails 인증된 사용자 정보
     * @return 사용자의 모든 버킷리스트 목록
     */
    @GetMapping
    public ResponseEntity<List<BucketListResponse>> getAllBucketLists(
            @AuthenticationPrincipal UserDetails userDetails) {

        User currentUser = getCurrentUser(userDetails);
        List<BucketListResponse> bucketLists = bucketListService.getAllBucketLists(currentUser);
        return ResponseEntity.ok(bucketLists);
    }

    /**
     * 완료된 버킷리스트 조회
     *
     * @param userDetails 인증된 사용자 정보
     * @return 사용자의 완료된 버킷리스트 목록
     */
    @GetMapping("/completed")
    public ResponseEntity<List<BucketListResponse>> getCompletedBucketLists(
            @AuthenticationPrincipal UserDetails userDetails) {

        User currentUser = getCurrentUser(userDetails);
        List<BucketListResponse> completedBucketLists = bucketListService.getCompletedBucketLists(currentUser);
        return ResponseEntity.ok(completedBucketLists);
    }

    /**
     * 미완료된 버킷리스트 조회
     *
     * @param userDetails 인증된 사용자 정보
     * @return 사용자의 미완료된 버킷리스트 목록
     */
    @GetMapping("/incomplete")
    public ResponseEntity<List<BucketListResponse>> getIncompleteBucketLists(
            @AuthenticationPrincipal UserDetails userDetails) {

        User currentUser = getCurrentUser(userDetails);
        List<BucketListResponse> incompleteBucketLists = bucketListService.getIncompleteBucketLists(currentUser);
        return ResponseEntity.ok(incompleteBucketLists);
    }

    /**
     * 특정 버킷리스트 조회
     *
     * @param userDetails 인증된 사용자 정보
     * @param bucketId 조회할 버킷리스트 ID
     * @return 해당 버킷리스트 정보
     */
    @GetMapping("/{bucketId}")
    public ResponseEntity<BucketListResponse> getBucketList(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long bucketId) {

        User currentUser = getCurrentUser(userDetails);
        BucketListResponse response = bucketListService.getBucketList(currentUser, bucketId);
        return ResponseEntity.ok(response);
    }

    /**
     * 버킷리스트 완료/미완료 처리 (명시적 값 입력 방식)
     *
     * @param userDetails 인증된 사용자 정보
     * @param bucketId 처리할 버킷리스트 ID
     * @param request 완료 처리 요청 데이터
     * @return 수정된 버킷리스트 정보
     */
    @PutMapping("/{bucketId}/complete")
    public ResponseEntity<BucketListResponse> completeBucketList(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long bucketId,
            @Valid @RequestBody BucketListCompleteRequest request) {

        User currentUser = getCurrentUser(userDetails);
        BucketListResponse response = bucketListService.completeBucketList(currentUser, bucketId, request);
        return ResponseEntity.ok(response);
    }

    /**
     * 특정 버킷리스트 수정
     *
     * @param userDetails 인증된 사용자 정보
     * @param bucketId 수정할 버킷리스트 ID
     * @param request 수정 요청 데이터
     * @return 수정된 버킷리스트 정보
     */
    @PutMapping("/{bucketId}")
    public ResponseEntity<BucketListResponse> updateBucketList(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long bucketId,
            @Valid @RequestBody BucketListUpdateRequest request) {

        User currentUser = getCurrentUser(userDetails);
        BucketListResponse response = bucketListService.updateBucketList(currentUser, bucketId, request);
        return ResponseEntity.ok(response);
    }

    /**
     * 특정 버킷리스트 삭제
     *
     * @param userDetails 인증된 사용자 정보
     * @param bucketId 삭제할 버킷리스트 ID
     * @return 삭제 완료 응답
     */
    @DeleteMapping("/{bucketId}")
    public ResponseEntity<?> deleteBucketList(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long bucketId) {

        User currentUser = getCurrentUser(userDetails);
        bucketListService.deleteBucketList(currentUser, bucketId);
        return ResponseEntity.ok("삭제가 완료되었습니다.");
    }
}