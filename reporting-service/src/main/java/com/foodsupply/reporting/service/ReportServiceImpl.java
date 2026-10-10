package com.foodsupply.reporting.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.foodsupply.reporting.client.BaseClient;
import com.foodsupply.reporting.client.BeneficiaryClient;
import com.foodsupply.reporting.client.DistributionClient;
import com.foodsupply.reporting.client.FoodClient;
import com.foodsupply.reporting.client.InventoryClient;
import com.foodsupply.reporting.client.ProcurementClient;
import com.foodsupply.reporting.client.ProgrammeClient;
import com.foodsupply.reporting.client.QualityClient;
import com.foodsupply.reporting.dto.DashboardResponse;
import com.foodsupply.reporting.dto.ReportResponse;
import com.foodsupply.reporting.dto.ServiceStatusResponse;
import com.foodsupply.reporting.exception.DownstreamServiceException;
import com.foodsupply.reporting.util.DownstreamTracker;
import com.foodsupply.reporting.util.JsonUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReportServiceImpl implements ReportService {

    private static final String BENEFICIARY = "beneficiary-service";
    private static final String FOOD = "food-nutrition-service";
    private static final String INVENTORY = "inventory-service";
    private static final String PROCUREMENT = "procurement-service";
    private static final String DISTRIBUTION = "distribution-service";
    private static final String PROGRAMME = "programme-service";
    private static final String QUALITY = "quality-service";

    private static final List<String> BENEFICIARY_COLUMNS = List.of(
            "id", "fullName", "type", "category", "peopleServed", "status", "eligible", "registeredOn");
    private static final List<String> STOCK_COLUMNS = List.of(
            "foodItemId", "foodItemName", "unit", "storageLocation", "minimumStockLevel", "onHand", "reserved",
            "usable", "expiringSoon", "expired", "quarantined", "rejected", "lowStock");
    private static final List<String> REQUEST_COLUMNS = List.of(
            "id", "requestNumber", "status", "source", "reason", "requestedBy", "createdOn", "needByDate",
            "estimatedCost");
    private static final List<String> PLAN_COLUMNS = List.of(
            "id", "planNumber", "programmeId", "programmeName", "centreId", "centreName", "status", "startDate",
            "endDate", "remarks");
    private static final List<String> PROGRAMME_COLUMNS = List.of(
            "id", "name", "type", "targetCategory", "status", "startDate", "endDate");
    private static final List<String> SHORTAGE_COLUMNS = List.of(
            "type", "programmeId", "foodItemId", "foodItemName", "unit", "requiredQuantity", "usableQuantity",
            "shortageQuantity");
    private static final List<String> BATCH_COLUMNS = List.of(
            "id", "batchNumber", "foodItemId", "foodItemName", "quantityAvailable", "usableQuantity", "expiryDate",
            "daysToExpiry", "qualityStatus", "storageLocation");
    private static final List<String> WASTAGE_COLUMNS = List.of(
            "id", "recordedOn", "batchId", "batchNumber", "foodItemId", "foodItemName", "unit", "quantity",
            "reason", "disposalMethod", "recordedBy");

    private final BeneficiaryClient beneficiaryClient;
    private final FoodClient foodClient;
    private final InventoryClient inventoryClient;
    private final ProcurementClient procurementClient;
    private final DistributionClient distributionClient;
    private final ProgrammeClient programmeClient;
    private final QualityClient qualityClient;

    // ------------------------------------------------------------------ dashboard

    @Override
    public DashboardResponse dashboard() {
        DownstreamTracker t = new DownstreamTracker();

        JsonNode bStats = t.node(BENEFICIARY, beneficiaryClient::stats);
        JsonNode bList = t.node(BENEFICIARY, beneficiaryClient::list);
        JsonNode foodStats = t.node(FOOD, foodClient::stats);
        JsonNode invSummary = t.node(INVENTORY, inventoryClient::summary);
        JsonNode requests = t.node(PROCUREMENT, procurementClient::requests);
        JsonNode orders = t.node(PROCUREMENT, procurementClient::purchaseOrders);
        JsonNode plans = t.node(DISTRIBUTION, distributionClient::plans);
        JsonNode programmes = t.node(PROGRAMME, programmeClient::programmes);
        JsonNode wastage = t.node(QUALITY, qualityClient::wastage);
        JsonNode inspections = t.node(QUALITY, qualityClient::inspections);
        JsonNode actions = t.node(QUALITY, qualityClient::correctiveActions);

        Map<String, Object> cards = new LinkedHashMap<>();
        cards.put("totalBeneficiaries", JsonUtil.value(bStats, "total"));
        cards.put("activeBeneficiaries", JsonUtil.value(bStats, "active"));
        cards.put("eligibleActiveBeneficiaries", JsonUtil.value(bStats, "eligibleActive"));
        cards.put("peopleServed", JsonUtil.value(bStats, "peopleServed"));
        cards.put("foodItems", JsonUtil.value(foodStats, "totalItems"));

        JsonNode byUnit = invSummary.path("usableByUnit");
        cards.put("stockItems", JsonUtil.value(invSummary, "totalItems"));
        cards.put("stockBatches", JsonUtil.value(invSummary, "totalBatches"));
        cards.put("usableStockKg", JsonUtil.value(byUnit, "KG"));
        cards.put("usableStockLitre", JsonUtil.value(byUnit, "LITRE"));
        cards.put("usableStockPiece", JsonUtil.value(byUnit, "PIECE"));
        cards.put("lowStockItems", JsonUtil.value(invSummary, "lowStockItems"));
        cards.put("outOfStockItems", JsonUtil.value(invSummary, "outOfStockItems"));
        cards.put("expiringBatches", JsonUtil.value(invSummary, "expiringBatches"));
        cards.put("expiredBatches", JsonUtil.value(invSummary, "expiredBatches"));
        cards.put("quarantinedBatches", JsonUtil.value(invSummary, "quarantinedBatches"));
        cards.put("activeReservations", JsonUtil.value(invSummary, "activeReservations"));

        cards.put("pendingProcurementRequests", countWhere(requests, "status", "DRAFT", "SUBMITTED"));
        cards.put("openPurchaseOrders", countWhere(orders, "status", "CREATED", "SENT", "PARTIALLY_RECEIVED"));
        cards.put("procurementEstimatedCost", sumField(requests, "estimatedCost"));

        cards.put("pendingDistributions", countWhere(plans, "status", "PLANNED", "RESERVED", "DISPATCHED"));
        cards.put("completedDistributions", countWhere(plans, "status", "DELIVERED", "PARTIAL"));

        cards.put("activeProgrammes", countWhere(programmes, "status", "ACTIVE"));

        cards.put("totalWastageRecords", wastage.isMissingNode() ? null : (Object) (long) JsonUtil.items(wastage).size());
        cards.put("wastageKg", unitTotal(wastage, "KG"));
        cards.put("wastageLitre", unitTotal(wastage, "LITRE"));
        cards.put("wastagePiece", unitTotal(wastage, "PIECE"));
        cards.put("failedInspections", countWhere(inspections, "result", "FAIL"));
        cards.put("openCorrectiveActions", countWhere(actions, "status", "OPEN", "IN_PROGRESS"));

        Map<String, Map<String, Object>> charts = new LinkedHashMap<>();
        charts.put("peopleByCategory",
                new LinkedHashMap<String, Object>(JsonUtil.sumBy(JsonUtil.items(bList), "category", "peopleServed")));
        charts.put("itemsPerCategory", JsonUtil.objectToMap(foodStats.path("itemsPerCategory")));
        Map<String, Object> stockStatus = new LinkedHashMap<>();
        stockStatus.put("Low stock", JsonUtil.value(invSummary, "lowStockItems"));
        stockStatus.put("Out of stock", JsonUtil.value(invSummary, "outOfStockItems"));
        stockStatus.put("Expiring", JsonUtil.value(invSummary, "expiringBatches"));
        stockStatus.put("Expired", JsonUtil.value(invSummary, "expiredBatches"));
        stockStatus.put("Quarantined", JsonUtil.value(invSummary, "quarantinedBatches"));
        charts.put("stockAlerts", stockStatus);
        charts.put("procurementByStatus",
                new LinkedHashMap<String, Object>(JsonUtil.countBy(JsonUtil.items(requests), "status")));
        charts.put("distributionByStatus",
                new LinkedHashMap<String, Object>(JsonUtil.countBy(JsonUtil.items(plans), "status")));
        charts.put("wastageByReason",
                new LinkedHashMap<String, Object>(JsonUtil.countBy(JsonUtil.items(wastage), "reason")));
        charts.put("wastageByMonth",
                new LinkedHashMap<String, Object>(JsonUtil.countByMonth(JsonUtil.items(wastage), "recordedOn")));
        charts.put("inspectionsByResult",
                new LinkedHashMap<String, Object>(JsonUtil.countBy(JsonUtil.items(inspections), "result")));

        return new DashboardResponse(LocalDateTime.now(), cards, charts, t.unavailable());
    }

    // ------------------------------------------------------------------ reports

    @Override
    public ReportResponse beneficiaryReport(LocalDate from, LocalDate to) {
        DownstreamTracker t = new DownstreamTracker();
        JsonNode list = t.node(BENEFICIARY, beneficiaryClient::list);

        List<JsonNode> filtered = new ArrayList<>();
        for (JsonNode b : JsonUtil.items(list)) {
            if (JsonUtil.inRange(JsonUtil.date(b, "registeredOn"), from, to)) {
                filtered.add(b);
            }
        }
        long people = 0;
        for (JsonNode b : filtered) {
            people += b.path("peopleServed").asLong(0);
        }
        Map<String, Object> totals = new LinkedHashMap<>();
        totals.put("records", filtered.size());
        totals.put("peopleServed", people);
        totals.put("peopleByCategory", JsonUtil.sumBy(filtered, "category", "peopleServed"));
        totals.put("recordsByStatus", JsonUtil.countBy(filtered, "status"));

        return report("beneficiary", "Beneficiary report", JsonUtil.pairs("from", from, "to", to),
                BENEFICIARY_COLUMNS, rows(filtered, BENEFICIARY_COLUMNS), totals, t);
    }

    @Override
    public ReportResponse inventoryReport(Long foodItemId) {
        DownstreamTracker t = new DownstreamTracker();
        JsonNode stock = t.node(INVENTORY, inventoryClient::stock);

        List<JsonNode> filtered = new ArrayList<>();
        for (JsonNode s : JsonUtil.items(stock)) {
            if (matchesId(s, "foodItemId", foodItemId)) {
                filtered.add(s);
            }
        }
        long lowStock = 0;
        for (JsonNode s : filtered) {
            if (s.path("lowStock").asBoolean(false)) {
                lowStock++;
            }
        }
        Map<String, Object> totals = new LinkedHashMap<>();
        totals.put("items", filtered.size());
        totals.put("lowStockItems", lowStock);
        totals.put("usableByUnit", JsonUtil.sumBy(filtered, "unit", "usable"));
        totals.put("onHandByUnit", JsonUtil.sumBy(filtered, "unit", "onHand"));

        return report("inventory", "Inventory stock report", JsonUtil.pairs("foodItemId", foodItemId),
                STOCK_COLUMNS, rows(filtered, STOCK_COLUMNS), totals, t);
    }

    @Override
    public ReportResponse procurementReport(LocalDate from, LocalDate to, Long foodItemId) {
        DownstreamTracker t = new DownstreamTracker();
        JsonNode requests = t.node(PROCUREMENT, procurementClient::requests);
        JsonNode orders = t.node(PROCUREMENT, procurementClient::purchaseOrders);

        List<JsonNode> filtered = new ArrayList<>();
        BigDecimal cost = BigDecimal.ZERO;
        for (JsonNode r : JsonUtil.items(requests)) {
            if (JsonUtil.inRange(JsonUtil.date(r, "createdOn"), from, to) && hasFoodItem(r, foodItemId)) {
                filtered.add(r);
                cost = cost.add(JsonUtil.decimal(r, "estimatedCost"));
            }
        }
        Map<String, Object> totals = new LinkedHashMap<>();
        totals.put("requests", filtered.size());
        totals.put("totalEstimatedCost", cost);
        totals.put("requestsByStatus", JsonUtil.countBy(filtered, "status"));
        totals.put("requestsBySource", JsonUtil.countBy(filtered, "source"));
        totals.put("purchaseOrdersByStatus", JsonUtil.countBy(JsonUtil.items(orders), "status"));

        return report("procurement", "Procurement report",
                JsonUtil.pairs("from", from, "to", to, "foodItemId", foodItemId),
                REQUEST_COLUMNS, rows(filtered, REQUEST_COLUMNS), totals, t);
    }

    @Override
    public ReportResponse distributionReport(LocalDate from, LocalDate to, Long programmeId) {
        DownstreamTracker t = new DownstreamTracker();
        JsonNode plans = t.node(DISTRIBUTION, distributionClient::plans);

        List<JsonNode> filtered = new ArrayList<>();
        for (JsonNode p : JsonUtil.items(plans)) {
            if (matchesId(p, "programmeId", programmeId)
                    && JsonUtil.inRange(JsonUtil.date(p, "startDate"), from, to)) {
                filtered.add(p);
            }
        }
        Map<String, Object> totals = new LinkedHashMap<>();
        totals.put("plans", filtered.size());
        totals.put("plansByStatus", JsonUtil.countBy(filtered, "status"));

        return report("distribution", "Distribution report",
                JsonUtil.pairs("from", from, "to", to, "programmeId", programmeId),
                PLAN_COLUMNS, rows(filtered, PLAN_COLUMNS), totals, t);
    }

    @Override
    public ReportResponse programmeReport(LocalDate from, LocalDate to, Long programmeId) {
        DownstreamTracker t = new DownstreamTracker();
        JsonNode programmes = t.node(PROGRAMME, programmeClient::programmes);

        List<JsonNode> filtered = new ArrayList<>();
        for (JsonNode p : JsonUtil.items(programmes)) {
            if (matchesId(p, "id", programmeId) && overlaps(p, from, to)) {
                filtered.add(p);
            }
        }
        Map<String, Object> totals = new LinkedHashMap<>();
        totals.put("programmes", filtered.size());
        totals.put("programmesByStatus", JsonUtil.countBy(filtered, "status"));
        totals.put("programmesByTargetCategory", JsonUtil.countBy(filtered, "targetCategory"));

        return report("programme", "Nutrition programme report",
                JsonUtil.pairs("from", from, "to", to, "programmeId", programmeId),
                PROGRAMME_COLUMNS, rows(filtered, PROGRAMME_COLUMNS), totals, t);
    }

    @Override
    public ReportResponse shortageReport(LocalDate from, LocalDate to, Long programmeId, Long foodItemId) {
        DownstreamTracker t = new DownstreamTracker();
        LocalDate start = from != null ? from : LocalDate.now();
        LocalDate end = to != null ? to : start.plusDays(6);

        Long resolved = programmeId;
        if (resolved == null) {
            JsonNode programmes = t.node(PROGRAMME, programmeClient::programmes);
            for (JsonNode p : JsonUtil.items(programmes)) {
                if ("ACTIVE".equals(JsonUtil.text(p, "status"))) {
                    resolved = p.path("id").asLong();
                    break;
                }
            }
        }
        final Long programmeToUse = resolved;

        List<Map<String, Object>> rows = new ArrayList<>();
        long programmeShortages = 0;
        if (programmeToUse != null) {
            JsonNode requirements = t.node(PROGRAMME, () -> programmeClient.requirements(programmeToUse, start, end));
            JsonNode shortages = requirements.findValue("shortages");
            if (shortages != null && shortages.isArray()) {
                for (JsonNode s : shortages) {
                    if (!matchesId(s, "foodItemId", foodItemId)) {
                        continue;
                    }
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("type", "PROGRAMME_SHORTAGE");
                    row.put("programmeId", programmeToUse);
                    row.put("foodItemId", JsonUtil.value(s, "foodItemId"));
                    row.put("foodItemName", JsonUtil.value(s, "foodItemName"));
                    row.put("unit", JsonUtil.value(s, "unit"));
                    row.put("requiredQuantity", JsonUtil.value(s, "requiredQuantity"));
                    row.put("usableQuantity", JsonUtil.value(s, "usableQuantity"));
                    row.put("shortageQuantity", JsonUtil.value(s, "shortage"));
                    rows.add(row);
                    programmeShortages++;
                }
            }
        }

        long lowStock = 0;
        JsonNode low = t.node(INVENTORY, inventoryClient::lowStock);
        for (JsonNode s : JsonUtil.items(low)) {
            if (!matchesId(s, "foodItemId", foodItemId)) {
                continue;
            }
            BigDecimal minimum = JsonUtil.decimal(s, "minimumStockLevel");
            BigDecimal usable = JsonUtil.decimal(s, "usable");
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("type", "LOW_STOCK");
            row.put("programmeId", null);
            row.put("foodItemId", JsonUtil.value(s, "foodItemId"));
            row.put("foodItemName", JsonUtil.value(s, "foodItemName"));
            row.put("unit", JsonUtil.value(s, "unit"));
            row.put("requiredQuantity", minimum);
            row.put("usableQuantity", usable);
            row.put("shortageQuantity", minimum.subtract(usable).max(BigDecimal.ZERO));
            rows.add(row);
            lowStock++;
        }

        Map<String, Object> totals = new LinkedHashMap<>();
        totals.put("programmeId", programmeToUse);
        totals.put("rangeFrom", start);
        totals.put("rangeTo", end);
        totals.put("programmeShortageItems", programmeShortages);
        totals.put("lowStockItems", lowStock);

        return report("shortage", "Shortage report",
                JsonUtil.pairs("from", from, "to", to, "programmeId", programmeId, "foodItemId", foodItemId),
                SHORTAGE_COLUMNS, rows, totals, t);
    }

    @Override
    public ReportResponse expiryReport(Long foodItemId, Integer days) {
        DownstreamTracker t = new DownstreamTracker();
        int window = days == null ? 7 : Math.max(1, Math.min(365, days));
        JsonNode expiring = t.node(INVENTORY, () -> inventoryClient.expiring(window));
        JsonNode expired = t.node(INVENTORY, inventoryClient::expired);

        List<String> columns = new ArrayList<>();
        columns.add("status");
        columns.addAll(BATCH_COLUMNS);

        List<Map<String, Object>> rows = new ArrayList<>();
        long expiringCount = 0;
        long expiredCount = 0;
        for (JsonNode b : JsonUtil.items(expiring)) {
            if (matchesId(b, "foodItemId", foodItemId)) {
                rows.add(batchRow("EXPIRING", b));
                expiringCount++;
            }
        }
        for (JsonNode b : JsonUtil.items(expired)) {
            if (matchesId(b, "foodItemId", foodItemId)) {
                rows.add(batchRow("EXPIRED", b));
                expiredCount++;
            }
        }
        Map<String, Object> totals = new LinkedHashMap<>();
        totals.put("expiringWithinDays", window);
        totals.put("expiringBatches", expiringCount);
        totals.put("expiredBatches", expiredCount);

        return report("expiry", "Expiry report", JsonUtil.pairs("foodItemId", foodItemId, "days", window),
                columns, rows, totals, t);
    }

    @Override
    public ReportResponse qualityWastageReport(LocalDate from, LocalDate to, Long foodItemId) {
        DownstreamTracker t = new DownstreamTracker();
        JsonNode wastage = t.node(QUALITY, qualityClient::wastage);
        JsonNode inspections = t.node(QUALITY, qualityClient::inspections);
        JsonNode actions = t.node(QUALITY, qualityClient::correctiveActions);

        List<JsonNode> filtered = new ArrayList<>();
        for (JsonNode w : JsonUtil.items(wastage)) {
            if (JsonUtil.inRange(JsonUtil.date(w, "recordedOn"), from, to) && matchesId(w, "foodItemId", foodItemId)) {
                filtered.add(w);
            }
        }
        List<JsonNode> inspectionsInRange = new ArrayList<>();
        for (JsonNode i : JsonUtil.items(inspections)) {
            if (JsonUtil.inRange(JsonUtil.date(i, "inspectionDate"), from, to) && matchesId(i, "foodItemId", foodItemId)) {
                inspectionsInRange.add(i);
            }
        }
        Map<String, Object> totals = new LinkedHashMap<>();
        totals.put("wastageRecords", filtered.size());
        totals.put("wastageQuantityByUnit", JsonUtil.sumBy(filtered, "unit", "quantity"));
        totals.put("wastageRecordsByReason", JsonUtil.countBy(filtered, "reason"));
        totals.put("wastageRecordsByMonth", JsonUtil.countByMonth(filtered, "recordedOn"));
        totals.put("inspections", inspectionsInRange.size());
        totals.put("inspectionsByResult", JsonUtil.countBy(inspectionsInRange, "result"));
        totals.put("correctiveActionsByStatus", JsonUtil.countBy(JsonUtil.items(actions), "status"));

        return report("quality-wastage", "Quality and wastage report",
                JsonUtil.pairs("from", from, "to", to, "foodItemId", foodItemId),
                WASTAGE_COLUMNS, rows(filtered, WASTAGE_COLUMNS), totals, t);
    }

    @Override
    public List<ServiceStatusResponse> serviceStatus() {
        List<ServiceStatusResponse> out = new ArrayList<>();
        out.add(probe(beneficiaryClient, beneficiaryClient::stats));
        out.add(probe(foodClient, foodClient::stats));
        out.add(probe(procurementClient, procurementClient::summary));
        out.add(probe(inventoryClient, inventoryClient::summary));
        out.add(probe(distributionClient, distributionClient::summary));
        out.add(probe(programmeClient, programmeClient::summary));
        out.add(probe(qualityClient, qualityClient::summary));
        return out;
    }

    // ------------------------------------------------------------------ helpers

    private ServiceStatusResponse probe(BaseClient client, Supplier<JsonNode> call) {
        try {
            call.get();
            return new ServiceStatusResponse(client.serviceName(), client.baseUrl(), true, null);
        } catch (DownstreamServiceException e) {
            return new ServiceStatusResponse(client.serviceName(), client.baseUrl(), false, e.getMessage());
        }
    }

    private ReportResponse report(String key, String title, Map<String, Object> filters, List<String> columns,
                                  List<Map<String, Object>> rows, Map<String, Object> totals, DownstreamTracker t) {
        return new ReportResponse(key, title, LocalDateTime.now(), filters, columns, rows, totals, t.unavailable());
    }

    private static List<Map<String, Object>> rows(List<JsonNode> items, List<String> columns) {
        List<Map<String, Object>> rows = new ArrayList<>();
        for (JsonNode item : items) {
            rows.add(JsonUtil.row(item, columns));
        }
        return rows;
    }

    private static Map<String, Object> batchRow(String status, JsonNode batch) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("status", status);
        row.putAll(JsonUtil.row(batch, BATCH_COLUMNS));
        return row;
    }

    private static boolean matchesId(JsonNode node, String field, Long id) {
        if (id == null) {
            return true;
        }
        JsonNode v = node.path(field);
        return v.isNumber() && v.asLong() == id;
    }

    private static boolean hasFoodItem(JsonNode request, Long foodItemId) {
        if (foodItemId == null) {
            return true;
        }
        for (JsonNode item : request.path("items")) {
            if (matchesId(item, "foodItemId", foodItemId)) {
                return true;
            }
        }
        return false;
    }

    private static boolean overlaps(JsonNode programme, LocalDate from, LocalDate to) {
        LocalDate start = JsonUtil.date(programme, "startDate");
        LocalDate end = JsonUtil.date(programme, "endDate");
        if (to != null && start != null && start.isAfter(to)) {
            return false;
        }
        return !(from != null && end != null && end.isBefore(from));
    }

    /** Number of items whose field equals one of the values, or null when the source service was unavailable. */
    private static Long countWhere(JsonNode listNode, String field, String... values) {
        if (listNode.isMissingNode()) {
            return null;
        }
        List<String> wanted = Arrays.asList(values);
        long count = 0;
        for (JsonNode item : JsonUtil.items(listNode)) {
            if (wanted.contains(JsonUtil.text(item, field))) {
                count++;
            }
        }
        return count;
    }

    private static BigDecimal sumField(JsonNode listNode, String field) {
        if (listNode.isMissingNode()) {
            return null;
        }
        BigDecimal sum = BigDecimal.ZERO;
        for (JsonNode item : JsonUtil.items(listNode)) {
            sum = sum.add(JsonUtil.decimal(item, field));
        }
        return sum;
    }

    private static BigDecimal unitTotal(JsonNode wastageList, String unit) {
        if (wastageList.isMissingNode()) {
            return null;
        }
        return JsonUtil.sumBy(JsonUtil.items(wastageList), "unit", "quantity").getOrDefault(unit, BigDecimal.ZERO);
    }
}
