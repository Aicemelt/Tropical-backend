package com.tropical.backend.smalltalk.repository;

import com.querydsl.jpa.impl.JPAQueryFactory;
import com.tropical.backend.smalltalk.entity.QSmalltalkTopic;
import com.tropical.backend.smalltalk.entity.SmalltalkTopic;
import lombok.RequiredArgsConstructor;

import java.util.List;

import static com.tropical.backend.smalltalk.entity.QSmalltalkSources.smalltalkSources;
import static com.tropical.backend.smalltalk.entity.QSmalltalkTopic.smalltalkTopic;

@RequiredArgsConstructor
public class SmalltalkTopicRepositoryImpl implements SmalltalkTopicCustom{

    private final JPAQueryFactory factory;

    @Override
    public List<SmalltalkTopic> findSmalltalkTopicsByUserId(Long id, int limit) {

        List<Long> ids = factory
                .select(smalltalkTopic.id)
                .from(smalltalkTopic)
                .orderBy(smalltalkTopic.createdAt.desc())
                .where(smalltalkTopic.user.id.eq(id))
                .limit(limit)
                .fetch();

        // 저장된 주제가 없으면 빈 배열 반환
        if (ids.isEmpty()) return List.of();

        // 저장된 주제가 있는 경우
        List<SmalltalkTopic> rows = factory
                .selectFrom(smalltalkTopic)
                .leftJoin(smalltalkTopic.smalltalkSources, smalltalkSources).fetchJoin()
                .where(smalltalkTopic.id.in(ids))
                .fetch();

        // IN은 정렬이 깨지므로 다시 최신순 정렬
        rows.sort((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()));
        return rows;

    }

    @Override
    public List<SmalltalkTopic> findAllSmalltalkTopicsByUserId(Long id) {
        return factory
                .selectFrom(smalltalkTopic)
                .where(smalltalkTopic.user.id.eq(id))
                .fetch();
    }
}
