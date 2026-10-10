package com.foodsupply.reporting.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.foodsupply.reporting.client.BeneficiaryClient;
import com.foodsupply.reporting.client.DistributionClient;
import com.foodsupply.reporting.client.FoodClient;
import com.foodsupply.reporting.client.InventoryClient;
import com.foodsupply.reporting.client.ProcurementClient;
import com.foodsupply.reporting.client.ProgrammeClient;
import com.foodsupply.reporting.client.QualityClient;
import com.foodsupply.reporting.dto.DashboardResponse;
import com.foodsupply.reporting.dto.ReportResponse;
import com.foodsupply.reporting.exception.DownstreamServiceException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportServiceImplTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Mock
    private BeneficiaryClient beneficiaryClient;
    @Mock
    private FoodClient foodClient;
    @Mock
    private InventoryClient inventoryClient;
    @Mock
    private ProcurementClient procurementClient;
    @Mock
    private DistributionClient distributionClient;
    @Mock
    private ProgrammeClient programmeClient;
    @Mock
    private QualityClient qualityClient;

    @InjectMocks
    private ReportServiceImpl service;

    @Test
    void dashboardCountsPendingDistributionsFromPlans() throws Exception {
        JsonNode plans = mapper.readTree("{\"content\":["
                + "{\"status\":\"PLANNED\"},{\"status\":\"RESERVED\"},{\"status\":\"DELIVERED\"}]}");
        when(distributionClient.plans()).thenReturn(plans);

        DashboardResponse dashboard = service.dashboard();

        assertEquals(2L, dashboard.cards().get("pendingDistributions"));
        assertEquals(1L, dashboard.cards().get("completedDistributions"));
    }

    @Test
    void dashboardReportsUnavailableServiceInsteadOfFailing() {
        when(qualityClient.wastage()).thenThrow(new DownstreamServiceException("quality-service is unavailable"));

        DashboardResponse dashboard = service.dashboard();

        assertTrue(dashboard.unavailable().contains("quality-service"));
        assertNull(dashboard.cards().get("totalWastageRecords"));
    }

    @Test
    void shortageReportUsesActiveProgrammeRequirements() throws Exception {
        when(programmeClient.programmes()).thenReturn(mapper.readTree(
                "{\"content\":[{\"id\":1,\"status\":\"ACTIVE\"}]}"));
        when(programmeClient.requirements(eq(1L), any(LocalDate.class), any(LocalDate.class))).thenReturn(
                mapper.readTree("{\"shortages\":[{\"foodItemId\":5,\"foodItemName\":\"Toor Dal\","
                        + "\"unit\":\"KG\",\"requiredQuantity\":140,\"usableQuantity\":90,\"shortage\":50}]}"));

        ReportResponse report = service.shortageReport(null, null, null, null);

        assertEquals(1, report.rows().size());
        Map<String, Object> row = report.rows().get(0);
        assertEquals("PROGRAMME_SHORTAGE", row.get("type"));
        assertEquals("Toor Dal", row.get("foodItemName"));
        assertEquals(50L, row.get("shortageQuantity"));
    }

    @Test
    void expiryReportClampsDaysWindow() {
        ReportResponse report = service.expiryReport(null, 1000);

        verify(inventoryClient).expiring(365);
        assertEquals(365, report.totals().get("expiringWithinDays"));
    }
}
