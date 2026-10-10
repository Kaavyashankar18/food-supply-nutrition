package com.foodsupply.webui.controller;

import com.foodsupply.webui.client.InventoryClient;
import com.foodsupply.webui.dto.Action;
import com.foodsupply.webui.dto.Field;
import com.foodsupply.webui.dto.Tab;
import com.foodsupply.webui.exception.ApiException;
import com.foodsupply.webui.util.FormUtil;
import com.foodsupply.webui.util.Opts;
import com.foodsupply.webui.util.Views;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryClient client;

    private List<Tab> tabs(String active) {
        return List.of(new Tab("Stock levels", "/inventory", "stock".equals(active)),
                new Tab("Batches", "/inventory/batches", "batches".equals(active)),
                new Tab("Alerts", "/inventory/alerts", "alerts".equals(active)),
                new Tab("Movements", "/inventory/movements", "movements".equals(active)));
    }

    @GetMapping
    public String stock(@RequestParam(required = false) String q, @RequestParam(required = false) String lowStock,
                        @RequestParam(defaultValue = "0") int page, Model model) {
        Map<String, Object> result = client.page("/api/v1/stock", Math.max(page, 0), 20,
                "q", q, "lowStock", lowStock, "sort", "foodItemName", "dir", "asc");
        List<Map<String, Object>> rows = Views.rows(result);
        List<Field> filters = List.of(Field.text("q", "Search item", q),
                Field.select("lowStock", "Low stock", lowStock, Opts.of("All items", "true")));
        Map<String, Object> summary = client.get("/api/v1/stock/summary");
        Views.list(model, "Stock levels", "inventory", tabs("stock"), filters, "/inventory", result, rows,
                Views.columns(rows, "foodItemName", "unit", "onHand", "reserved", "usable", "minimumStockLevel",
                        "expiringSoon", "quarantined", "lowStock"),
                "/inventory/item", "foodItemId", null, null,
                FormUtil.listUrl("/inventory", "q", q, "lowStock", lowStock),
                Views.cards(summary, "totalItems", "totalBatches", "lowStockItems", "outOfStockItems",
                        "expiringBatches", "expiredBatches", "quarantinedBatches", "activeReservations"));
        return "generic/list";
    }

    @GetMapping("/item/{foodItemId}")
    public String item(@PathVariable Long foodItemId, Model model) {
        Map<String, Object> level = client.get("/api/v1/stock/{id}", foodItemId);
        Map<String, Object> batches = client.page("/api/v1/batches", 0, 100, "foodItemId", String.valueOf(foodItemId));
        Map<String, Object> all = new LinkedHashMap<>(level);
        all.put("batches", Views.rows(batches));
        Views.detail(model, Opts.str(level.get("foodItemName")), "inventory", "/inventory", "Stock levels",
                tabs("stock"), Views.sections(all),
                List.of(Action.link("Change minimum level", "/inventory/item/" + foodItemId + "/minimum", "outline-primary")));
        return "generic/detail";
    }

    @GetMapping("/item/{foodItemId}/minimum")
    public String minimumForm(@PathVariable Long foodItemId, Model model) {
        Map<String, Object> level = client.get("/api/v1/stock/{id}", foodItemId);
        Views.form(model, "Minimum level for " + Opts.str(level.get("foodItemName")), "inventory",
                "/inventory/item/" + foodItemId, "/inventory/item/" + foodItemId + "/minimum",
                List.of(Field.number("value", "Minimum stock level (" + Opts.str(level.get("unit")) + ")",
                        Opts.str(level.get("minimumStockLevel"))).req()), null, "Save");
        return "generic/form";
    }

    @PostMapping("/item/{foodItemId}/minimum")
    public String setMinimum(@PathVariable Long foodItemId, @RequestParam String value, RedirectAttributes ra) {
        try {
            client.patch("/api/v1/stock/{id}/minimum-level?value={value}", foodItemId, value);
            ra.addFlashAttribute("flashSuccess", "Minimum level updated.");
        } catch (ApiException ex) {
            ra.addFlashAttribute("flashError", ex.getMessage());
        }
        return "redirect:/inventory/item/" + foodItemId;
    }

    @GetMapping("/batches")
    public String batches(@RequestParam(required = false) String q, @RequestParam(required = false) String foodItemId,
                          @RequestParam(required = false) String qualityStatus,
                          @RequestParam(required = false) String expired,
                          @RequestParam(required = false) String expiringWithinDays,
                          @RequestParam(defaultValue = "0") int page, Model model) {
        Map<String, Object> result = client.page("/api/v1/batches", Math.max(page, 0), 20, "q", q,
                "foodItemId", foodItemId, "qualityStatus", qualityStatus, "expired", expired,
                "expiringWithinDays", expiringWithinDays, "sort", "expiryDate", "dir", "asc");
        List<Map<String, Object>> rows = Views.rows(result);
        List<Field> filters = List.of(Field.text("q", "Batch number", q), Field.text("foodItemId", "Food item id", foodItemId),
                Field.select("qualityStatus", "Quality", qualityStatus, Opts.of("Any quality", "APPROVED", "QUARANTINED", "REJECTED")),
                Field.select("expired", "Expired", expired, Opts.of("Expired or not", "true", "false")),
                Field.text("expiringWithinDays", "Expiring within (days)", expiringWithinDays));
        Views.list(model, "Stock batches", "inventory", tabs("batches"), filters, "/inventory/batches", result, rows,
                Views.columns(rows, "id", "batchNumber", "foodItemName", "usableQuantity", "quantityReserved",
                        "expiryDate", "daysToExpiry", "qualityStatus"),
                "/inventory/batches", "id", null, null,
                FormUtil.listUrl("/inventory/batches", "q", q, "foodItemId", foodItemId, "qualityStatus", qualityStatus,
                        "expired", expired, "expiringWithinDays", expiringWithinDays), null);
        return "generic/list";
    }

    @GetMapping("/batches/{id}")
    public String batch(@PathVariable Long id, Model model) {
        Map<String, Object> b = client.get("/api/v1/batches/{id}", id);
        Views.detail(model, "Batch " + Opts.str(b.get("batchNumber")), "inventory", "/inventory/batches", "All batches",
                tabs("batches"), Views.sections(b),
                List.of(Action.link("Record quality inspection", "/quality/inspections/new?batchId=" + id, "outline-primary"),
                        Action.link("Record wastage", "/quality/wastage/new?batchId=" + id, "outline-danger")));
        return "generic/detail";
    }

    @GetMapping("/alerts")
    public String alerts(@RequestParam(defaultValue = "7") String days, Model model) {
        Map<String, Object> all = new LinkedHashMap<>();
        all.put("Low stock items", client.list("/api/v1/alerts/low-stock"));
        all.put("Batches expiring soon", client.list("/api/v1/alerts/expiring", "days", days));
        all.put("Expired batches", client.list("/api/v1/alerts/expired"));
        Views.detail(model, "Stock alerts", "inventory", null, null, tabs("alerts"), Views.sections(all), null);
        return "generic/detail";
    }

    @GetMapping("/movements")
    public String movements(@RequestParam(required = false) String foodItemId, @RequestParam(required = false) String type,
                            @RequestParam(required = false) String referenceType,
                            @RequestParam(defaultValue = "0") int page, Model model) {
        Map<String, Object> result = client.page("/api/v1/movements", Math.max(page, 0), 20, "foodItemId", foodItemId,
                "type", type, "referenceType", referenceType);
        List<Map<String, Object>> rows = Views.rows(result);
        List<Field> filters = List.of(Field.text("foodItemId", "Food item id", foodItemId),
                Field.select("type", "Type", type, Opts.of("Any type", "RECEIPT", "RESERVE", "RELEASE", "DISPATCH",
                        "ADJUSTMENT", "WASTAGE")),
                Field.text("referenceType", "Reference type", referenceType));
        Views.list(model, "Stock movements", "inventory", tabs("movements"), filters, "/inventory/movements", result, rows,
                Views.columns(rows, "createdAt", "type", "foodItemId", "batchId", "quantity", "referenceType",
                        "referenceId", "remarks"),
                null, "id", null, null,
                FormUtil.listUrl("/inventory/movements", "foodItemId", foodItemId, "type", type,
                        "referenceType", referenceType), null);
        return "generic/list";
    }
}
