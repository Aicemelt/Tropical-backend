package com.tropical.backend.todo.controller;

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
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Todo REST API 컨트롤러
 *
 * <p>
 * Todo(할 일) 관련 CRUD 기능을 제공하는 REST API 엔드포인트입니다.
 * 사용자는 할 일을 생성, 조회, 수정, 삭제하고 완료 상태를 변경할 수 있습니다.
 * </p>
 *
 * <p>
 * 현재는 테스트를 위해 userId를 PathVariable로 직접 받도록 구현되어 있습니다.
 * 추후 JWT 토큰 인증 기능 완료 후 @AuthenticationPrincipal로 변경 예정입니다.
 * </p>
 *
 * @author 백승현
 * @version 1.0
 * @since 2025.09.16
 */
@RestController
@RequestMapping("/api/users/{userId}/todos")
@RequiredArgsConstructor
@Slf4j
public class TodoController {

    private final TodoService todoService;

    /**
     * 새 할 일 생성
     *
     * @param userId 사용자 ID (PathVariable)
     * @param request 생성 요청 DTO
     * @return 생성된 할 일 정보
     */
    @PostMapping
    public ResponseEntity<TodosResponse> createTodo(
            @PathVariable Long userId,
            @Valid @RequestBody TodosCreateRequest request) {

        log.info("POST /api/users/{}/todos - Creating todo", userId);

        TodosResponse response = todoService.createTodo(userId, request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * 내 모든 할 일 조회
     *
     * @param userId 사용자 ID (PathVariable)
     * @return 할 일 목록
     */
    @GetMapping
    public ResponseEntity<List<TodosResponse>> getAllTodos(
            @PathVariable Long userId) {

        log.info("GET /api/users/{}/todos - Fetching todos", userId);

        List<TodosResponse> todos = todoService.getAllTodos(userId);

        return ResponseEntity.ok(todos);
    }

    /**
     * 특정 할 일 조회
     *
     * @param userId 사용자 ID (PathVariable)
     * @param todoId 할 일 ID
     * @return 할 일 정보
     */
    @GetMapping("/{todoId}")
    public ResponseEntity<TodosResponse> getTodoById(
            @PathVariable Long userId,
            @PathVariable Long todoId) {

        log.info("GET /api/users/{}/todos/{} - Fetching todo", userId, todoId);

        TodosResponse response = todoService.getTodoById(userId, todoId);

        return ResponseEntity.ok(response);
    }

    /**
     * 특정 할 일 수정 (부분 업데이트)
     *
     * @param userId 사용자 ID (PathVariable)
     * @param todoId 할 일 ID
     * @param request 수정 요청 DTO (content, dueDate 중 하나 또는 둘 다 포함)
     * @return 수정된 할 일 정보
     */
    @PutMapping("/{todoId}")
    public ResponseEntity<TodosResponse> updateTodo(
            @PathVariable Long userId,
            @PathVariable Long todoId,
            @Valid @RequestBody TodosUpdateRequest request) {

        log.info("PUT /api/users/{}/todos/{} - Updating todo", userId, todoId);

        TodosResponse response = todoService.updateTodo(userId, todoId, request);

        return ResponseEntity.ok(response);
    }

    /**
     * 특정 할 일 삭제
     *
     * @param userId 사용자 ID (PathVariable)
     * @param todoId 할 일 ID
     * @return 삭제 완료 응답
     */
    @DeleteMapping("/{todoId}")
    public ResponseEntity<Void> deleteTodo(
            @PathVariable Long userId,
            @PathVariable Long todoId) {

        log.info("DELETE /api/users/{}/todos/{} - Deleting todo", userId, todoId);

        todoService.deleteTodo(userId, todoId);

        return ResponseEntity.noContent().build();
    }

    /**
     * 할 일 완료/미완료 처리
     *
     * @param userId 사용자 ID (PathVariable)
     * @param todoId 할 일 ID
     * @param request 완료 처리 요청 DTO
     * @return 수정된 할 일 정보
     */
    @PutMapping("/{todoId}/complete")
    public ResponseEntity<TodosResponse> completeTodo(
            @PathVariable Long userId,
            @PathVariable Long todoId,
            @Valid @RequestBody TodosCompleteRequest request) {

        log.info("PUT /api/users/{}/todos/{}/complete - Updating completion status to {}",
                userId, todoId, request.getIsCompleted());

        TodosResponse response = todoService.completeTodo(userId, todoId, request);

        return ResponseEntity.ok(response);
    }

    /**
     * 할 일 마감일 제거
     *
     * @param userId 사용자 ID (PathVariable)
     * @param todoId 할 일 ID
     * @return 수정된 할 일 정보
     */
    @DeleteMapping("/{todoId}/due-date")
    public ResponseEntity<TodosResponse> removeDueDate(
            @PathVariable Long userId,
            @PathVariable Long todoId) {

        log.info("DELETE /api/users/{}/todos/{}/due-date - Removing due date", userId, todoId);

        TodosResponse response = todoService.removeDueDate(userId, todoId);

        return ResponseEntity.ok(response);
    }

    /**
     * 완료된 할 일 목록 조회
     *
     * @param userId 사용자 ID (PathVariable)
     * @return 완료된 할 일 목록
     */
    @GetMapping("/completed")
    public ResponseEntity<List<TodosResponse>> getCompletedTodos(
            @PathVariable Long userId) {

        log.info("GET /api/users/{}/todos/completed - Fetching completed todos", userId);

        List<TodosResponse> completedTodos = todoService.getCompletedTodos(userId);

        return ResponseEntity.ok(completedTodos);
    }

    /**
     * 마감기한이 지난 할 일 목록 조회
     *
     * @param userId 사용자 ID (PathVariable)
     * @return 마감기한이 지난 미완료 할 일 목록
     */
    @GetMapping("/overdue")
    public ResponseEntity<List<TodosResponse>> getOverdueTodos(
            @PathVariable Long userId) {

        log.info("GET /api/users/{}/todos/overdue - Fetching overdue todos", userId);

        List<TodosResponse> overdueTodos = todoService.getOverdueTodos(userId);

        return ResponseEntity.ok(overdueTodos);
    }

    /**
     * 미완료 할 일 목록 조회
     *
     * @param userId 사용자 ID (PathVariable)
     * @return 미완료 할 일 목록
     */
    @GetMapping("/incomplete")
    public ResponseEntity<List<TodosResponse>> getIncompleteTodos(
            @PathVariable Long userId) {

        log.info("GET /api/users/{}/todos/incomplete - Fetching incomplete todos", userId);

        List<TodosResponse> incompleteTodos = todoService.getIncompleteTodos(userId);

        return ResponseEntity.ok(incompleteTodos);
    }
}