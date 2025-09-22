package com.tropical.backend.config;

import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class QueryDslConfig {

    private final EntityManager em;

    /**
     * JPAQueryFactory Bean 등록
     * QueryDSL을 사용하기 위한 핵심 컴포넌트
     */
    @Bean
    public JPAQueryFactory factory() {
        return new JPAQueryFactory(em);
    }

}
