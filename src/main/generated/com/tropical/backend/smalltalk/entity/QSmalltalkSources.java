package com.tropical.backend.smalltalk.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QSmalltalkSources is a Querydsl query type for SmalltalkSources
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QSmalltalkSources extends EntityPathBase<SmalltalkSources> {

    private static final long serialVersionUID = 318010750L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QSmalltalkSources smalltalkSources = new QSmalltalkSources("smalltalkSources");

    public final DateTimePath<java.time.LocalDateTime> createdAt = createDateTime("createdAt", java.time.LocalDateTime.class);

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final QSmalltalkTopic smalltalkTopic;

    public final NumberPath<Long> sourceId = createNumber("sourceId", Long.class);

    public final EnumPath<com.tropical.backend.smalltalk.enums.SourceType> sourceType = createEnum("sourceType", com.tropical.backend.smalltalk.enums.SourceType.class);

    public QSmalltalkSources(String variable) {
        this(SmalltalkSources.class, forVariable(variable), INITS);
    }

    public QSmalltalkSources(Path<? extends SmalltalkSources> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QSmalltalkSources(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QSmalltalkSources(PathMetadata metadata, PathInits inits) {
        this(SmalltalkSources.class, metadata, inits);
    }

    public QSmalltalkSources(Class<? extends SmalltalkSources> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.smalltalkTopic = inits.isInitialized("smalltalkTopic") ? new QSmalltalkTopic(forProperty("smalltalkTopic"), inits.get("smalltalkTopic")) : null;
    }

}

