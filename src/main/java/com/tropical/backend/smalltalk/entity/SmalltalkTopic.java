package com.tropical.backend.smalltalk.entity;

import com.tropical.backend.auth.entity.User;
import com.tropical.backend.smalltalk.dto.response.AISmallTalkResponse;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Entity
@Table(name = "smalltalk_topic")
@Getter
@ToString(exclude = {"user", "smalltalkSources"})
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SmalltalkTopic {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    @Column(name = "topic_type", nullable = false, length = 50)
    private String topicType;

    @Column(name = "topic_content", nullable = false, length = 200)
    private String topicContent;

    @Column(name = "example_question", nullable = false)
    private String exampleQuestion;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;



    // 연관 관계 엔터티
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Setter
    @OneToMany(mappedBy = "smalltalkTopic", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<SmalltalkSources> smalltalkSources = new ArrayList<>();


    // AI 응답 저장용 편의 메소드
    public static SmalltalkTopic toEntity(AISmallTalkResponse dto, User user) {

        SmalltalkTopic topic = SmalltalkTopic.builder()
                .topicType(dto.topicType())
                .topicContent(dto.topicContent())
                .exampleQuestion(dto.exampleQuestion())
                .user(user)
                .build();

        List<SmalltalkSources> sources = dto.sources().stream()
                .map(
                        aiSourceDto -> SmalltalkSources.builder()
                                .sourceId(aiSourceDto.sourceId())
                                .sourceType(aiSourceDto.sourceType())
                                .smalltalkTopic(topic)
                                .build()
                )
                .collect(Collectors.toList());

        topic.setSmalltalkSources(sources);

        return topic;

    }

}
