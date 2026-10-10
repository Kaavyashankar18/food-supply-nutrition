package com.foodsupply.distribution.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.foodsupply.distribution.client.InventoryClient;
import com.foodsupply.distribution.client.ProcurementClient;
import com.foodsupply.distribution.client.ProgrammeClient;
import com.foodsupply.distribution.dto.DeliveredItem;
import com.foodsupply.distribution.dto.DeliveryRequest;
import com.foodsupply.distribution.dto.PlanRequest;
import com.foodsupply.distribution.dto.PlanResponse;
import com.foodsupply.distribution.entity.CentreStatus;
import com.foodsupply.distribution.entity.DistributionCentre;
import com.foodsupply.distribution.entity.DistributionItem;
import com.foodsupply.distribution.entity.DistributionPlan;
import com.foodsupply.distribution.entity.FoodUnit;
import com.foodsupply.distribution.entity.PlanBeneficiary;
import com.foodsupply.distribution.entity.PlanStatus;
import com.foodsupply.distribution.exception.BusinessException;
import com.foodsupply.distribution.exception.ShortageException;
import com.foodsupply.distribution.repository.CentreRepository;
import com.foodsupply.distribution.repository.PlanRepository;
import com.foodsupply.distribution.repository.RecordRepository;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlanServiceImplTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String REQUIREMENTS = "{\"programmeName\":\"School\",\"enrolledPeople\":500,\"items\":["
            + "{\"foodItemId\":1,\"foodItemName\":\"Rice (Raw)\",\"unit\":\"KG\",\"totalQuantity\":50.0}]}";
    private static final String PARTICIPANTS = "[{\"beneficiaryId\":1,\"beneficiaryName\":\"School A\","
            + "\"peopleCovered\":500,\"status\":\"ENROLLED\"}]";

    @Mock
    private PlanRepository planRepository;
    @Mock
    private CentreRepository centreRepository;
    @Mock
    private RecordRepository recordRepository;
    @Mock
    private ProgrammeClient programmeClient;
    @Mock
    private InventoryClient inventoryClient;
    @Mock
    private ProcurementClient procurementClient;
    @InjectMocks
    private PlanServiceImpl service;

    private DistributionCentre centre() {
        return DistributionCentre.builder().id(1L).name("Adyar Hub").status(CentreStatus.ACTIVE).build();
    }

    private PlanRequest request() {
        return PlanRequest.builder().programmeId(1L).centreId(1L).startDate(LocalDate.now())
                .endDate(LocalDate.now().plusDays(6)).build();
    }

    private DistributionPlan plan(PlanStatus status, String dispatched) {
        DistributionPlan p = DistributionPlan.builder().id(5L).planNumber("DP-2026-0005").programmeId(1L)
                .programmeName("School").centre(centre()).startDate(LocalDate.now()).endDate(LocalDate.now().plusDays(6))
                .status(status).createdOn(LocalDate.now()).enrolledPeople(500).build();
        p.getItems().add(DistributionItem.builder().plan(p).foodItemId(1L).foodItemName("Rice (Raw)").unit(FoodUnit.KG)
                .plannedQty(new BigDecimal("100")).dispatchedQty(new BigDecimal(dispatched)).build());
        p.getBeneficiaries().add(PlanBeneficiary.builder().plan(p).beneficiaryId(1L).beneficiaryName("School A")
                .peopleCovered(500).build());
        return p;
    }

    @Test
    void create_reservesStockAndMarksPlanReserved() throws Exception {
        when(centreRepository.findById(1L)).thenReturn(Optional.of(centre()));
        when(programmeClient.requirements(any(), any(), any())).thenReturn(MAPPER.readTree(REQUIREMENTS));
        when(programmeClient.participants(1L)).thenReturn(MAPPER.readTree(PARTICIPANTS));
        when(planRepository.saveAndFlush(any(DistributionPlan.class))).thenAnswer(inv -> {
            DistributionPlan p = inv.getArgument(0);
            if (p.getId() == null) {
                p.setId(10L);
            }
            return p;
        });
        when(inventoryClient.reserve(any())).thenReturn(MAPPER.readTree(
                "{\"allocations\":[{\"foodItemId\":1,\"batchId\":7,\"batchNumber\":\"B1\",\"expiryDate\":\"2026-12-01\",\"quantity\":50}]}"));

        PlanResponse r = service.create(request());

        assertEquals(PlanStatus.RESERVED, r.status());
        assertTrue(r.planNumber().startsWith("DP-"));
        assertEquals(1, r.items().get(0).allocations().size());
    }

    @Test
    void create_propagatesShortageFromInventory() throws Exception {
        when(centreRepository.findById(1L)).thenReturn(Optional.of(centre()));
        when(programmeClient.requirements(any(), any(), any())).thenReturn(MAPPER.readTree(REQUIREMENTS));
        when(programmeClient.participants(1L)).thenReturn(MAPPER.readTree(PARTICIPANTS));
        when(planRepository.saveAndFlush(any(DistributionPlan.class))).thenAnswer(inv -> {
            DistributionPlan p = inv.getArgument(0);
            p.setId(11L);
            return p;
        });
        when(inventoryClient.reserve(any())).thenThrow(new ShortageException("Insufficient stock", MAPPER.readTree("[]")));

        assertThrows(ShortageException.class, () -> service.create(request()));
    }

    @Test
    void dispatch_rejectsPlanThatIsNotReserved() {
        when(planRepository.findById(5L)).thenReturn(Optional.of(plan(PlanStatus.PLANNED, "0")));

        assertThrows(BusinessException.class, () -> service.dispatch(5L));
    }

    @Test
    void deliver_partialQuantityGivesPartialStatus() {
        when(planRepository.findById(5L)).thenReturn(Optional.of(plan(PlanStatus.DISPATCHED, "100")));

        DeliveryRequest req = new DeliveryRequest(List.of(new DeliveredItem(1L, new BigDecimal("60"))), "short", null);
        PlanResponse r = service.deliver(5L, req);

        assertEquals(PlanStatus.PARTIAL, r.status());
        assertEquals(0, new BigDecimal("60").compareTo(r.items().get(0).deliveredQty()));
    }

    @Test
    void deliver_rejectsQuantityAboveDispatched() {
        when(planRepository.findById(5L)).thenReturn(Optional.of(plan(PlanStatus.DISPATCHED, "100")));

        DeliveryRequest req = new DeliveryRequest(List.of(new DeliveredItem(1L, new BigDecimal("150"))), null, null);

        assertThrows(BusinessException.class, () -> service.deliver(5L, req));
    }
}
