package com.tropical.backend.smalltalk.service;

import com.tropical.backend.auth.entity.User;
import com.tropical.backend.auth.repository.UserRepository;
import com.tropical.backend.smalltalk.dto.response.SmallTalkTopicDto;
import com.tropical.backend.smalltalk.dto.response.SmalltalkSourcesDto;
import com.tropical.backend.smalltalk.dto.response.TopicResponse;
import com.tropical.backend.smalltalk.entity.SmalltalkSources;
import com.tropical.backend.smalltalk.entity.SmalltalkTopic;
import com.tropical.backend.smalltalk.provider.WelcomeTopicProvider;
import com.tropical.backend.smalltalk.repository.SmalltalkTopicRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Transactional
@Service
@RequiredArgsConstructor
@Slf4j
public class SmallTalkService {

    private final UserRepository userRepository;
    private final SmalltalkTopicRepository smalltalkTopicRepository;

    private final UserActivityService userActivityService;
    private final UserReadService userReadService;

    @Transactional(readOnly = true)
    public TopicResponse getSmallTalks (String email) {

        // 1. 유저 검색
        User user = userReadService.getUserByEmail(email);
        Long userId = user.getId();

        // 2. 사용자의 활동을 확인
        boolean hasActivity = userActivityService.hasActivity(userId, user);

        // 2-2. 사용자가 받아온 스몰토크 최신순 5개 가져옴
        // 스몰토크 주제는 스케줄러에 따라 배치됨
        List<SmalltalkTopic> topics = smalltalkTopicRepository.findSmalltalkTopicsByUserId(userId, 5);

        // 3. 사용자 활동과 스몰토크가 없는 경우: 웰컴 주제 반환
        if(!hasActivity && topics.isEmpty()){
            log.info("사용자: {}님의 활동이 없습니다. 웰컴 주제를 반환합니다.", user);
            return getInitialTopics();
        }

        // 4. 사용자 활동이 있는 경우: dto 매핑 후 반환
        List<SmallTalkTopicDto> dtoList = topics.stream()
                .map(topic -> SmallTalkTopicDto.from(
                        topic,
                        (topic.getSmalltalkSources() == null ? List.<SmalltalkSources>of() : topic.getSmalltalkSources())
                                .stream()
                                .map(SmalltalkSourcesDto::from)
                                .collect(Collectors.toList())
                ))
                .toList();

        return new TopicResponse(dtoList);
    }


    /**
     * 회원가입 및 최초 진입 시 웰컴 주제(더미데이터) 반환용 메소드입니다.
     * @return TopicResponse
     */
    private TopicResponse getInitialTopics() {
        List<WelcomeTopicProvider.WelcomeTopicSeed> seeds = WelcomeTopicProvider.seeds();
        List<SmallTalkTopicDto> dtoList = new ArrayList<>();

        for(int i = 0; i < seeds.size(); i++) {
            SmallTalkTopicDto dto = SmallTalkTopicDto.from(seeds.get(i), i + 1L);
            dtoList.add(dto);
        }

        return new TopicResponse(dtoList);
    }
}
