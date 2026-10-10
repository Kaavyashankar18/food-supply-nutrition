package com.foodsupply.quality.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(name = "corrective_actions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CorrectiveAction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Local reference (same database) to a QualityInspection; nullable. */
    private Long inspectionId;

    /** Local reference (same database) to a WastageRecord; nullable. */
    private Long wastageId;

    @Column(nullable = false, length = 1000)
    private String description;

    @Column(nullable = false, length = 100)
    private String assignedTo;

    @Column(nullable = false)
    private LocalDate dueDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private ActionStatus status = ActionStatus.OPEN;

    @Column(nullable = false)
    @Builder.Default
    private LocalDate createdOn = LocalDate.now();

    private LocalDate completedOn;
}
