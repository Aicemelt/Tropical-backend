package com.tropical.backend.smalltalk.entity;

import com.tropical.backend.smalltalk.enums.SourceType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "smalltalk_sources")
@Getter
@ToString(exclude = {"smalltalkTopic"})
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SmalltalkSources {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false)
    private SourceType sourceType;

    // 주제의 기반이 되는 id, 보편적 주제는 기반이 없기 때문에 sourceId는 null 값을 허용
    @Column(name = "source_id")
    private Long sourceId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    // 연관 관계 엔터티
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "smalltalk_id", nullable = false)
    private SmalltalkTopic smalltalkTopic;

}

