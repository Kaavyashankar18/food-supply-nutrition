package com.foodsupply.procurement.service;

import com.foodsupply.procurement.client.FoodNutritionClient;
import com.foodsupply.procurement.dto.FoodItemInfo;
import com.foodsupply.procurement.dto.ProcurementRequestInput;
import com.foodsupply.procurement.dto.RequestItemInput;
import com.foodsupply.procurement.dto.RequestResponse;
import com.foodsupply.procurement.entity.ProcurementRequest;
import com.foodsupply.procurement.entity.RequestStatus;
import com.foodsupply.procurement.exception.BusinessException;
import com.foodsupply.procurement.repository.ProcurementRequestRepository;
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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProcurementRequestServiceImplTest {

    @Mock
    private ProcurementRequestRepository requestRepository;

    @Mock
    private FoodNutritionClient foodNutritionClient;

    @InjectMocks
    private ProcurementRequestServiceImpl service;

    @Test
    void create_resolvesNameAndUnitFromFoodService_andComputesCost() {
        when(requestRepository.findTopByRequestNumberStartingWithOrderByRequestNumberDesc(anyString()))
                .thenReturn(Optional.empty());
        when(foodNutritionClient.getByIds(anyList()))
                .thenReturn(List.of(new FoodItemInfo(5L, "Toor Dal", "KG", true)));
        when(requestRepository.save(any(ProcurementRequest.class))).thenAnswer(inv -> inv.getArgument(0));

        RequestItemInput item = RequestItemInput.builder()
                .foodItemId(5L)
                .quantity(new BigDecimal("50"))
                .estimatedUnitPrice(new BigDecimal("100.00"))
                .build();
        ProcurementRequestInput input = ProcurementRequestInput.builder()
                .requestedBy("store.manager")
                .reason("Shortage")
                .items(List.of(item))
                .build();

        RequestResponse response = service.create(input);

        assertTrue(response.requestNumber().endsWith("-0001"));
        assertEquals(RequestStatus.DRAFT, response.status());
        assertEquals("Toor Dal", response.items().get(0).foodItemName());
        assertEquals(0, new BigDecimal("5000.00").compareTo(response.estimatedCost()));
    }

    @Test
    void submit_failsWhenRequestIsNotDraft() {
        ProcurementRequest request = ProcurementRequest.builder()
                .id(1L).requestNumber("PR-2026-0001").status(RequestStatus.APPROVED)
                .requestedBy("x").createdOn(LocalDate.now()).build();
        when(requestRepository.findById(1L)).thenReturn(Optional.of(request));

        assertThrows(BusinessException.class, () -> service.submit(1L));
    }

    @Test
    void approve_movesSubmittedToApproved() {
        ProcurementRequest request = ProcurementRequest.builder()
                .id(2L).requestNumber("PR-2026-0002").status(RequestStatus.SUBMITTED)
                .requestedBy("x").createdOn(LocalDate.now()).build();
        when(requestRepository.findById(2L)).thenReturn(Optional.of(request));

        RequestResponse response = service.approve(2L);

        assertEquals(RequestStatus.APPROVED, response.status());
    }
}
