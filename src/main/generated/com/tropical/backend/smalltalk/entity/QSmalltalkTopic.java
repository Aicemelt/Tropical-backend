package com.tropical.backend.smalltalk.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QSmalltalkTopic is a Querydsl query type for SmalltalkTopic
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QSmalltalkTopic extends EntityPathBase<SmalltalkTopic> {

    private static final long serialVersionUID = -1750704011L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QSmalltalkTopic smalltalkTopic = new QSmalltalkTopic("smalltalkTopic");

    public final DateTimePath<java.time.LocalDateTime> createdAt = createDateTime("createdAt", java.time.LocalDateTime.class);

    public final StringPath embedding = createString("embedding");

    public final StringPath exampleQuestion = createString("exampleQuestion");

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final ListPath<SmalltalkSources, QSmalltalkSources> smalltalkSources = this.<SmalltalkSources, QSmalltalkSources>createList("smalltalkSources", SmalltalkSources.class, QSmalltalkSources.class, PathInits.DIRECT2);

    public final StringPath topicContent = createString("topicContent");

    public final StringPath topicType = createString("topicType");

    public final com.tropical.backend.auth.entity.QUser user;

    public QSmalltalkTopic(String variable) {
        this(SmalltalkTopic.class, forVariable(variable), INITS);
    }

    public QSmalltalkTopic(Path<? extends SmalltalkTopic> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QSmalltalkTopic(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QSmalltalkTopic(PathMetadata metadata, PathInits inits) {
        this(SmalltalkTopic.class, metadata, inits);
    }

    public QSmalltalkTopic(Class<? extends SmalltalkTopic> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.user = inits.isInitialized("user") ? new com.tropical.backend.auth.entity.QUser(forProperty("user")) : null;
    }

}

