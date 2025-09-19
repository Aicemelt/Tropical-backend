package com.tropical.backend.auth.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QUser is a Querydsl query type for User
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QUser extends EntityPathBase<User> {

    private static final long serialVersionUID = 2099763527L;

    public static final QUser user = new QUser("user");

    public final EnumPath<User.AccountType> accountType = createEnum("accountType", User.AccountType.class);

    public final DatePath<java.time.LocalDate> birthDate = createDate("birthDate", java.time.LocalDate.class);

    public final ListPath<com.tropical.backend.bucketList.entity.BucketList, com.tropical.backend.bucketList.entity.QBucketList> bucketLists = this.<com.tropical.backend.bucketList.entity.BucketList, com.tropical.backend.bucketList.entity.QBucketList>createList("bucketLists", com.tropical.backend.bucketList.entity.BucketList.class, com.tropical.backend.bucketList.entity.QBucketList.class, PathInits.DIRECT2);

    public final DateTimePath<java.time.LocalDateTime> createdAt = createDateTime("createdAt", java.time.LocalDateTime.class);

    public final EnumPath<User.DateSystem> dateSystem = createEnum("dateSystem", User.DateSystem.class);

    public final ListPath<com.tropical.backend.diary.entity.Diary, com.tropical.backend.diary.entity.QDiary> diaries = this.<com.tropical.backend.diary.entity.Diary, com.tropical.backend.diary.entity.QDiary>createList("diaries", com.tropical.backend.diary.entity.Diary.class, com.tropical.backend.diary.entity.QDiary.class, PathInits.DIRECT2);

    public final StringPath email = createString("email");

    public final BooleanPath emailVerified = createBoolean("emailVerified");

    public final DateTimePath<java.time.LocalDateTime> emailVerifiedAt = createDateTime("emailVerifiedAt", java.time.LocalDateTime.class);

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final DateTimePath<java.time.LocalDateTime> lastLoginAt = createDateTime("lastLoginAt", java.time.LocalDateTime.class);

    public final StringPath nickname = createString("nickname");

    public final BooleanPath onboardingCompleted = createBoolean("onboardingCompleted");

    public final StringPath passwordHash = createString("passwordHash");

    public final BooleanPath scheduleNotificationEnabled = createBoolean("scheduleNotificationEnabled");

    public final NumberPath<Integer> scheduleNotificationMinutes = createNumber("scheduleNotificationMinutes", Integer.class);

    public final ListPath<com.tropical.backend.schedule.entity.Schedule, com.tropical.backend.schedule.entity.QSchedule> schedules = this.<com.tropical.backend.schedule.entity.Schedule, com.tropical.backend.schedule.entity.QSchedule>createList("schedules", com.tropical.backend.schedule.entity.Schedule.class, com.tropical.backend.schedule.entity.QSchedule.class, PathInits.DIRECT2);

    public final BooleanPath showHolidays = createBoolean("showHolidays");

    public final StringPath smalltalkNotificationDays = createString("smalltalkNotificationDays");

    public final BooleanPath smalltalkNotificationEnabled = createBoolean("smalltalkNotificationEnabled");

    public final TimePath<java.time.LocalTime> smalltalkNotificationTime = createTime("smalltalkNotificationTime", java.time.LocalTime.class);

    public final ListPath<com.tropical.backend.smalltalk.entity.SmalltalkTopic, com.tropical.backend.smalltalk.entity.QSmalltalkTopic> smalltalkTopics = this.<com.tropical.backend.smalltalk.entity.SmalltalkTopic, com.tropical.backend.smalltalk.entity.QSmalltalkTopic>createList("smalltalkTopics", com.tropical.backend.smalltalk.entity.SmalltalkTopic.class, com.tropical.backend.smalltalk.entity.QSmalltalkTopic.class, PathInits.DIRECT2);

    public final ListPath<SocialAccount, QSocialAccount> socialAccounts = this.<SocialAccount, QSocialAccount>createList("socialAccounts", SocialAccount.class, QSocialAccount.class, PathInits.DIRECT2);

    public final EnumPath<User.UserStatus> status = createEnum("status", User.UserStatus.class);

    public final StringPath timezone = createString("timezone");

    public final NumberPath<Integer> todoNotificationDaysBefore = createNumber("todoNotificationDaysBefore", Integer.class);

    public final BooleanPath todoNotificationEnabled = createBoolean("todoNotificationEnabled");

    public final TimePath<java.time.LocalTime> todoNotificationTime = createTime("todoNotificationTime", java.time.LocalTime.class);

    public final ListPath<com.tropical.backend.todo.entity.Todo, com.tropical.backend.todo.entity.QTodo> todos = this.<com.tropical.backend.todo.entity.Todo, com.tropical.backend.todo.entity.QTodo>createList("todos", com.tropical.backend.todo.entity.Todo.class, com.tropical.backend.todo.entity.QTodo.class, PathInits.DIRECT2);

    public final ListPath<UserConsent, QUserConsent> userConsents = this.<UserConsent, QUserConsent>createList("userConsents", UserConsent.class, QUserConsent.class, PathInits.DIRECT2);

    public final EnumPath<User.WeekStart> weekStart = createEnum("weekStart", User.WeekStart.class);

    public QUser(String variable) {
        super(User.class, forVariable(variable));
    }

    public QUser(Path<? extends User> path) {
        super(path.getType(), path.getMetadata());
    }

    public QUser(PathMetadata metadata) {
        super(User.class, metadata);
    }

}

