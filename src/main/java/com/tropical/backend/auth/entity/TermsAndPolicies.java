package com.tropical.backend.auth.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "terms_and_policies")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Builder
public class TermsAndPolicies {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PolicyType type;

    @Column(nullable = false, length = 20)
    private String version;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, columnDefinition = "LONGTEXT")
    private String content;

    @Column(name = "effective_date", nullable = false)
    private LocalDateTime effectiveDate;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;


    public enum PolicyType {
        TERMS_OF_SERVICE,
        PRIVACY_POLICY,
        CALENDAR_PERSONALIZATION,
        DIARY_PERSONALIZATION,
        TODO_PERSONALIZATION,
        BUCKET_PERSONALIZATION
    }

}

