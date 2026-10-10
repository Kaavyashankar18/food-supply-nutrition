package com.foodsupply.quality.service;

import com.foodsupply.quality.client.InventoryClient;
import com.foodsupply.quality.dto.BatchInfo;
import com.foodsupply.quality.dto.InspectionRequest;
import com.foodsupply.quality.dto.InspectionResponse;
import com.foodsupply.quality.entity.InspectionResult;
import com.foodsupply.quality.entity.QualityInspection;
import com.foodsupply.quality.entity.QualityStatus;
import com.foodsupply.quality.exception.DownstreamServiceException;
import com.foodsupply.quality.exception.InvalidRequestException;
import com.foodsupply.quality.repository.QualityInspectionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InspectionServiceImplTest {

    @Mock
    private QualityInspectionRepository repository;

    @Mock
    private InventoryClient inventoryClient;

    @InjectMocks
    private InspectionServiceImpl service;

    private InspectionRequest request(InspectionResult result, QualityStatus status) {
        return InspectionRequest.builder()
                .batchId(5L)
                .inspector("Meena Krishnan")
                .result(result)
                .qualityStatus(status)
                .remarks("Off smell")
                .build();
    }

    @Test
    void failedInspectionWithoutStatusRejectsBatchAndSavesInspection() {
        when(inventoryClient.updateQualityStatus(eq(5L), eq(QualityStatus.REJECTED), anyString()))
                .thenReturn(new BatchInfo(5L, 9L, "Soya Chunks", "SOYA-1", "REJECTED"));
        when(repository.save(any(QualityInspection.class))).thenAnswer(inv -> {
            QualityInspection saved = inv.getArgument(0);
            saved.setId(1L);
            return saved;
        });

        InspectionResponse response = service.create(request(InspectionResult.FAIL, null));

        assertEquals(1L, response.id());
        assertEquals(QualityStatus.REJECTED, response.qualityStatus());
        assertEquals("Soya Chunks", response.foodItemName());
        assertEquals("SOYA-1", response.batchNumber());
        assertEquals(9L, response.foodItemId());
    }

    @Test
    void inventoryFailureMeansNothingIsSaved() {
        when(inventoryClient.updateQualityStatus(eq(5L), eq(QualityStatus.APPROVED), anyString()))
                .thenThrow(new DownstreamServiceException("inventory-service is unavailable"));

        assertThrows(DownstreamServiceException.class,
                () -> service.create(request(InspectionResult.PASS, null)));

        verify(repository, never()).save(any(QualityInspection.class));
    }

    @Test
    void failedInspectionCannotApproveBatch() {
        assertThrows(InvalidRequestException.class,
                () -> service.create(request(InspectionResult.FAIL, QualityStatus.APPROVED)));

        verify(repository, never()).save(any(QualityInspection.class));
    }
}
