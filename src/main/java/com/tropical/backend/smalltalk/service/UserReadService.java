package com.tropical.backend.smalltalk.service;

import com.tropical.backend.auth.entity.User;
import com.tropical.backend.auth.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class UserReadService {

    private final UserRepository userRepository;

    /**
     * 이메일로 사용자를 확인하는 메소드입니다.
     * @param email
     * @return User
     */
    public User getUserByEmail(String email) {
        return userRepository.findByEmail(email).orElseThrow(
                () -> new RuntimeException("사용자 없음")
        );
    }

    public List<User> getAllUser() {
        return userRepository.findAll();
    }

    public User getUserById(Long userId) {
        return userRepository.findById(userId).orElseThrow();
    }
}
