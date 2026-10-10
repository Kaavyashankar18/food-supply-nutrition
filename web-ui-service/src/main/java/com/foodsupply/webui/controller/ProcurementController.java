package com.foodsupply.webui.controller;

import com.foodsupply.webui.client.FoodClient;
import com.foodsupply.webui.client.ProcurementClient;
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
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/procurement")
@RequiredArgsConstructor
public class ProcurementController {

    private static final String REQ = "/api/v1/procurement-requests";
    private static final String PO = "/api/v1/purchase-orders";
    private static final String SUP = "/api/v1/suppliers";

    private final ProcurementClient client;
    private final FoodClient food;

    private List<Tab> tabs(String active) {
        return List.of(new Tab("Requests", "/procurement/requests", "requests".equals(active)),
                new Tab("Purchase orders", "/procurement/orders", "orders".equals(active)),
                new Tab("Suppliers", "/procurement/suppliers", "suppliers".equals(active)));
    }

    @GetMapping
    public String home() {
        return "redirect:/procurement/requests";
    }

    // ---------- requests ----------

    @GetMapping("/requests")
    public String requests(@RequestParam(required = false) String q, @RequestParam(required = false) String status,
                           @RequestParam(required = false) String source,
                           @RequestParam(defaultValue = "0") int page, Model model) {
        Map<String, Object> result = client.page(REQ, Math.max(page, 0), 20, "q", q, "status", status, "source", source);
        List<Map<String, Object>> rows = Views.rows(result);
        List<Field> filters = List.of(Field.text("q", "Search", q),
                Field.select("status", "Status", status, Opts.of("Any status", "DRAFT", "SUBMITTED", "APPROVED",
                        "REJECTED", "ORDERED", "COMPLETED")),
                Field.select("source", "Source", source, Opts.of("Any source", "MANUAL", "SHORTAGE")));
        Map<String, Object> summary = client.get("/api/v1/procurement/summary");
        Views.list(model, "Procurement requests", "procurement", tabs("requests"), filters, "/procurement/requests",
                result, rows, Views.columns(rows, "requestNumber", "status", "source", "reason", "estimatedCost",
                        "requestedBy", "createdOn", "needByDate"),
                "/procurement/requests", "id", "/procurement/requests/new", "New request",
                FormUtil.listUrl("/procurement/requests", "q", q, "status", status, "source", source),
                Views.cards(summary, "pendingRequests", "totalEstimatedCost", "totalOrderValue"));
        return "generic/list";
    }

    @GetMapping("/requests/new")
    public String newRequest(Model model) {
        showRequestForm(model, null);
        return "generic/form";
    }

    private void showRequestForm(Model model, String error) {
        List<Map<String, Object>> items = Views.rows(food.list(null, null, null, "true", 0, 1000));
        List<Field> fields = new ArrayList<>();
        fields.add(Field.text("requestedBy", "Requested by", "store.manager").req());
        fields.add(Field.date("needByDate", "Need by date", LocalDate.now().plusDays(7).toString()));
        fields.add(Field.area("reason", "Reason", ""));
        fields.add(Field.select("submit", "After saving", "false",
                List.of(new com.foodsupply.webui.dto.Option("false", "Keep as draft"),
                        new com.foodsupply.webui.dto.Option("true", "Submit for approval"))));
        for (int i = 1; i <= 5; i++) {
            fields.add(Field.select("foodItemId", "Item " + i, "", Opts.from(items, i == 1 ? "Choose item" : "(none)", "id", "name", "unit")));
            fields.add(Field.number("quantity", "Item " + i + " quantity", ""));
            fields.add(Field.number("estimatedUnitPrice", "Item " + i + " unit price", ""));
        }
        Views.form(model, "New procurement request", "procurement", "/procurement/requests", "/procurement/requests/save",
                fields, error, "Save request");
    }

    @PostMapping("/requests/save")
    public String saveRequest(@RequestParam MultiValueMap<String, String> params, Model model, RedirectAttributes ra) {
        try {
            List<String> foods = params.getOrDefault("foodItemId", List.of());
            List<String> qtys = params.getOrDefault("quantity", List.of());
            List<String> prices = params.getOrDefault("estimatedUnitPrice", List.of());
            List<Map<String, Object>> items = new ArrayList<>();
            for (int i = 0; i < foods.size(); i++) {
                if (Opts.blank(foods.get(i))) {
                    continue;
                }
                if (i >= qtys.size() || Opts.blank(qtys.get(i))) {
                    throw new ApiException(400, "Item " + (i + 1) + ": please enter a quantity");
                }
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("foodItemId", Opts.toLong("foodItemId", foods.get(i)));
                item.put("quantity", Opts.toDecimal("quantity", qtys.get(i)));
                if (i < prices.size() && !Opts.blank(prices.get(i))) {
                    item.put("estimatedUnitPrice", Opts.toDecimal("estimatedUnitPrice", prices.get(i)));
                }
                items.add(item);
            }
            if (items.isEmpty()) {
                throw new ApiException(400, "Add at least one item");
            }
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("requestedBy", params.getFirst("requestedBy"));
            body.put("reason", Opts.blank(params.getFirst("reason")) ? null : params.getFirst("reason"));
            body.put("needByDate", Opts.blank(params.getFirst("needByDate")) ? null : params.getFirst("needByDate"));
            body.put("source", "MANUAL");
            body.put("submit", "true".equals(params.getFirst("submit")));
            body.put("items", items);
            Map<String, Object> saved = client.post(REQ, body);
            ra.addFlashAttribute("flashSuccess", "Request " + Opts.str(saved.get("requestNumber")) + " created.");
            return "redirect:/procurement/requests/" + saved.get("id");
        } catch (ApiException ex) {
            showRequestForm(model, ex.getMessage());
            return "generic/form";
        }
    }

    @GetMapping("/requests/{id}")
    public String viewRequest(@PathVariable Long id, Model model) {
        Map<String, Object> r = client.get(REQ + "/{id}", id);
        String status = Opts.str(r.get("status"));
        List<Action> actions = new ArrayList<>();
        if ("DRAFT".equals(status)) {
            actions.add(Action.post("Submit", "/procurement/requests/" + id + "/submit", "primary", null));
        }
        if ("SUBMITTED".equals(status)) {
            actions.add(Action.post("Approve", "/procurement/requests/" + id + "/approve", "success", null));
            actions.add(Action.post("Reject", "/procurement/requests/" + id + "/reject", "outline-danger", "Reject this request?"));
        }
        if ("APPROVED".equals(status)) {
            actions.add(Action.link("Create purchase order", "/procurement/requests/" + id + "/order", "primary"));
        }
        Views.detail(model, "Request " + Opts.str(r.get("requestNumber")), "procurement", "/procurement/requests",
                "All requests", tabs("requests"), Views.sections(r), actions);
        return "generic/detail";
    }

    @PostMapping("/requests/{id}/{action}")
    public String requestAction(@PathVariable Long id, @PathVariable String action, RedirectAttributes ra) {
        try {
            if (!List.of("submit", "approve", "reject").contains(action)) {
                throw new ApiException(400, "Unknown action");
            }
            String uri = REQ + "/{id}/" + action;
            if ("reject".equals(action)) {
                client.post(uri + "?reason={reason}", null, id, "Rejected from the web UI");
            } else {
                client.post(uri, null, id);
            }
            ra.addFlashAttribute("flashSuccess", "Request " + Map.of("submit", "submitted", "approve", "approved", "reject", "rejected").get(action) + ".");
        } catch (ApiException ex) {
            ra.addFlashAttribute("flashError", ex.getMessage());
        }
        return "redirect:/procurement/requests/" + id;
    }

    @GetMapping("/requests/{id}/order")
    public String orderForm(@PathVariable Long id, Model model) {
        showOrderForm(model, id, null);
        return "generic/form";
    }

    private void showOrderForm(Model model, Long requestId, String error) {
        List<Map<String, Object>> suppliers = Views.rows(client.page(SUP, 0, 200, "status", "ACTIVE"));
        List<Field> fields = List.of(
                Field.select("supplierId", "Supplier", "", Opts.from(suppliers, null, "id", "name")).req(),
                Field.date("expectedDeliveryDate", "Expected delivery", LocalDate.now().plusDays(5).toString()).req());
        Views.form(model, "Create purchase order", "procurement", "/procurement/requests/" + requestId,
                "/procurement/requests/" + requestId + "/order", fields, error, "Create order");
    }

    @PostMapping("/requests/{id}/order")
    public String createOrder(@PathVariable Long id, @RequestParam Map<String, String> params, Model model) {
        try {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("requestId", id);
            body.put("supplierId", Opts.toLong("supplierId", params.getOrDefault("supplierId", "")));
            body.put("expectedDeliveryDate", params.get("expectedDeliveryDate"));
            Map<String, Object> po = client.post(PO, body);
            return "redirect:/procurement/orders/" + po.get("id");
        } catch (ApiException ex) {
            showOrderForm(model, id, ex.getMessage());
            return "generic/form";
        }
    }

    // ---------- purchase orders ----------

    @GetMapping("/orders")
    public String orders(@RequestParam(required = false) String q, @RequestParam(required = false) String status,
                         @RequestParam(defaultValue = "0") int page, Model model) {
        Map<String, Object> result = client.page(PO, Math.max(page, 0), 20, "q", q, "status", status);
        List<Map<String, Object>> rows = Views.rows(result);
        List<Field> filters = List.of(Field.text("q", "Search", q),
                Field.select("status", "Status", status, Opts.of("Any status", "CREATED", "SENT",
                        "PARTIALLY_RECEIVED", "RECEIVED", "CANCELLED")));
        Views.list(model, "Purchase orders", "procurement", tabs("orders"), filters, "/procurement/orders", result, rows,
                Views.columns(rows, "poNumber", "supplierName", "status", "orderDate", "expectedDeliveryDate", "requestId"),
                "/procurement/orders", "id", null, null,
                FormUtil.listUrl("/procurement/orders", "q", q, "status", status), null);
        return "generic/list";
    }

    @GetMapping("/orders/{id}")
    public String viewOrder(@PathVariable Long id, Model model) {
        Map<String, Object> po = client.get(PO + "/{id}", id);
        String status = Opts.str(po.get("status"));
        List<Action> actions = new ArrayList<>();
        if ("CREATED".equals(status)) {
            actions.add(Action.post("Mark as sent", "/procurement/orders/" + id + "/status?value=SENT", "outline-primary", null));
        }
        if ("CREATED".equals(status) || "SENT".equals(status) || "PARTIALLY_RECEIVED".equals(status)) {
            actions.add(Action.link("Receive goods", "/procurement/orders/" + id + "/receive", "success"));
        }
        if ("CREATED".equals(status) || "SENT".equals(status)) {
            actions.add(Action.post("Cancel order", "/procurement/orders/" + id + "/status?value=CANCELLED",
                    "outline-danger", "Cancel this purchase order?"));
        }
        Views.detail(model, "Purchase order " + Opts.str(po.get("poNumber")), "procurement", "/procurement/orders",
                "All purchase orders", tabs("orders"), Views.sections(po), actions);
        return "generic/detail";
    }

    @PostMapping("/orders/{id}/status")
    public String orderStatus(@PathVariable Long id, @RequestParam String value, RedirectAttributes ra) {
        try {
            client.patch(PO + "/{id}/status?value={value}", id, value);
            ra.addFlashAttribute("flashSuccess", "Order marked " + value + ".");
        } catch (ApiException ex) {
            ra.addFlashAttribute("flashError", ex.getMessage());
        }
        return "redirect:/procurement/orders/" + id;
    }

    @GetMapping("/orders/{id}/receive")
    public String receiveForm(@PathVariable Long id, Model model) {
        showReceiveForm(model, id, null);
        return "generic/form";
    }

    @SuppressWarnings("unchecked")
    private void showReceiveForm(Model model, Long id, String error) {
        Map<String, Object> po = client.get(PO + "/{id}", id);
        List<Map<String, Object>> items = po.get("items") instanceof List<?> l
                ? (List<Map<String, Object>>) l : new ArrayList<>();
        List<Field> fields = new ArrayList<>();
        int line = 1;
        for (Map<String, Object> item : items) {
            String name = Opts.str(item.get("foodItemName"));
            double ordered = item.get("orderedQty") instanceof Number n ? n.doubleValue() : 0;
            double received = item.get("receivedQty") instanceof Number n ? n.doubleValue() : 0;
            double remaining = Math.max(ordered - received, 0);
            fields.add(Field.hidden("itemId", Opts.str(item.get("id"))));
            fields.add(Field.number("qty", name + " - quantity received (ordered " + ordered + ", already " + received + ")",
                    remaining > 0 ? String.valueOf(remaining) : ""));
            fields.add(Field.text("batch", name + " - batch number",
                    Opts.str(po.get("poNumber")) + "-L" + line));
            fields.add(Field.date("expiry", name + " - expiry date", LocalDate.now().plusDays(120).toString()));
            fields.add(Field.text("loc", name + " - storage location", "Main Warehouse"));
            line++;
        }
        Views.form(model, "Receive goods for " + Opts.str(po.get("poNumber")), "procurement",
                "/procurement/orders/" + id, "/procurement/orders/" + id + "/receive", fields, error, "Receive into stock");
    }

    @PostMapping("/orders/{id}/receive")
    public String receive(@PathVariable Long id, @RequestParam MultiValueMap<String, String> params,
                          Model model, RedirectAttributes ra) {
        try {
            List<String> ids = params.getOrDefault("itemId", List.of());
            List<String> qtys = params.getOrDefault("qty", List.of());
            List<String> batches = params.getOrDefault("batch", List.of());
            List<String> expiries = params.getOrDefault("expiry", List.of());
            List<String> locs = params.getOrDefault("loc", List.of());
            List<Map<String, Object>> lines = new ArrayList<>();
            for (int i = 0; i < ids.size() && i < qtys.size(); i++) {
                if (Opts.blank(qtys.get(i)) || Opts.toDecimal("qty", qtys.get(i)).signum() <= 0) {
                    continue;
                }
                Map<String, Object> line = new LinkedHashMap<>();
                line.put("itemId", Opts.toLong("itemId", ids.get(i)));
                line.put("quantityReceived", Opts.toDecimal("qty", qtys.get(i)));
                line.put("batchNumber", i < batches.size() ? batches.get(i) : null);
                line.put("expiryDate", i < expiries.size() && !Opts.blank(expiries.get(i)) ? expiries.get(i) : null);
                line.put("storageLocation", i < locs.size() && !Opts.blank(locs.get(i)) ? locs.get(i) : null);
                lines.add(line);
            }
            if (lines.isEmpty()) {
                throw new ApiException(400, "Enter a quantity for at least one line");
            }
            client.post(PO + "/{id}/receive", Map.of("items", lines), id);
            ra.addFlashAttribute("flashSuccess", "Goods received into inventory.");
            return "redirect:/procurement/orders/" + id;
        } catch (ApiException ex) {
            showReceiveForm(model, id, ex.getMessage());
            return "generic/form";
        }
    }

    // ---------- suppliers ----------

    @GetMapping("/suppliers")
    public String suppliers(@RequestParam(required = false) String q, @RequestParam(required = false) String status,
                            @RequestParam(defaultValue = "0") int page, Model model) {
        Map<String, Object> result = client.page(SUP, Math.max(page, 0), 20, "q", q, "status", status);
        List<Map<String, Object>> rows = Views.rows(result);
        List<Field> filters = List.of(Field.text("q", "Search", q),
                Field.select("status", "Status", status, Opts.of("Any status", "ACTIVE", "INACTIVE")));
        Views.list(model, "Suppliers", "procurement", tabs("suppliers"), filters, "/procurement/suppliers", result, rows,
                Views.columns(rows, "name", "contactPerson", "phone", "email", "status", "rating"),
                "/procurement/suppliers", "id", "/procurement/suppliers/new", "Add supplier",
                FormUtil.listUrl("/procurement/suppliers", "q", q, "status", status), null);
        return "generic/list";
    }

    @GetMapping("/suppliers/new")
    public String newSupplier(Model model) {
        showSupplierForm(model, null);
        return "generic/form";
    }

    private void showSupplierForm(Model model, String error) {
        List<Field> fields = List.of(Field.text("name", "Name", "").req(), Field.text("contactPerson", "Contact person", ""),
                Field.text("phone", "Phone", ""), Field.text("email", "Email", ""),
                Field.text("address", "Address", ""),
                Field.select("status", "Status", "ACTIVE", Opts.of(null, "ACTIVE", "INACTIVE")),
                Field.number("rating", "Rating (1-5, optional)", ""));
        Views.form(model, "New supplier", "procurement", "/procurement/suppliers", "/procurement/suppliers/save",
                fields, error, "Save supplier");
    }

    @PostMapping("/suppliers/save")
    public String saveSupplier(@RequestParam Map<String, String> params, Model model, RedirectAttributes ra) {
        try {
            Map<String, Object> saved = client.post(SUP, FormUtil.payload(params, java.util.Set.of(),
                    java.util.Set.of("rating")));
            ra.addFlashAttribute("flashSuccess", "Supplier saved.");
            return "redirect:/procurement/suppliers/" + saved.get("id");
        } catch (ApiException ex) {
            showSupplierForm(model, ex.getMessage());
            return "generic/form";
        }
    }

    @GetMapping("/suppliers/{id}")
    public String viewSupplier(@PathVariable Long id, Model model) {
        Map<String, Object> s = client.get(SUP + "/{id}", id);
        boolean active = "ACTIVE".equals(Opts.str(s.get("status")));
        List<Action> actions = List.of(Action.post(active ? "Deactivate" : "Activate",
                "/procurement/suppliers/" + id + "/status?value=" + (active ? "INACTIVE" : "ACTIVE"), "outline-secondary", null));
        Views.detail(model, Opts.str(s.get("name")), "procurement", "/procurement/suppliers", "All suppliers",
                tabs("suppliers"), Views.sections(s), actions);
        return "generic/detail";
    }

    @PostMapping("/suppliers/{id}/status")
    public String supplierStatus(@PathVariable Long id, @RequestParam String value, RedirectAttributes ra) {
        try {
            client.patch(SUP + "/{id}/status?value={value}", id, value);
            ra.addFlashAttribute("flashSuccess", "Supplier is now " + value + ".");
        } catch (ApiException ex) {
            ra.addFlashAttribute("flashError", ex.getMessage());
        }
        return "redirect:/procurement/suppliers/" + id;
    }
}
