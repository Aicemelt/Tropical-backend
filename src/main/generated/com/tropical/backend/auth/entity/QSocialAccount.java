package com.tropical.backend.auth.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QSocialAccount is a Querydsl query type for SocialAccount
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QSocialAccount extends EntityPathBase<SocialAccount> {

    private static final long serialVersionUID = -1043807228L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QSocialAccount socialAccount = new QSocialAccount("socialAccount");

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final DateTimePath<java.time.LocalDateTime> linkedAt = createDateTime("linkedAt", java.time.LocalDateTime.class);

    public final EnumPath<SocialAccount.SocialProvider> provider = createEnum("provider", SocialAccount.SocialProvider.class);

    public final StringPath providerUserId = createString("providerUserId");

    public final QUser user;

    public QSocialAccount(String variable) {
        this(SocialAccount.class, forVariable(variable), INITS);
    }

    public QSocialAccount(Path<? extends SocialAccount> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QSocialAccount(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QSocialAccount(PathMetadata metadata, PathInits inits) {
        this(SocialAccount.class, metadata, inits);
    }

    public QSocialAccount(Class<? extends SocialAccount> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.user = inits.isInitialized("user") ? new QUser(forProperty("user")) : null;
    }

}

