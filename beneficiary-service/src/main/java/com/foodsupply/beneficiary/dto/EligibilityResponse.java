package com.foodsupply.beneficiary.dto;

import com.foodsupply.beneficiary.entity.BeneficiaryCategory;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EligibilityResponse {
    private Long beneficiaryId;
    private String fullName;
    private BeneficiaryCategory category;
    private boolean eligible;
    private String reason;
}
