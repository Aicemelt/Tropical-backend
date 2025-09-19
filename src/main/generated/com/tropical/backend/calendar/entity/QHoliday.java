package com.tropical.backend.calendar.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QHoliday is a Querydsl query type for Holiday
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QHoliday extends EntityPathBase<Holiday> {

    private static final long serialVersionUID = 1230700678L;

    public static final QHoliday holiday = new QHoliday("holiday");

    public final StringPath countryCode = createString("countryCode");

    public final DateTimePath<java.time.LocalDateTime> createdAt = createDateTime("createdAt", java.time.LocalDateTime.class);

    public final DatePath<java.time.LocalDate> endDate = createDate("endDate", java.time.LocalDate.class);

    public final StringPath externalId = createString("externalId");

    public final StringPath externalSource = createString("externalSource");

    public final EnumPath<Holiday.HolidayType> holidayType = createEnum("holidayType", Holiday.HolidayType.class);

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final BooleanPath isSubstitute = createBoolean("isSubstitute");

    public final StringPath nameKo = createString("nameKo");

    public final DatePath<java.time.LocalDate> startDate = createDate("startDate", java.time.LocalDate.class);

    public final DateTimePath<java.time.LocalDateTime> updatedAt = createDateTime("updatedAt", java.time.LocalDateTime.class);

    public final NumberPath<Short> year = createNumber("year", Short.class);

    public QHoliday(String variable) {
        super(Holiday.class, forVariable(variable));
    }

    public QHoliday(Path<? extends Holiday> path) {
        super(path.getType(), path.getMetadata());
    }

    public QHoliday(PathMetadata metadata) {
        super(Holiday.class, metadata);
    }

}

