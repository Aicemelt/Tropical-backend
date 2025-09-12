package com.tropical.backend.auth.entity;

import com.tropical.backend.bucketList.entity.BucketList;
import com.tropical.backend.diary.entity.Diary;
import com.tropical.backend.schedule.entity.Schedule;
import com.tropical.backend.smalltalk.entity.SmalltalkTopic;
import com.tropical.backend.todo.entity.Todo;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "user")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"socialAccounts", "userConsents", "schedules", "diaries", "todos", "bucketLists", "smalltalkTopics"})
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String email;

    @Column(name = "password_hash")
    private String passwordHash;

    @Column(name = "email_verified", nullable = false)
    @Builder.Default
    private Boolean emailVerified = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "account_type", nullable = false)
    private AccountType accountType;

    @Column(nullable = false, unique = true, length = 50)
    private String nickname;

    @Column(name = "birth_date")
    private LocalDate birthDate;

    @Column(name = "onboarding_completed", nullable = false)
    @Builder.Default
    private Boolean onboardingCompleted = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private UserStatus status = UserStatus.ACTIVE;

    @Enumerated(EnumType.STRING)
    @Column(name = "week_start", nullable = false)
    @Builder.Default
    private WeekStart weekStart = WeekStart.MON;

    @Column(nullable = false)
    @Builder.Default
    private String timezone = "Asia/Seoul";

    @Column(name = "show_holidays", nullable = false)
    @Builder.Default
    private Boolean showHolidays = true;

    @Column(name = "smalltalk_notification_enabled", nullable = false)
    @Builder.Default
    private Boolean smalltalkNotificationEnabled = true;

    @Column(name = "smalltalk_notification_time", nullable = false)
    @Builder.Default
    private LocalTime smalltalkNotificationTime = LocalTime.of(8, 0);

    @Column(name = "smalltalk_notification_days", nullable = false)
    @Builder.Default
    private String smalltalkNotificationDays = "daily";

    @Column(name = "schedule_notification_enabled", nullable = false)
    @Builder.Default
    private Boolean scheduleNotificationEnabled = true;

    @Column(name = "schedule_notification_minutes", nullable = false)
    @Builder.Default
    private Integer scheduleNotificationMinutes = 30;

    @Column(name = "todo_notification_enabled", nullable = false)
    @Builder.Default
    private Boolean todoNotificationEnabled = true;

    @Column(name = "todo_notification_time", nullable = false)
    @Builder.Default
    private LocalTime todoNotificationTime = LocalTime.of(8, 0);

    @Column(name = "todo_notification_days_before", nullable = false)
    @Builder.Default
    private Integer todoNotificationDaysBefore = 1;

    // 연관관계
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<SocialAccount> socialAccounts = new ArrayList<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<UserConsent> userConsents = new ArrayList<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Schedule> schedules = new ArrayList<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Diary> diaries = new ArrayList<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Todo> todos = new ArrayList<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<BucketList> bucketLists = new ArrayList<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<SmalltalkTopic> smalltalkTopics = new ArrayList<>();


    // Enum Classes
    public enum AccountType {
        LOCAL, SOCIAL
    }

    public enum UserStatus {
        ACTIVE, BLOCKED, DELETED
    }

    public enum WeekStart {
        SUN, MON
    }
}

