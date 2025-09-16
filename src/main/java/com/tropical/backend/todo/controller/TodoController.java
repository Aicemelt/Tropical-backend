package com.tropical.backend.todo.controller;

import com.tropical.backend.auth.entity.User;
import com.tropical.backend.auth.repository.UserRepository;
import com.tropical.backend.todo.dto.request.TodosCreateRequest;
import com.tropical.backend.todo.dto.request.TodosUpdateRequest;
import com.tropical.backend.todo.dto.request.TodosCompleteRequest;
import com.tropical.backend.todo.dto.response.TodosResponse;
import com.tropical.backend.todo.service.TodoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Todo REST API 컨트롤러
 *
 * <p>
 * Todo(할 일) 관련 CRUD 기능을 제공하는 REST API 엔드포인트입니다.
 * 사용자는 할 일을 생성, 조회, 수정, 삭제하고 완료 상태를 변경할 수 있습니다.
 * JWT 토큰을 통해 인증된 사용자만 접근 가능합니다.
 * </p>
 *
 * @author 백승현
 * @version 2.0
 * @since 2025.09.16
 */
@RestController
@RequestMapping("/api/todos")
@RequiredArgsConstructor
@Slf4j
public class TodoController {

    private final TodoService todoService;
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
     * 새 할 일 생성
     *
     * @param userDetails 인증된 사용자 정보
     * @param request 생성 요청 DTO
     * @return 생성된 할 일 정보
     */
    @PostMapping
    public ResponseEntity<TodosResponse> createTodo(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody TodosCreateRequest request) {

        Long userId = Long.valueOf(userDetails.getUsername());
        log.info("POST /api/todos - Creating todo for user: {}", userId);

        TodosResponse response = todoService.createTodo(userId, request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * 내 모든 할 일 조회
     *
     * @param userDetails 인증된 사용자 정보
     * @return 할 일 목록
     */
    @GetMapping
    public ResponseEntity<List<TodosResponse>> getAllTodos(
            @AuthenticationPrincipal UserDetails userDetails) {

        Long userId = Long.valueOf(userDetails.getUsername());
        log.info("GET /api/todos - Fetching todos for user: {}", userId);

        List<TodosResponse> todos = todoService.getAllTodos(userId);

        return ResponseEntity.ok(todos);
    }

    /**
     * 특정 할 일 조회
     *
     * @param userDetails 인증된 사용자 정보
     * @param todoId 할 일 ID
     * @return 할 일 정보
     */
    @GetMapping("/{todoId}")
    public ResponseEntity<TodosResponse> getTodoById(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long todoId) {

        Long userId = Long.valueOf(userDetails.getUsername());
        log.info("GET /api/todos/{} - Fetching todo for user: {}", todoId, userId);

        TodosResponse response = todoService.getTodoById(userId, todoId);

        return ResponseEntity.ok(response);
    }

    /**
     * 특정 할 일 수정 (부분 업데이트)
     *
     * @param userDetails 인증된 사용자 정보
     * @param todoId 할 일 ID
     * @param request 수정 요청 DTO (content, dueDate 중 하나 또는 둘 다 포함)
     * @return 수정된 할 일 정보
     */
    @PutMapping("/{todoId}")
    public ResponseEntity<TodosResponse> updateTodo(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long todoId,
            @Valid @RequestBody TodosUpdateRequest request) {

        Long userId = Long.valueOf(userDetails.getUsername());
        log.info("PUT /api/todos/{} - Updating todo for user: {}", todoId, userId);

        TodosResponse response = todoService.updateTodo(userId, todoId, request);

        return ResponseEntity.ok(response);
    }

    /**
     * 특정 할 일 삭제
     *
     * @param userDetails 인증된 사용자 정보
     * @param todoId 할 일 ID
     * @return 삭제 완료 응답
     */
    @DeleteMapping("/{todoId}")
    public ResponseEntity<Void> deleteTodo(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long todoId) {

        Long userId = Long.valueOf(userDetails.getUsername());
        log.info("DELETE /api/todos/{} - Deleting todo for user: {}", todoId, userId);

        todoService.deleteTodo(userId, todoId);

        return ResponseEntity.noContent().build();
    }

    /**
     * 할 일 완료/미완료 처리
     *
     * @param userDetails 인증된 사용자 정보
     * @param todoId 할 일 ID
     * @param request 완료 처리 요청 DTO
     * @return 수정된 할 일 정보
     */
    @PutMapping("/{todoId}/complete")
    public ResponseEntity<TodosResponse> completeTodo(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long todoId,
            @Valid @RequestBody TodosCompleteRequest request) {

        Long userId = Long.valueOf(userDetails.getUsername());
        log.info("PUT /api/todos/{}/complete - Updating completion status to {} for user: {}",
                todoId, request.getIsCompleted(), userId);

        TodosResponse response = todoService.completeTodo(userId, todoId, request);

        return ResponseEntity.ok(response);
    }

    /**
     * 할 일 마감일 제거
     *
     * @param userDetails 인증된 사용자 정보
     * @param todoId 할 일 ID
     * @return 수정된 할 일 정보
     */
    @DeleteMapping("/{todoId}/due-date")
    public ResponseEntity<TodosResponse> removeDueDate(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long todoId) {

        Long userId = Long.valueOf(userDetails.getUsername());
        log.info("DELETE /api/todos/{}/due-date - Removing due date for user: {}", todoId, userId);

        TodosResponse response = todoService.removeDueDate(userId, todoId);

        return ResponseEntity.ok(response);
    }

    /**
     * 완료된 할 일 목록 조회
     *
     * @param userDetails 인증된 사용자 정보
     * @return 완료된 할 일 목록
     */
    @GetMapping("/completed")
    public ResponseEntity<List<TodosResponse>> getCompletedTodos(
            @AuthenticationPrincipal UserDetails userDetails) {

        Long userId = Long.valueOf(userDetails.getUsername());
        log.info("GET /api/todos/completed - Fetching completed todos for user: {}", userId);

        List<TodosResponse> completedTodos = todoService.getCompletedTodos(userId);

        return ResponseEntity.ok(completedTodos);
    }

    /**
     * 마감기한이 지난 할 일 목록 조회
     *
     * @param userDetails 인증된 사용자 정보
     * @return 마감기한이 지난 미완료 할 일 목록
     */
    @GetMapping("/overdue")
    public ResponseEntity<List<TodosResponse>> getOverdueTodos(
            @AuthenticationPrincipal UserDetails userDetails) {

        Long userId = Long.valueOf(userDetails.getUsername());
        log.info("GET /api/todos/overdue - Fetching overdue todos for user: {}", userId);

        List<TodosResponse> overdueTodos = todoService.getOverdueTodos(userId);

        return ResponseEntity.ok(overdueTodos);
    }

    /**
     * 미완료 할 일 목록 조회
     *
     * @param userDetails 인증된 사용자 정보
     * @return 미완료 할 일 목록
     */
    @GetMapping("/incomplete")
    public ResponseEntity<List<TodosResponse>> getIncompleteTodos(
            @AuthenticationPrincipal UserDetails userDetails) {

        Long userId = Long.valueOf(userDetails.getUsername());
        log.info("GET /api/todos/incomplete - Fetching incomplete todos for user: {}", userId);

        List<TodosResponse> incompleteTodos = todoService.getIncompleteTodos(userId);

        return ResponseEntity.ok(incompleteTodos);
    }
}