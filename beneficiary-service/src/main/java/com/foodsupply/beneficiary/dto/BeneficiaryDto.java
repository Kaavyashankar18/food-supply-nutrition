package com.foodsupply.beneficiary.dto;

import com.foodsupply.beneficiary.entity.BeneficiaryCategory;
import com.foodsupply.beneficiary.entity.BeneficiaryStatus;
import com.foodsupply.beneficiary.entity.BeneficiaryType;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BeneficiaryDto {

    private Long id;

    @NotBlank(message = "Name is required")
    @Size(max = 120)
    private String fullName;

    /** Defaults to INDIVIDUAL when omitted. */
    private BeneficiaryType type;

    /** Required for groups (at least 1); forced to 1 for individuals. */
    @Min(value = 1, message = "People served must be at least 1")
    private Integer peopleServed;

    /** Required for INDIVIDUAL records (checked in the service). */
    @Min(value = 0, message = "Age cannot be negative")
    @Max(value = 120, message = "Age must be 120 or less")
    private Integer age;

    /** Required for INDIVIDUAL records (checked in the service). */
    @Pattern(regexp = "MALE|FEMALE|OTHER", message = "Gender must be MALE, FEMALE or OTHER")
    private String gender;

    @NotNull(message = "Category is required")
    private BeneficiaryCategory category;

    @Size(max = 255)
    private String address;

    @Pattern(regexp = "^$|^[0-9+\\- ]{7,20}$", message = "Contact number is not valid")
    private String contactNumber;

    @Min(value = 1, message = "Household size must be at least 1")
    private Integer householdSize;

    @Size(max = 255)
    private String healthCondition;

    @Size(max = 30)
    private String dietaryPreference;

    @Size(max = 255)
    private String dietaryRestrictions;

    private BeneficiaryStatus status;

    /** Read-only: calculated by the service from the eligibility rules. */
    private boolean eligible;

    private String eligibilityNotes;

    /** Read-only: set when eligibility is verified. */
    private LocalDate eligibilityVerifiedOn;

    private LocalDate registeredOn;
}
