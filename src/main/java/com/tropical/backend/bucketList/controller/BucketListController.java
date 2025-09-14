package com.tropical.backend.bucketList.controller;

import com.tropical.backend.auth.entity.User;
import com.tropical.backend.bucketList.dto.request.BucketListCompleteRequest;
import com.tropical.backend.bucketList.dto.request.BucketListCreateRequest;
import com.tropical.backend.bucketList.dto.request.BucketListUpdateRequest;
import com.tropical.backend.bucketList.dto.response.BucketListResponse;
import com.tropical.backend.bucketList.service.BucketListService;
import com.tropical.backend.auth.repository.UserRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.NoSuchElementException;

/**
 * 버킷리스트 관리 컨트롤러 (테스트 버전)
 *
 * <p>
 * 사용자의 버킷리스트 생성, 조회, 수정, 삭제, 완료 처리 기능을 제공합니다.
 * API 명세서의 /buckets 엔드포인트를 구현합니다.
 *
 * 현재는 인증 기능이 구현되지 않아 userId를 파라미터로 받아 테스트합니다.
 * </p>
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
    private final UserRepository userRepository;

    /**
     * 새 버킷리스트 생성
     *
     * @param userId 사용자 ID (더미 데이터 테스트용)
     * @param request 버킷리스트 생성 요청 데이터
     * @return 생성된 버킷리스트 정보
     */
    @PostMapping("/user/{userId}")
    public ResponseEntity<BucketListResponse> createBucketList(
            @PathVariable Long userId,
            @Valid @RequestBody BucketListCreateRequest request) {

        User user = getUserById(userId);
        BucketListResponse response = bucketListService.createBucketList(user, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * 내 모든 버킷리스트 조회
     *
     * @param userId 사용자 ID (더미 데이터 테스트용)
     * @return 사용자의 모든 버킷리스트 목록
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<BucketListResponse>> getAllBucketLists(
            @PathVariable Long userId) {

        User user = getUserById(userId);
        List<BucketListResponse> bucketLists = bucketListService.getAllBucketLists(user);
        return ResponseEntity.ok(bucketLists);
    }

    /**
     * 특정 버킷리스트 조회
     *
     * @param userId 사용자 ID (더미 데이터 테스트용)
     * @param bucketId 조회할 버킷리스트 ID
     * @return 해당 버킷리스트 정보
     */
    @GetMapping("/user/{userId}/{bucketId}")
    public ResponseEntity<BucketListResponse> getBucketList(
            @PathVariable Long userId,
            @PathVariable Long bucketId) {

        User user = getUserById(userId);
        BucketListResponse response = bucketListService.getBucketList(user, bucketId);
        return ResponseEntity.ok(response);
    }

    /**
     * 버킷리스트 완료/미완료 토글 (Request Body 없음)
     *
     * @param userId 사용자 ID (더미 데이터 테스트용)
     * @param bucketId 토글할 버킷리스트 ID
     * @return 수정된 버킷리스트 정보
     */
    @PutMapping("/user/{userId}/{bucketId}/complete")
    public ResponseEntity<BucketListResponse> toggleBucketListCompletion(
            @PathVariable Long userId,
            @PathVariable Long bucketId) {

        User user = getUserById(userId);
        BucketListResponse response = bucketListService.toggleBucketListCompletion(user, bucketId);
        return ResponseEntity.ok(response);
    }
    /**
     * 특정 버킷리스트 수정
     *
     * @param userId 사용자 ID (더미 데이터 테스트용)
     * @param bucketId 수정할 버킷리스트 ID
     * @param request 수정 요청 데이터
     * @return 수정된 버킷리스트 정보
     */
    @PutMapping("/user/{userId}/{bucketId}")
    public ResponseEntity<BucketListResponse> updateBucketList(
            @PathVariable Long userId,
            @PathVariable Long bucketId,
            @Valid @RequestBody BucketListUpdateRequest request) {

        User user = getUserById(userId);
        BucketListResponse response = bucketListService.updateBucketList(user, bucketId, request);
        return ResponseEntity.ok(response);
    }

    /**
     * 특정 버킷리스트 삭제
     *
     * @param userId 사용자 ID (더미 데이터 테스트용)
     * @param bucketId 삭제할 버킷리스트 ID
     * @return 삭제 완료 응답
     */
    @DeleteMapping("/user/{userId}/{bucketId}")
    public ResponseEntity<?> deleteBucketList(
            @PathVariable Long userId,
            @PathVariable Long bucketId) {

        User user = getUserById(userId);
        bucketListService.deleteBucketList(user, bucketId);
        return ResponseEntity.ok("삭제가 완료되었습니다.");
    }


    /**
     * 사용자 ID로 User 엔티티 조회 (헬퍼 메서드)
     *
     * @param userId 사용자 ID
     * @return User 엔티티
     * @throws NoSuchElementException 사용자를 찾을 수 없는 경우
     */
    private User getUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("사용자를 찾을 수 없습니다. ID: " + userId));
    }
}