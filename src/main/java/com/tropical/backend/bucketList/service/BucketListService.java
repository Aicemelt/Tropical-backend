package com.tropical.backend.bucketList.service;

import com.tropical.backend.auth.entity.User;
import com.tropical.backend.bucketList.dto.request.BucketListCompleteRequest;
import com.tropical.backend.bucketList.dto.request.BucketListCreateRequest;
import com.tropical.backend.bucketList.dto.request.BucketListUpdateRequest;
import com.tropical.backend.bucketList.dto.response.BucketListResponse;
import com.tropical.backend.bucketList.entity.BucketList;
import com.tropical.backend.bucketList.repository.BucketListRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

/**
 * 버킷리스트 비즈니스 로직 서비스
 *
 * <p>
 * 버킷리스트의 생성, 조회, 수정, 삭제 및 완료 처리 등의
 * 핵심 비즈니스 로직을 담당합니다.
 * </p>
 *
 * @author 백승현
 * @version 1.0
 * @since 2025.09.14
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class BucketListService {

    private final BucketListRepository bucketListRepository;

    /**
     * 새로운 버킷리스트 생성
     *
     * @param user 사용자 정보
     * @param request 생성 요청 데이터
     * @return 생성된 버킷리스트 응답 정보
     */
    @Transactional
    public BucketListResponse createBucketList(User user, BucketListCreateRequest request) {
        log.info("Creating bucket list for user: {}, content: {}", user.getId(), request.getContent());

        BucketList bucketList = BucketList.builder()
                .user(user)
                .content(request.getContent())
                .isCompleted(false)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        BucketList savedBucketList = bucketListRepository.save(bucketList);

        log.info("Successfully created bucket list with ID: {}", savedBucketList.getBucketId());
        return BucketListResponse.from(savedBucketList);
    }

    /**
     * 사용자의 모든 버킷리스트 조회
     *
     * @param user 사용자 정보
     * @return 버킷리스트 목록
     */
    public List<BucketListResponse> getAllBucketLists(User user) {
        log.info("Fetching all bucket lists for user: {}", user.getId());

        List<BucketList> bucketLists = bucketListRepository.findByUserOrderByCreatedAtDesc(user);

        return bucketLists.stream()
                .map(BucketListResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * 특정 버킷리스트 조회
     *
     * @param user 사용자 정보
     * @param bucketId 버킷리스트 ID
     * @return 버킷리스트 응답 정보
     * @throws NoSuchElementException 버킷리스트를 찾을 수 없는 경우
     * @throws SecurityException 접근 권한이 없는 경우
     */
    public BucketListResponse getBucketList(User user, Long bucketId) {
        log.info("Fetching bucket list: {} for user: {}", bucketId, user.getId());

        BucketList bucketList = bucketListRepository.findById(bucketId)
                .orElseThrow(() -> new NoSuchElementException("버킷리스트를 찾을 수 없습니다."));

        validateBucketListOwnership(bucketList, user);

        return BucketListResponse.from(bucketList);
    }

    /**
     * 버킷리스트 내용 수정
     *
     * @param user 사용자 정보
     * @param bucketId 수정할 버킷리스트 ID
     * @param request 수정 요청 데이터
     * @return 수정된 버킷리스트 응답 정보
     * @throws NoSuchElementException 버킷리스트를 찾을 수 없는 경우
     * @throws SecurityException 접근 권한이 없는 경우
     */
    @Transactional
    public BucketListResponse updateBucketList(User user, Long bucketId, BucketListUpdateRequest request) {
        log.info("Updating bucket list: {} for user: {}", bucketId, user.getId());

        BucketList bucketList = bucketListRepository.findById(bucketId)
                .orElseThrow(() -> new NoSuchElementException("버킷리스트를 찾을 수 없습니다."));

        validateBucketListOwnership(bucketList, user);

        // 내용 업데이트
        BucketList updatedBucketList = BucketList.builder()
                .bucketId(bucketList.getBucketId())
                .user(bucketList.getUser())
                .content(request.getContent())
                .isCompleted(bucketList.getIsCompleted())
                .createdAt(bucketList.getCreatedAt())
                .updatedAt(LocalDateTime.now())
                .build();

        BucketList savedBucketList = bucketListRepository.save(updatedBucketList);

        log.info("Successfully updated bucket list: {}", bucketId);
        return BucketListResponse.from(savedBucketList);
    }

    /**
     * 버킷리스트 삭제
     *
     * @param user 사용자 정보
     * @param bucketId 삭제할 버킷리스트 ID
     * @throws NoSuchElementException 버킷리스트를 찾을 수 없는 경우
     * @throws SecurityException 접근 권한이 없는 경우
     */
    @Transactional
    public void deleteBucketList(User user, Long bucketId) {
        log.info("Deleting bucket list: {} for user: {}", bucketId, user.getId());

        BucketList bucketList = bucketListRepository.findById(bucketId)
                .orElseThrow(() -> new NoSuchElementException("버킷리스트를 찾을 수 없습니다."));

        validateBucketListOwnership(bucketList, user);

        bucketListRepository.delete(bucketList);

        log.info("Successfully deleted bucket list: {}", bucketId);
    }

    /**
     * 버킷리스트 완료 상태 변경
     *
     * @param user 사용자 정보
     * @param bucketId 완료 처리할 버킷리스트 ID
     * @param request 완료 상태 변경 요청 데이터
     * @return 수정된 버킷리스트 응답 정보
     * @throws NoSuchElementException 버킷리스트를 찾을 수 없는 경우
     * @throws SecurityException 접근 권한이 없는 경우
     */
    @Transactional
    public BucketListResponse updateBucketListCompletion(User user, Long bucketId, BucketListCompleteRequest request) {
        log.info("Updating completion status for bucket list: {} to {} for user: {}",
                bucketId, request.getIsCompleted(), user.getId());

        BucketList bucketList = bucketListRepository.findById(bucketId)
                .orElseThrow(() -> new NoSuchElementException("버킷리스트를 찾을 수 없습니다."));

        validateBucketListOwnership(bucketList, user);

        // 완료 상태 업데이트
        BucketList updatedBucketList = BucketList.builder()
                .bucketId(bucketList.getBucketId())
                .user(bucketList.getUser())
                .content(bucketList.getContent())
                .isCompleted(request.getIsCompleted())
                .createdAt(bucketList.getCreatedAt())
                .updatedAt(LocalDateTime.now())
                .build();

        BucketList savedBucketList = bucketListRepository.save(updatedBucketList);

        log.info("Successfully updated completion status for bucket list: {}", bucketId);
        return BucketListResponse.from(savedBucketList);
    }

    /**
     * 버킷리스트 소유권 검증
     *
     * @param bucketList 검증할 버킷리스트
     * @param user 현재 사용자
     * @throws SecurityException 소유권이 없는 경우
     */
    private void validateBucketListOwnership(BucketList bucketList, User user) {
        if (!bucketList.getUser().getId().equals(user.getId())) {
            log.warn("User {} attempted to access bucket list {} owned by user {}",
                    user.getId(), bucketList.getBucketId(), bucketList.getUser().getId());
            throw new SecurityException("해당 버킷리스트에 접근할 권한이 없습니다.");
        }
    }
}