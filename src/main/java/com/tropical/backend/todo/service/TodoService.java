package com.tropical.backend.todo.service;

import com.tropical.backend.auth.entity.User;
import com.tropical.backend.auth.repository.UserRepository;
import com.tropical.backend.todo.dto.request.TodosCreateRequest;
import com.tropical.backend.todo.dto.request.TodosUpdateRequest;
import com.tropical.backend.todo.dto.request.TodosCompleteRequest;
import com.tropical.backend.todo.dto.response.TodosResponse;
import com.tropical.backend.todo.entity.Todo;
import com.tropical.backend.todo.repository.TodoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Todo 비즈니스 로직 서비스
 *
 * @author 백승현
 * @version 1.0
 * @since 2025.09.16
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class TodoService {

    private final TodoRepository todoRepository;
    private final UserRepository userRepository;

    /**
     * 새 할 일 생성
     *
     * @param userId 사용자 ID
     * @param request 생성 요청 DTO
     * @return 생성된 할 일 응답 DTO
     */
    @Transactional
    public TodosResponse createTodo(Long userId, TodosCreateRequest request) {
        log.info("Creating todo for user: {}", userId);

        User user = userRepository.findByIdAndActive(userId)
                .orElseThrow(() -> new RuntimeException("활성 사용자를 찾을 수 없습니다."));

        Todo todo = Todo.builder()
                .user(user)
                .content(request.getContent())
                .dueDate(request.getDueDate())
                .build();

        Todo savedTodo = todoRepository.save(todo);
        log.info("Todo created successfully with ID: {}", savedTodo.getTodoId());

        return convertToResponse(savedTodo);
    }

    /**
     * 사용자의 모든 할 일 조회
     * 1순위: 마감기한이 적게 남은 순 (null인 경우 마지막으로)
     * 2순위: 등록일이 빠른 순
     *
     * @param userId 사용자 ID
     * @return 할 일 목록
     */
    public List<TodosResponse> getAllTodos(Long userId) {
        log.info("Fetching all todos for user: {}", userId);

        User user = userRepository.findByIdAndActive(userId)
                .orElseThrow(() -> new RuntimeException("활성 사용자를 찾을 수 없습니다."));

        List<Todo> todos = todoRepository.findByUserOrderByDueDateAndCreatedAt(user);

        return todos.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    /**
     * 특정 할 일 조회
     *
     * @param userId 사용자 ID
     * @param todoId 할 일 ID
     * @return 할 일 응답 DTO
     */
    public TodosResponse getTodoById(Long userId, Long todoId) {
        log.info("Fetching todo {} for user: {}", todoId, userId);

        Todo todo = todoRepository.findByTodoIdAndUserId(todoId, userId)
                .orElseThrow(() -> new RuntimeException("할 일을 찾을 수 없습니다."));

        return convertToResponse(todo);
    }

    /**
     * 할 일 수정 (부분 업데이트)
     *
     * @param userId 사용자 ID
     * @param todoId 할 일 ID
     * @param request 수정 요청 DTO
     * @return 수정된 할 일 응답 DTO
     */
    @Transactional
    public TodosResponse updateTodo(Long userId, Long todoId, TodosUpdateRequest request) {
        log.info("Updating todo {} for user: {}", todoId, userId);

        Todo todo = todoRepository.findByTodoIdAndUserId(todoId, userId)
                .orElseThrow(() -> new RuntimeException("할 일을 찾을 수 없습니다."));

        // content가 null이 아니고 빈 값이 아닌 경우에만 업데이트
        if (request.getContent() != null && !request.getContent().trim().isEmpty()) {
            todo.setContent(request.getContent().trim());
            log.info("Content updated for todo {}", todoId);
        }

        // dueDate가 요청에 포함된 경우에만 업데이트 (null 값도 설정 가능)
        if (request.getDueDate() != null) {
            todo.setDueDate(request.getDueDate());
            log.info("Due date updated for todo {}", todoId);
        }

        // 둘 다 업데이트할 내용이 없으면 예외 발생
        if ((request.getContent() == null || request.getContent().trim().isEmpty()) &&
                request.getDueDate() == null) {
            throw new RuntimeException("수정할 내용이 없습니다.");
        }

        Todo updatedTodo = todoRepository.save(todo);
        log.info("Todo {} updated successfully", todoId);

        return convertToResponse(updatedTodo);
    }

    /**
     * 할 일 마감일 제거
     *
     * @param userId 사용자 ID
     * @param todoId 할 일 ID
     * @return 수정된 할 일 응답 DTO
     */
    @Transactional
    public TodosResponse removeDueDate(Long userId, Long todoId) {
        log.info("Removing due date from todo {} for user: {}", todoId, userId);

        Todo todo = todoRepository.findByTodoIdAndUserId(todoId, userId)
                .orElseThrow(() -> new RuntimeException("할 일을 찾을 수 없습니다."));

        todo.setDueDate(null);

        Todo updatedTodo = todoRepository.save(todo);
        log.info("Due date removed from todo {} successfully", todoId);

        return convertToResponse(updatedTodo);
    }

    /**
     * 사용자의 완료된 할 일 목록 조회
     *
     * @param userId 사용자 ID
     * @return 완료된 할 일 목록 (최신순)
     */
    public List<TodosResponse> getCompletedTodos(Long userId) {
        log.info("Fetching completed todos for user: {}", userId);

        User user = userRepository.findByIdAndActive(userId)
                .orElseThrow(() -> new RuntimeException("활성 사용자를 찾을 수 없습니다."));

        List<Todo> completedTodos = todoRepository.findByUserAndIsCompletedTrueOrderByCreatedAtDesc(user);

        return completedTodos.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    /**
     * 사용자의 마감기한이 지난 미완료 할 일 목록 조회
     *
     * @param userId 사용자 ID
     * @return 마감기한이 지난 미완료 할 일 목록 (마감일 오름차순)
     */
    public List<TodosResponse> getOverdueTodos(Long userId) {
        log.info("Fetching overdue todos for user: {}", userId);

        User user = userRepository.findByIdAndActive(userId)
                .orElseThrow(() -> new RuntimeException("활성 사용자를 찾을 수 없습니다."));

        LocalDate today = LocalDate.now();
        List<Todo> overdueTodos = todoRepository.findOverdueTodos(user, today.minusDays(1)); // 어제까지를 과거로 간주

        return overdueTodos.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    /**
     * 사용자의 미완료 할 일 목록 조회
     *
     * @param userId 사용자 ID
     * @return 미완료 할 일 목록 (최신순)
     */
    public List<TodosResponse> getIncompleteTodos(Long userId) {
        log.info("Fetching incomplete todos for user: {}", userId);

        User user = userRepository.findByIdAndActive(userId)
                .orElseThrow(() -> new RuntimeException("활성 사용자를 찾을 수 없습니다."));

        List<Todo> incompleteTodos = todoRepository.findByUserAndIsCompletedFalseOrderByCreatedAtDesc(user);

        return incompleteTodos.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    /**
     * 할 일 삭제
     *
     * @param userId 사용자 ID
     * @param todoId 할 일 ID
     */
    @Transactional
    public void deleteTodo(Long userId, Long todoId) {
        log.info("Deleting todo {} for user: {}", todoId, userId);

        Todo todo = todoRepository.findByTodoIdAndUserId(todoId, userId)
                .orElseThrow(() -> new RuntimeException("할 일을 찾을 수 없습니다."));

        todoRepository.delete(todo);
        log.info("Todo {} deleted successfully", todoId);
    }

    /**
     * 할 일 완료/미완료 처리
     *
     * @param userId 사용자 ID
     * @param todoId 할 일 ID
     * @param request 완료 처리 요청 DTO
     * @return 수정된 할 일 응답 DTO
     */
    @Transactional
    public TodosResponse completeTodo(Long userId, Long todoId, TodosCompleteRequest request) {
        log.info("Updating completion status of todo {} for user: {} to {}",
                todoId, userId, request.getIsCompleted());

        Todo todo = todoRepository.findByTodoIdAndUserId(todoId, userId)
                .orElseThrow(() -> new RuntimeException("할 일을 찾을 수 없습니다."));

        todo.setIsCompleted(request.getIsCompleted());

        Todo updatedTodo = todoRepository.save(todo);
        log.info("Todo {} completion status updated successfully", todoId);

        return convertToResponse(updatedTodo);
    }

    /**
     * Todo 엔터티를 응답 DTO로 변환
     *
     * @param todo Todo 엔터티
     * @return TodosResponse
     */
    private TodosResponse convertToResponse(Todo todo) {
        return TodosResponse.builder()
                .todoId(todo.getTodoId())
                .content(todo.getContent())
                .dueDate(todo.getDueDate())
                .isCompleted(todo.getIsCompleted())
                .createdAt(todo.getCreatedAt())
                .updatedAt(todo.getUpdatedAt())
                .build();
    }
}