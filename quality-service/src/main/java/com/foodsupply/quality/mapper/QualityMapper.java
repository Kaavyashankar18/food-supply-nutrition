package com.foodsupply.quality.mapper;

import com.foodsupply.quality.dto.ActionResponse;
import com.foodsupply.quality.dto.InspectionResponse;
import com.foodsupply.quality.dto.WastageResponse;
import com.foodsupply.quality.entity.ActionStatus;
import com.foodsupply.quality.entity.CorrectiveAction;
import com.foodsupply.quality.entity.QualityInspection;
import com.foodsupply.quality.entity.WastageRecord;

import java.time.LocalDate;

public final class QualityMapper {

    private QualityMapper() {
    }

    public static InspectionResponse toInspectionResponse(QualityInspection e) {
        return new InspectionResponse(e.getId(), e.getInspectionDate(), e.getFoodItemId(), e.getFoodItemName(),
                e.getBatchId(), e.getBatchNumber(), e.getInspector(), e.getResult(), e.getQualityStatus(),
                e.getRemarks(), e.getCreatedAt());
    }

    public static WastageResponse toWastageResponse(WastageRecord e) {
        return new WastageResponse(e.getId(), e.getBatchId(), e.getBatchNumber(), e.getFoodItemId(),
                e.getFoodItemName(), e.getUnit(), e.getQuantity(), e.getReason(), e.getDisposalMethod(),
                e.getRecordedOn(), e.getRecordedBy(), e.getRemarks());
    }

    public static ActionResponse toActionResponse(CorrectiveAction e) {
        boolean overdue = e.getStatus() != ActionStatus.CLOSED
                && e.getDueDate() != null
                && e.getDueDate().isBefore(LocalDate.now());
        return new ActionResponse(e.getId(), e.getInspectionId(), e.getWastageId(), e.getDescription(),
                e.getAssignedTo(), e.getDueDate(), e.getStatus(), e.getCreatedOn(), e.getCompletedOn(), overdue);
    }
}
