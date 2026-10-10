package com.foodsupply.beneficiary.service;

import com.foodsupply.beneficiary.dto.BeneficiaryDto;
import com.foodsupply.beneficiary.dto.CategorySummary;
import com.foodsupply.beneficiary.dto.EligibilityResponse;
import com.foodsupply.beneficiary.dto.PageResponse;
import com.foodsupply.beneficiary.entity.BeneficiaryCategory;
import com.foodsupply.beneficiary.entity.BeneficiaryStatus;
import com.foodsupply.beneficiary.entity.BeneficiaryType;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;

public interface BeneficiaryService {

    PageResponse<BeneficiaryDto> search(String q, BeneficiaryCategory category, BeneficiaryType type,
                                        BeneficiaryStatus status, Boolean eligible, Pageable pageable);

    BeneficiaryDto get(Long id);

    BeneficiaryDto create(BeneficiaryDto dto);

    BeneficiaryDto update(Long id, BeneficiaryDto dto);

    void delete(Long id);

    /** Evaluates the rules without saving anything. */
    EligibilityResponse checkEligibility(Long id);

    /** Evaluates the rules, saves the outcome and stamps the verification date. */
    EligibilityResponse verifyEligibility(Long id);

    /** Activate or deactivate (a deactivated beneficiary is never eligible). */
    BeneficiaryDto changeStatus(Long id, BeneficiaryStatus status);

    List<BeneficiaryDto> getByIds(List<Long> ids);

    /** People covered per category (active + eligible) with each category's daily requirement. */
    List<CategorySummary> categorySummary();

    Map<String, Long> stats();
}
