package com.foodsupply.procurement.service;

import com.foodsupply.procurement.client.InventoryClient;
import com.foodsupply.procurement.dto.BatchInfo;
import com.foodsupply.procurement.dto.PurchaseOrderResponse;
import com.foodsupply.procurement.dto.ReceiveInput;
import com.foodsupply.procurement.dto.ReceiveLineInput;
import com.foodsupply.procurement.dto.ReceiveStockRequest;
import com.foodsupply.procurement.entity.FoodUnit;
import com.foodsupply.procurement.entity.ProcurementRequest;
import com.foodsupply.procurement.entity.PurchaseOrder;
import com.foodsupply.procurement.entity.PurchaseOrderItem;
import com.foodsupply.procurement.entity.PurchaseOrderStatus;
import com.foodsupply.procurement.entity.RequestStatus;
import com.foodsupply.procurement.exception.BusinessException;
import com.foodsupply.procurement.repository.ProcurementRequestRepository;
import com.foodsupply.procurement.repository.PurchaseOrderRepository;
import com.foodsupply.procurement.repository.SupplierRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PurchaseOrderServiceImplTest {

    @Mock
    private PurchaseOrderRepository orderRepository;

    @Mock
    private ProcurementRequestRepository requestRepository;

    @Mock
    private SupplierRepository supplierRepository;

    @Mock
    private InventoryClient inventoryClient;

    @InjectMocks
    private PurchaseOrderServiceImpl service;

    private PurchaseOrder newOrder(String... quantities) {
        PurchaseOrder po = PurchaseOrder.builder()
                .id(1L).poNumber("PO-2026-0001").supplierId(3L).supplierName("Supplier")
                .requestId(7L).requestNumber("PR-2026-0001")
                .orderDate(LocalDate.now()).expectedDeliveryDate(LocalDate.now().plusDays(3))
                .status(PurchaseOrderStatus.CREATED).build();
        long itemId = 10L;
        for (String qty : quantities) {
            po.addItem(PurchaseOrderItem.builder()
                    .id(itemId).foodItemId(itemId).foodItemName("Item " + itemId).unit(FoodUnit.KG)
                    .orderedQty(new BigDecimal(qty)).receivedQty(BigDecimal.ZERO)
                    .unitPrice(new BigDecimal("40.00")).build());
            itemId++;
        }
        return po;
    }

    private ReceiveLineInput line(long itemId, String qty, String batch) {
        return ReceiveLineInput.builder().itemId(itemId).quantityReceived(new BigDecimal(qty))
                .batchNumber(batch).expiryDate(LocalDate.now().plusDays(90)).storageLocation("Warehouse A").build();
    }

    private BatchInfo batch() {
        return new BatchInfo(1L, 10L, "B1", BigDecimal.TEN, "APPROVED");
    }

    @Test
    void receive_fullQuantity_marksOrderReceivedAndCompletesRequest() {
        PurchaseOrder po = newOrder("100");
        ProcurementRequest request = ProcurementRequest.builder()
                .id(7L).requestNumber("PR-2026-0001").status(RequestStatus.ORDERED)
                .requestedBy("x").createdOn(LocalDate.now()).build();
        when(orderRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(po));
        when(inventoryClient.receiveStock(any(ReceiveStockRequest.class))).thenReturn(batch());
        when(requestRepository.findById(7L)).thenReturn(Optional.of(request));

        ReceiveInput input = ReceiveInput.builder().items(List.of(line(10L, "100", "B-100"))).build();
        PurchaseOrderResponse response = service.receive(1L, input);

        assertEquals(PurchaseOrderStatus.RECEIVED, response.status());
        assertEquals(RequestStatus.COMPLETED, request.getStatus());
        verify(inventoryClient, times(1)).receiveStock(any(ReceiveStockRequest.class));
    }

    @Test
    void receive_overReceipt_isRejectedBeforeCallingInventory() {
        PurchaseOrder po = newOrder("100");
        when(orderRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(po));

        ReceiveInput input = ReceiveInput.builder().items(List.of(line(10L, "150", "B-150"))).build();

        assertThrows(BusinessException.class, () -> service.receive(1L, input));
        verifyNoInteractions(inventoryClient);
    }

    @Test
    void receive_failureOnSecondLine_keepsFirstLineAndMarksPartiallyReceived() {
        PurchaseOrder po = newOrder("100", "50");
        when(orderRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(po));
        when(inventoryClient.receiveStock(any(ReceiveStockRequest.class)))
                .thenReturn(batch())
                .thenThrow(new BusinessException("Batch number already exists"));

        ReceiveInput input = ReceiveInput.builder()
                .items(List.of(line(10L, "100", "B-A"), line(11L, "50", "B-B"))).build();

        assertThrows(BusinessException.class, () -> service.receive(1L, input));
        assertEquals(PurchaseOrderStatus.PARTIALLY_RECEIVED, po.getStatus());
        assertEquals(0, new BigDecimal("100").compareTo(po.getItems().get(0).getReceivedQty()));
        assertEquals(0, BigDecimal.ZERO.compareTo(po.getItems().get(1).getReceivedQty()));
    }
}
