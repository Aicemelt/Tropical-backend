package com.tropical.backend.smalltalk.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Service
@RequiredArgsConstructor
public class EmbeddingService {

    private final WebClient webClient;
}
