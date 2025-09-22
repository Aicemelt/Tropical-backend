package com.tropical.backend.bucketList.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QBucketList is a Querydsl query type for BucketList
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QBucketList extends EntityPathBase<BucketList> {

    private static final long serialVersionUID = 1367118052L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QBucketList bucketList = new QBucketList("bucketList");

    public final NumberPath<Long> bucketId = createNumber("bucketId", Long.class);

    public final StringPath content = createString("content");

    public final DateTimePath<java.time.LocalDateTime> createdAt = createDateTime("createdAt", java.time.LocalDateTime.class);

    public final BooleanPath isCompleted = createBoolean("isCompleted");

    public final DateTimePath<java.time.LocalDateTime> updatedAt = createDateTime("updatedAt", java.time.LocalDateTime.class);

    public final com.tropical.backend.auth.entity.QUser user;

    public QBucketList(String variable) {
        this(BucketList.class, forVariable(variable), INITS);
    }

    public QBucketList(Path<? extends BucketList> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QBucketList(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QBucketList(PathMetadata metadata, PathInits inits) {
        this(BucketList.class, metadata, inits);
    }

    public QBucketList(Class<? extends BucketList> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.user = inits.isInitialized("user") ? new com.tropical.backend.auth.entity.QUser(forProperty("user")) : null;
    }

}

