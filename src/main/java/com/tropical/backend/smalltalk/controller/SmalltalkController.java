package com.tropical.backend.smalltalk.controller;

import com.tropical.backend.common.dto.response.ApiResponse;
import com.tropical.backend.smalltalk.dto.response.TopicResponse;
import com.tropical.backend.smalltalk.service.SmallTalkService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/v1/")
@RequiredArgsConstructor
@Tag(name = "Smalltalk", description = "스몰토크 API")
public class SmalltalkController {

    private final SmallTalkService smallTalkService;

    @GetMapping("smalltalk")
    @Tag(name = "Smalltalk", description = "스몰토크 조회 API, 최초 진입시 더미데이터 반환")
    public ResponseEntity<?> getSmallTalks(@AuthenticationPrincipal UserDetails userDetails) {

        // 인증된 사용자 정보 검증
        if (userDetails == null) {
            throw new IllegalArgumentException("인증된 사용자 정보가 필요합니다");
        }

        Long userId = Long.valueOf(userDetails.getUsername());

        TopicResponse smallTalks = smallTalkService.getSmallTalks(userId);

        return ResponseEntity.ok().body(ApiResponse.success("스몰토크 주제 조회를 성공했습니다.",smallTalks));
    }
}
