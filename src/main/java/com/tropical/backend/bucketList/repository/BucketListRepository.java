package com.tropical.backend.bucketList.repository;

import com.tropical.backend.auth.entity.User;
import com.tropical.backend.bucketList.entity.BucketList;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 버킷리스트 데이터 접근 레포지토리
 *
 * <p>
 * 버킷리스트 엔티티에 대한 데이터베이스 접근 로직을 담당합니다.
 * JPA를 활용하여 기본적인 CRUD 및 커스텀 쿼리를 제공합니다.
 * </p>
 *
 * @author 백승현
 * @version 1.0
 * @since 2025.09.15
 */
@Repository
public interface BucketListRepository extends JpaRepository<BucketList, Long> {

    /**
     * 특정 사용자의 모든 버킷리스트를 생성일 기준 내림차순으로 조회
     *
     * @param user 사용자 정보
     * @return 버킷리스트 목록 (최신순)
     */
    List<BucketList> findByUserOrderByCreatedAtDesc(User user);

    /**
     * 특정 사용자의 완료 상태별 버킷리스트 조회
     *
     * @param user 사용자 정보
     * @param isCompleted 완료 상태
     * @return 해당 완료 상태의 버킷리스트 목록
     */
    List<BucketList> findByUserAndIsCompletedOrderByCreatedAtDesc(User user, Boolean isCompleted);

    /**
     * 특정 사용자의 버킷리스트 개수 조회
     *
     * @param user 사용자 정보
     * @return 버킷리스트 총 개수
     */
    long countByUser(User user);

    /**
     * 특정 사용자의 완료된 버킷리스트 개수 조회
     *
     * @param user 사용자 정보
     * @return 완료된 버킷리스트 개수
     */
    long countByUserAndIsCompleted(User user, Boolean isCompleted);

    /**
     * 사용자와 버킷리스트 ID로 특정 버킷리스트 조회
     *
     * @param user 사용자 정보
     * @param bucketId 버킷리스트 ID
     * @return 해당 버킷리스트 (Optional)
     */
    Optional<BucketList> findByUserAndBucketId(User user, Long bucketId);

    /**
     * 특정 사용자의 버킷리스트 내용으로 검색
     *
     * @param user 사용자 정보
     * @param keyword 검색 키워드
     * @return 키워드가 포함된 버킷리스트 목록
     */
    @Query("SELECT b FROM BucketList b WHERE b.user = :user AND b.content LIKE %:keyword% ORDER BY b.createdAt DESC")
    List<BucketList> findByUserAndContentContainingIgnoreCaseOrderByCreatedAtDesc(
            @Param("user") User user,
            @Param("keyword") String keyword);

    /**
     * 특정 사용자의 최근 N개 버킷리스트 조회
     *
     * @param user 사용자 정보
     * @param limit 조회할 개수
     * @return 최근 버킷리스트 목록
     */
    @Query("SELECT b FROM BucketList b WHERE b.user = :user ORDER BY b.createdAt DESC LIMIT :limit")
    List<BucketList> findRecentBucketListsByUser(@Param("user") User user, @Param("limit") int limit);

    /**
     * 사용자가 존재하는지 확인
     *
     * @param user 사용자 정보
     * @return 버킷리스트 존재 여부
     */
    boolean existsByUser(User user);
}