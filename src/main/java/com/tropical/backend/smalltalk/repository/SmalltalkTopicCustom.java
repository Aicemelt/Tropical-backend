package com.tropical.backend.smalltalk.repository;

import com.tropical.backend.smalltalk.entity.SmalltalkTopic;

import java.util.List;

public interface SmalltalkTopicCustom {

    // 유저 id로 db에 저장된 스몰토크 limit 개수에 맞게 조회
    List<SmalltalkTopic> findSmalltalkTopicsByUserId(Long id, int limit);

    List<SmalltalkTopic> findAllSmalltalkTopicsByUserId(Long id);
}
