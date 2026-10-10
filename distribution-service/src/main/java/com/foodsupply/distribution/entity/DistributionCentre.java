package com.foodsupply.distribution.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "distribution_centres")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class DistributionCentre {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 150)
    private String name;

    @Column(length = 300)
    private String address;

    @Column(length = 100)
    private String contactPerson;

    @Column(length = 20)
    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    @Builder.Default
    private CentreStatus status = CentreStatus.ACTIVE;
}
