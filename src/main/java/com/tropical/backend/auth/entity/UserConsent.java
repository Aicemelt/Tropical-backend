package com.tropical.backend.auth.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_consent")
@Getter
@ToString(exclude = {"user"})
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserConsent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "consent_type", nullable = false)
    private ConsentType consentType;

    @Column(nullable = false)
    private Boolean agreed;

    @CreationTimestamp
    @Column(name = "agreed_at", nullable = false)
    private LocalDateTime agreedAt;

    public enum ConsentType {
        TERMS_OF_SERVICE,
        CALENDAR_PERSONALIZATION,
        DIARY_PERSONALIZATION,
        TODO_PERSONALIZATION,
        BUCKET_PERSONALIZATION
    }

}

