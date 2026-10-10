package com.foodsupply.procurement.mapper;

import com.foodsupply.procurement.dto.PurchaseOrderItemResponse;
import com.foodsupply.procurement.dto.PurchaseOrderResponse;
import com.foodsupply.procurement.dto.RequestItemResponse;
import com.foodsupply.procurement.dto.RequestResponse;
import com.foodsupply.procurement.entity.ProcurementRequest;
import com.foodsupply.procurement.entity.PurchaseOrder;
import com.foodsupply.procurement.entity.PurchaseOrderItem;
import com.foodsupply.procurement.entity.RequestItem;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/** Must be called inside a transaction (items are lazy). */
public final class ProcurementMapper {

    private ProcurementMapper() {
    }

    public static RequestResponse toRequestResponse(ProcurementRequest r) {
        List<RequestItemResponse> items = new ArrayList<>();
        for (RequestItem i : r.getItems()) {
            BigDecimal cost = i.getEstimatedUnitPrice() == null ? null
                    : i.getQuantity().multiply(i.getEstimatedUnitPrice()).setScale(2, RoundingMode.HALF_UP);
            items.add(new RequestItemResponse(i.getId(), i.getFoodItemId(), i.getFoodItemName(), i.getUnit(),
                    i.getQuantity(), i.getEstimatedUnitPrice(), cost));
        }
        return new RequestResponse(r.getId(), r.getRequestNumber(), r.getStatus(), r.getSource(), r.getReason(),
                r.getEstimatedCost(), r.getRequestedBy(), r.getCreatedOn(), r.getNeedByDate(),
                r.getDecisionRemarks(), items);
    }

    public static PurchaseOrderResponse toOrderResponse(PurchaseOrder po) {
        List<PurchaseOrderItemResponse> items = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;
        for (PurchaseOrderItem i : po.getItems()) {
            BigDecimal pending = i.getOrderedQty().subtract(i.getReceivedQty()).max(BigDecimal.ZERO);
            BigDecimal line = i.getOrderedQty().multiply(i.getUnitPrice()).setScale(2, RoundingMode.HALF_UP);
            total = total.add(line);
            items.add(new PurchaseOrderItemResponse(i.getId(), i.getFoodItemId(), i.getFoodItemName(), i.getUnit(),
                    i.getOrderedQty(), i.getReceivedQty(), pending, i.getUnitPrice(), line));
        }
        return new PurchaseOrderResponse(po.getId(), po.getPoNumber(), po.getSupplierId(), po.getSupplierName(),
                po.getRequestId(), po.getRequestNumber(), po.getOrderDate(), po.getExpectedDeliveryDate(),
                po.getStatus(), total.setScale(2, RoundingMode.HALF_UP), items);
    }
}
