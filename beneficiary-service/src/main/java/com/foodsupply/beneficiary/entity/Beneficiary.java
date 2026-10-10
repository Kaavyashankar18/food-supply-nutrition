package com.foodsupply.beneficiary.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(name = "beneficiaries", indexes = {
        @Index(name = "idx_beneficiary_category", columnList = "category"),
        @Index(name = "idx_beneficiary_status", columnList = "status"),
        @Index(name = "idx_beneficiary_type", columnList = "type")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Beneficiary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String fullName;

    /** INDIVIDUAL or a group (school, orphanage...). Groups are served as one unit. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BeneficiaryType type;

    /** Number of people covered by this record: 1 for an individual, the head-count for a group. */
    @Column(nullable = false)
    private Integer peopleServed;

    /** Only meaningful for INDIVIDUAL records. */
    private Integer age;

    @Column(length = 10)
    private String gender;

    /** Target group; decides which nutrition requirement applies (a school is STUDENT, an orphanage CHILD...). */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private BeneficiaryCategory category;

    @Column(length = 255)
    private String address;

    @Column(length = 20)
    private String contactNumber;

    private Integer householdSize;

    @Column(length = 255)
    private String healthCondition;

    @Column(length = 30)
    private String dietaryPreference;

    @Column(length = 255)
    private String dietaryRestrictions;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private BeneficiaryStatus status;

    @Column(nullable = false)
    private boolean eligible;

    @Column(length = 255)
    private String eligibilityNotes;

    private LocalDate eligibilityVerifiedOn;

    @Column(nullable = false)
    private LocalDate registeredOn;

    @PrePersist
    void onCreate() {
        if (registeredOn == null) {
            registeredOn = LocalDate.now();
        }
        if (status == null) {
            status = BeneficiaryStatus.ACTIVE;
        }
        if (type == null) {
            type = BeneficiaryType.INDIVIDUAL;
        }
        if (peopleServed == null) {
            peopleServed = 1;
        }
    }
}
