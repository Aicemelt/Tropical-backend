package com.tropical.backend.auth.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QUserConsent is a Querydsl query type for UserConsent
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QUserConsent extends EntityPathBase<UserConsent> {

    private static final long serialVersionUID = 56938707L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QUserConsent userConsent = new QUserConsent("userConsent");

    public final BooleanPath agreed = createBoolean("agreed");

    public final DateTimePath<java.time.LocalDateTime> agreedAt = createDateTime("agreedAt", java.time.LocalDateTime.class);

    public final EnumPath<UserConsent.ConsentType> consentType = createEnum("consentType", UserConsent.ConsentType.class);

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final QTerms terms;

    public final StringPath termsVersionSnapshot = createString("termsVersionSnapshot");

    public final QUser user;

    public QUserConsent(String variable) {
        this(UserConsent.class, forVariable(variable), INITS);
    }

    public QUserConsent(Path<? extends UserConsent> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QUserConsent(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QUserConsent(PathMetadata metadata, PathInits inits) {
        this(UserConsent.class, metadata, inits);
    }

    public QUserConsent(Class<? extends UserConsent> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.terms = inits.isInitialized("terms") ? new QTerms(forProperty("terms")) : null;
        this.user = inits.isInitialized("user") ? new QUser(forProperty("user")) : null;
    }

}

