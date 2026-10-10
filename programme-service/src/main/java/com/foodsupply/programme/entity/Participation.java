package com.foodsupply.programme.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "participations",
        uniqueConstraints = @UniqueConstraint(columnNames = {"programme_id", "beneficiaryId"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Participation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "programme_id", nullable = false)
    private NutritionProgramme programme;

    @Column(nullable = false)
    private Long beneficiaryId;

    @Column(nullable = false, length = 150)
    private String beneficiaryName;

    @Column(nullable = false)
    private int peopleCovered;

    @Column(nullable = false)
    private LocalDate enrolledOn;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private ParticipationStatus status = ParticipationStatus.ENROLLED;
}
