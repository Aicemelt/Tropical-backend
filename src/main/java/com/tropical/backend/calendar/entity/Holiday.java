package com.tropical.backend.calendar.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "holiday")
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Holiday {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "country_code", nullable = false, length = 2)
    @Builder.Default
    private String countryCode = "KR";

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "name_ko", nullable = false, length = 100)
    private String nameKo;

    @Column(name = "is_substitute", nullable = false)
    @Builder.Default
    private Boolean isSubstitute = false;

    @Column(nullable = false)
    private Short year;
}
