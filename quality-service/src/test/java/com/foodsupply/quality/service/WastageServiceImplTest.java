package com.foodsupply.quality.service;

import com.foodsupply.quality.client.InventoryClient;
import com.foodsupply.quality.dto.BatchInfo;
import com.foodsupply.quality.dto.WastageRequest;
import com.foodsupply.quality.dto.WastageResponse;
import com.foodsupply.quality.entity.FoodUnit;
import com.foodsupply.quality.entity.WastageReason;
import com.foodsupply.quality.entity.WastageRecord;
import com.foodsupply.quality.exception.BusinessException;
import com.foodsupply.quality.repository.WastageRecordRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WastageServiceImplTest {

    @Mock
    private WastageRecordRepository repository;

    @Mock
    private InventoryClient inventoryClient;

    @InjectMocks
    private WastageServiceImpl service;

    private WastageRequest request() {
        return WastageRequest.builder()
                .batchId(7L)
                .quantity(new BigDecimal("12.500"))
                .reason(WastageReason.SPOILED)
                .recordedBy("Store Manager")
                .unit(FoodUnit.KG)
                .build();
    }

    @Test
    void createWritesOffStockThenSavesRecord() {
        BatchInfo batch = new BatchInfo(7L, 12L, "Tomato", "TOMA-1", "APPROVED");
        when(inventoryClient.getBatch(7L)).thenReturn(batch);
        when(inventoryClient.writeOff(eq(7L), eq(new BigDecimal("12.500")), eq("SPOILED"), eq(null)))
                .thenReturn(batch);
        when(repository.save(any(WastageRecord.class))).thenAnswer(inv -> {
            WastageRecord saved = inv.getArgument(0);
            saved.setId(3L);
            return saved;
        });

        WastageResponse response = service.create(request());

        assertEquals(3L, response.id());
        assertEquals("Tomato", response.foodItemName());
        assertEquals(FoodUnit.KG, response.unit());
        verify(inventoryClient).writeOff(7L, new BigDecimal("12.500"), "SPOILED", null);
    }

    @Test
    void rejectedWriteOffMeansNoRecordIsSaved() {
        BatchInfo batch = new BatchInfo(7L, 12L, "Tomato", "TOMA-1", "APPROVED");
        when(inventoryClient.getBatch(7L)).thenReturn(batch);
        when(inventoryClient.writeOff(eq(7L), any(BigDecimal.class), eq("SPOILED"), eq(null)))
                .thenThrow(new BusinessException("Quantity exceeds the unreserved on-hand stock"));

        assertThrows(BusinessException.class, () -> service.create(request()));

        verify(repository, never()).save(any(WastageRecord.class));
    }
}
