package com.tropical.backend.todo.repository;

import com.tropical.backend.auth.entity.User;
import com.tropical.backend.todo.entity.Todo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Todo 데이터 접근 Repository
 *
 * @author 백승현
 * @version 1.0
 * @since 2025.09.15
 */
@Repository
public interface TodoRepository extends JpaRepository<Todo, Long> {

    /**
     * 사용자의 모든 할 일 조회 (마감일 오름차순, 생성일 오름차순)
     * 1순위: 마감기한이 적게 남은 순 (null인 경우 마지막으로)
     * 2순위: 등록일이 빠른 순
     *
     * @param user 사용자 엔터티
     * @return 할 일 목록
     */
    @Query("SELECT t FROM Todo t WHERE t.user = :user " +
            "ORDER BY " +
            "CASE WHEN t.dueDate IS NULL THEN 1 ELSE 0 END, " +
            "t.dueDate ASC, " +
            "t.createdAt ASC")
    List<Todo> findByUserOrderByDueDateAndCreatedAt(@Param("user") User user);

    /**
     * 사용자 ID와 할 일 ID로 특정 할 일 조회
     *
     * @param todoId 할 일 ID
     * @param userId 사용자 ID
     * @return 할 일 Optional
     */
    @Query("SELECT t FROM Todo t WHERE t.todoId = :todoId AND t.user.id = :userId")
    Optional<Todo> findByTodoIdAndUserId(@Param("todoId") Long todoId, @Param("userId") Long userId);

    /**
     * 사용자의 완료되지 않은 할 일 조회
     *
     * @param user 사용자 엔터티
     * @return 미완료 할 일 목록
     */
    List<Todo> findByUserAndIsCompletedFalseOrderByCreatedAtDesc(User user);

    /**
     * 사용자의 완료된 할 일 조회
     *
     * @param user 사용자 엔터티
     * @return 완료된 할 일 목록
     */
    List<Todo> findByUserAndIsCompletedTrueOrderByCreatedAtDesc(User user);

    /**
     * 사용자의 특정 날짜까지 마감인 미완료 할 일 조회
     *
     * @param user 사용자 엔터티
     * @param dueDate 마감 날짜
     * @return 마감이 임박한 미완료 할 일 목록
     */
    @Query("SELECT t FROM Todo t WHERE t.user = :user AND t.isCompleted = false " +
            "AND t.dueDate IS NOT NULL AND t.dueDate <= :dueDate ORDER BY t.dueDate ASC")
    List<Todo> findOverdueTodos(@Param("user") User user, @Param("dueDate") LocalDate dueDate);

    /**
     * 사용자의 특정 날짜 범위 내 할 일 조회
     *
     * @param user 사용자 엔터티
     * @param startDate 시작 날짜
     * @param endDate 종료 날짜
     * @return 해당 기간의 할 일 목록
     */
    @Query("SELECT t FROM Todo t WHERE t.user = :user " +
            "AND t.dueDate IS NOT NULL " +
            "AND t.dueDate BETWEEN :startDate AND :endDate " +
            "ORDER BY t.dueDate ASC, t.createdAt DESC")
    List<Todo> findByUserAndDueDateBetween(@Param("user") User user,
                                           @Param("startDate") LocalDate startDate,
                                           @Param("endDate") LocalDate endDate);

    /**
     * 사용자의 마감기한이 지나지 않은 미완료 할 일 조회
     * (마감일이 null이거나 오늘 이후인 미완료 할 일)
     *
     * @param user 사용자 엔터티
     * @param today 오늘 날짜
     * @return 마감기한이 지나지 않은 미완료 할 일 목록
     */
    @Query("SELECT t FROM Todo t WHERE t.user = :user AND t.isCompleted = false " +
            "AND (t.dueDate IS NULL OR t.dueDate >= :today) " +
            "ORDER BY " +
            "CASE WHEN t.dueDate IS NULL THEN 1 ELSE 0 END, " +
            "t.dueDate ASC, " +
            "t.createdAt ASC")
    List<Todo> findNonOverdueIncompleteTodos(@Param("user") User user, @Param("today") LocalDate today);



    /**
     * 사용자의 할 일 개수 조회
     *
     * @param user 사용자 엔터티
     * @return 전체 할 일 개수
     */
    long countByUser(User user);

    /**
     * 사용자의 완료된 할 일 개수 조회
     *
     * @param user 사용자 엔터티
     * @return 완료된 할 일 개수
     */
    long countByUserAndIsCompletedTrue(User user);

    /**
     * 사용자의 미완료 할 일 개수 조회
     *
     * @param user 사용자 엔터티
     * @return 미완료 할 일 개수
     */
    long countByUserAndIsCompletedFalse(User user);
}