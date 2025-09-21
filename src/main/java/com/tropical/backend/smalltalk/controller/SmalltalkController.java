package com.tropical.backend.smalltalk.controller;

import com.tropical.backend.smalltalk.service.SmallTalkService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/v1/")
@RequiredArgsConstructor
@Tag(name = "Smalltalk", description = "스몰토크 API")
public class SmalltalkController {

    private SmallTalkService smallTalkService;

}
