package com.foodsupply.distribution.mapper;

import com.foodsupply.distribution.dto.AllocationResponse;
import com.foodsupply.distribution.dto.CentreResponse;
import com.foodsupply.distribution.dto.ItemResponse;
import com.foodsupply.distribution.dto.PlanListItem;
import com.foodsupply.distribution.dto.PlanResponse;
import com.foodsupply.distribution.dto.RecordResponse;
import com.foodsupply.distribution.dto.TargetResponse;
import com.foodsupply.distribution.entity.DistributionCentre;
import com.foodsupply.distribution.entity.DistributionItem;
import com.foodsupply.distribution.entity.DistributionPlan;
import com.foodsupply.distribution.entity.DistributionRecord;

import java.util.List;

public final class PlanMapper {

    private PlanMapper() {
    }

    public static CentreResponse toCentre(DistributionCentre c) {
        return new CentreResponse(c.getId(), c.getName(), c.getAddress(), c.getContactPerson(), c.getPhone(), c.getStatus());
    }

    public static ItemResponse toItem(DistributionItem i) {
        List<AllocationResponse> allocations = i.getAllocations().stream()
                .map(a -> new AllocationResponse(a.getBatchId(), a.getBatchNumber(), a.getExpiryDate(), a.getQuantity()))
                .toList();
        return new ItemResponse(i.getFoodItemId(), i.getFoodItemName(), i.getUnit(), i.getPlannedQty(),
                i.getDispatchedQty(), i.getDeliveredQty(), allocations);
    }

    public static PlanResponse toPlan(DistributionPlan p) {
        List<ItemResponse> items = p.getItems().stream().map(PlanMapper::toItem).toList();
        List<TargetResponse> targets = p.getBeneficiaries().stream()
                .map(b -> new TargetResponse(b.getBeneficiaryId(), b.getBeneficiaryName(), b.getPeopleCovered()))
                .toList();
        return new PlanResponse(p.getId(), p.getPlanNumber(), p.getProgrammeId(), p.getProgrammeName(),
                p.getCentre().getId(), p.getCentre().getName(), p.getStartDate(), p.getEndDate(), p.getStatus(),
                p.getRemarks(), p.getCreatedOn(), p.getDispatchedOn(), p.getDeliveredOn(), p.getEnrolledPeople(),
                items, targets);
    }

    public static PlanListItem toListItem(DistributionPlan p) {
        return new PlanListItem(p.getId(), p.getPlanNumber(), p.getProgrammeId(), p.getProgrammeName(),
                p.getCentre().getId(), p.getCentre().getName(), p.getStartDate(), p.getEndDate(), p.getStatus(),
                p.getEnrolledPeople(), p.getItems().size());
    }

    public static RecordResponse toRecord(DistributionRecord r) {
        return new RecordResponse(r.getId(), r.getPlan().getId(), r.getPlan().getPlanNumber(), r.getBeneficiaryId(),
                r.getBeneficiaryName(), r.getFoodItemId(), r.getFoodItemName(), r.getUnit(), r.getQuantity(),
                r.getDeliveredOn(), r.getStatus());
    }
}
