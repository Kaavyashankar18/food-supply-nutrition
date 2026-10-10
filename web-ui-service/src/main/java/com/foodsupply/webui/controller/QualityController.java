package com.foodsupply.webui.controller;

import com.foodsupply.webui.client.InventoryClient;
import com.foodsupply.webui.client.QualityClient;
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

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Controller
@RequestMapping("/quality")
@RequiredArgsConstructor
public class QualityController {

    private final QualityClient client;
    private final InventoryClient inventory;

    private List<Tab> tabs(String active) {
        return List.of(new Tab("Overview", "/quality", "overview".equals(active)),
                new Tab("Inspections", "/quality/inspections", "inspections".equals(active)),
                new Tab("Wastage", "/quality/wastage", "wastage".equals(active)),
                new Tab("Corrective actions", "/quality/actions", "actions".equals(active)));
    }

    @GetMapping
    public String overview(Model model) {
        List<com.foodsupply.webui.dto.Section> sections = new ArrayList<>();
        sections.addAll(Views.sections(client.get("/api/v1/quality/summary")));
        sections.addAll(Views.sections(client.get("/api/v1/wastage/summary")));
        Views.detail(model, "Quality and wastage", "quality", null, null, tabs("overview"), sections,
                List.of(Action.link("Record inspection", "/quality/inspections/new", "primary"),
                        Action.link("Record wastage", "/quality/wastage/new", "outline-danger"),
                        Action.link("Add corrective action", "/quality/actions/new", "outline-secondary")));
        return "generic/detail";
    }

    // ---------- inspections ----------

    @GetMapping("/inspections")
    public String inspections(@RequestParam(required = false) String q, @RequestParam(required = false) String result,
                              @RequestParam(required = false) String qualityStatus,
                              @RequestParam(defaultValue = "0") int page, Model model) {
        Map<String, Object> data = client.page("/api/v1/inspections", Math.max(page, 0), 20, "q", q, "result", result,
                "qualityStatus", qualityStatus);
        List<Map<String, Object>> rows = Views.rows(data);
        List<Field> filters = List.of(Field.text("q", "Search", q),
                Field.select("result", "Result", result, Opts.of("Any result", "PASS", "FAIL", "CONDITIONAL")),
                Field.select("qualityStatus", "Status", qualityStatus, Opts.of("Any status", "APPROVED", "QUARANTINED", "REJECTED")));
        Views.list(model, "Quality inspections", "quality", tabs("inspections"), filters, "/quality/inspections", data, rows,
                Views.columns(rows, "inspectionDate", "foodItemName", "batchNumber", "inspector", "result", "qualityStatus", "remarks"),
                "/quality/inspections", "id", "/quality/inspections/new", "Record inspection",
                FormUtil.listUrl("/quality/inspections", "q", q, "result", result, "qualityStatus", qualityStatus), null);
        return "generic/list";
    }

    private List<Field> batchField(String batchId) {
        List<Map<String, Object>> batches = Views.rows(inventory.page("/api/v1/batches", 0, 1000,
                "inStock", "true", "sort", "expiryDate", "dir", "asc"));
        List<Map<String, Object>> labelled = new ArrayList<>();
        for (Map<String, Object> b : batches) {
            Map<String, Object> copy = new LinkedHashMap<>(b);
            copy.put("label", "#" + Opts.str(b.get("id")) + " " + Opts.str(b.get("batchNumber")) + " - "
                    + Opts.str(b.get("foodItemName")) + " (" + Opts.str(b.get("usableQuantity")) + " usable, "
                    + Opts.str(b.get("qualityStatus")) + ")");
            labelled.add(copy);
        }
        return List.of(Field.select("batchId", "Stock batch", batchId == null ? "" : batchId,
                Opts.from(labelled, null, "id", "label")).req());
    }

    private void inspectionForm(Model model, String batchId, Map<String, String> v, String error) {
        List<Field> fields = new ArrayList<>(batchField(batchId));
        fields.add(Field.text("inspector", "Inspector", v.getOrDefault("inspector", "Meena Krishnan")).req());
        fields.add(Field.select("result", "Result", v.getOrDefault("result", "PASS"), Opts.of(null, "PASS", "FAIL", "CONDITIONAL")));
        fields.add(Field.select("qualityStatus", "Batch status afterwards", v.getOrDefault("qualityStatus", ""),
                Opts.of("Automatic (from result)", "APPROVED", "QUARANTINED", "REJECTED")));
        fields.add(Field.area("remarks", "Remarks", v.getOrDefault("remarks", "")));
        Views.form(model, "Record quality inspection", "quality", "/quality/inspections", "/quality/inspections/save",
                fields, error, "Save inspection");
    }

    @GetMapping("/inspections/new")
    public String newInspection(@RequestParam(required = false) String batchId, Model model) {
        inspectionForm(model, batchId, Map.of(), null);
        return "generic/form";
    }

    @PostMapping("/inspections/save")
    public String saveInspection(@RequestParam Map<String, String> params, Model model, RedirectAttributes ra) {
        try {
            Map<String, Object> saved = client.post("/api/v1/inspections", FormUtil.payload(params, Set.of("batchId"), Set.of()));
            ra.addFlashAttribute("flashSuccess", "Inspection saved and the batch status updated in inventory.");
            return "redirect:/quality/inspections/" + saved.get("id");
        } catch (ApiException ex) {
            inspectionForm(model, params.get("batchId"), params, ex.getMessage());
            return "generic/form";
        }
    }

    @GetMapping("/inspections/{id}")
    public String inspection(@PathVariable Long id, Model model) {
        Map<String, Object> i = client.get("/api/v1/inspections/{id}", id);
        Views.detail(model, "Inspection #" + id, "quality", "/quality/inspections", "All inspections", tabs("inspections"),
                Views.sections(i), List.of(Action.link("Add corrective action", "/quality/actions/new?inspectionId=" + id, "outline-primary")));
        return "generic/detail";
    }

    // ---------- wastage ----------

    @GetMapping("/wastage")
    public String wastage(@RequestParam(required = false) String q, @RequestParam(required = false) String reason,
                          @RequestParam(defaultValue = "0") int page, Model model) {
        Map<String, Object> data = client.page("/api/v1/wastage", Math.max(page, 0), 20, "q", q, "reason", reason);
        List<Map<String, Object>> rows = Views.rows(data);
        List<Field> filters = List.of(Field.text("q", "Search", q),
                Field.select("reason", "Reason", reason, Opts.of("Any reason", "EXPIRED", "DAMAGED", "SPOILED", "CONTAMINATED", "OTHER")));
        Views.list(model, "Wastage records", "quality", tabs("wastage"), filters, "/quality/wastage", data, rows,
                Views.columns(rows, "recordedOn", "foodItemName", "batchNumber", "quantity", "unit", "reason", "disposalMethod", "recordedBy"),
                "/quality/wastage", "id", "/quality/wastage/new", "Record wastage",
                FormUtil.listUrl("/quality/wastage", "q", q, "reason", reason), null);
        return "generic/list";
    }

    private void wastageForm(Model model, String batchId, Map<String, String> v, String error) {
        List<Field> fields = new ArrayList<>(batchField(batchId));
        fields.add(Field.number("quantity", "Quantity wasted", v.getOrDefault("quantity", "")).req());
        fields.add(Field.select("reason", "Reason", v.getOrDefault("reason", "SPOILED"),
                Opts.of(null, "EXPIRED", "DAMAGED", "SPOILED", "CONTAMINATED", "OTHER")));
        fields.add(Field.text("disposalMethod", "Disposal method", v.getOrDefault("disposalMethod", "Composting")));
        fields.add(Field.text("recordedBy", "Recorded by", v.getOrDefault("recordedBy", "Store Manager")).req());
        Views.form(model, "Record wastage", "quality", "/quality/wastage", "/quality/wastage/save", fields, error, "Save wastage");
    }

    @GetMapping("/wastage/new")
    public String newWastage(@RequestParam(required = false) String batchId, Model model) {
        wastageForm(model, batchId, Map.of(), null);
        return "generic/form";
    }

    @PostMapping("/wastage/save")
    public String saveWastage(@RequestParam Map<String, String> params, Model model, RedirectAttributes ra) {
        try {
            Map<String, Object> saved = client.post("/api/v1/wastage",
                    FormUtil.payload(params, Set.of("batchId"), Set.of("quantity")));
            ra.addFlashAttribute("flashSuccess", "Wastage recorded and stock written off in inventory.");
            return "redirect:/quality/wastage/" + saved.get("id");
        } catch (ApiException ex) {
            wastageForm(model, params.get("batchId"), params, ex.getMessage());
            return "generic/form";
        }
    }

    @GetMapping("/wastage/{id}")
    public String wastageView(@PathVariable Long id, Model model) {
        Map<String, Object> w = client.get("/api/v1/wastage/{id}", id);
        Views.detail(model, "Wastage #" + id, "quality", "/quality/wastage", "All wastage records", tabs("wastage"),
                Views.sections(w), List.of(Action.link("Add corrective action", "/quality/actions/new?wastageId=" + id, "outline-primary")));
        return "generic/detail";
    }

    // ---------- corrective actions ----------

    @GetMapping("/actions")
    public String actions(@RequestParam(required = false) String q, @RequestParam(required = false) String status,
                          @RequestParam(required = false) String overdue, @RequestParam(defaultValue = "0") int page, Model model) {
        Map<String, Object> data = client.page("/api/v1/corrective-actions", Math.max(page, 0), 20, "q", q,
                "status", status, "overdue", overdue);
        List<Map<String, Object>> rows = Views.rows(data);
        List<Field> filters = List.of(Field.text("q", "Search", q),
                Field.select("status", "Status", status, Opts.of("Any status", "OPEN", "IN_PROGRESS", "CLOSED")),
                Field.select("overdue", "Overdue", overdue, Opts.of("All", "true")));
        Views.list(model, "Corrective actions", "quality", tabs("actions"), filters, "/quality/actions", data, rows,
                Views.columns(rows, "description", "assignedTo", "dueDate", "status", "inspectionId", "wastageId"),
                "/quality/actions", "id", "/quality/actions/new", "Add corrective action",
                FormUtil.listUrl("/quality/actions", "q", q, "status", status, "overdue", overdue), null);
        return "generic/list";
    }

    private void actionForm(Model model, Map<String, String> v, String error) {
        List<Field> fields = List.of(Field.number("inspectionId", "Inspection id", v.getOrDefault("inspectionId", "")),
                Field.number("wastageId", "Wastage record id", v.getOrDefault("wastageId", "")),
                Field.text("description", "What must be done", v.getOrDefault("description", "")).req(),
                Field.text("assignedTo", "Assigned to", v.getOrDefault("assignedTo", "")).req(),
                Field.date("dueDate", "Due date", v.getOrDefault("dueDate", LocalDate.now().plusDays(14).toString())).req());
        Views.form(model, "New corrective action", "quality", "/quality/actions", "/quality/actions/save", fields, error, "Save action");
    }

    @GetMapping("/actions/new")
    public String newAction(@RequestParam(required = false) String inspectionId, @RequestParam(required = false) String wastageId,
                            Model model) {
        Map<String, String> v = new LinkedHashMap<>();
        if (!Opts.blank(inspectionId)) {
            v.put("inspectionId", inspectionId);
        }
        if (!Opts.blank(wastageId)) {
            v.put("wastageId", wastageId);
        }
        actionForm(model, v, null);
        return "generic/form";
    }

    @PostMapping("/actions/save")
    public String saveAction(@RequestParam Map<String, String> params, Model model, RedirectAttributes ra) {
        try {
            Map<String, Object> saved = client.post("/api/v1/corrective-actions",
                    FormUtil.payload(params, Set.of("inspectionId", "wastageId"), Set.of()));
            ra.addFlashAttribute("flashSuccess", "Corrective action saved.");
            return "redirect:/quality/actions/" + saved.get("id");
        } catch (ApiException ex) {
            actionForm(model, params, ex.getMessage());
            return "generic/form";
        }
    }

    @GetMapping("/actions/{id}")
    public String actionView(@PathVariable Long id, Model model) {
        Map<String, Object> a = client.get("/api/v1/corrective-actions/{id}", id);
        String status = Opts.str(a.get("status"));
        List<Action> buttons = new ArrayList<>();
        if ("OPEN".equals(status)) {
            buttons.add(Action.post("Start work", "/quality/actions/" + id + "/status?value=IN_PROGRESS", "outline-primary", null));
        }
        if (!"CLOSED".equals(status)) {
            buttons.add(Action.post("Close action", "/quality/actions/" + id + "/status?value=CLOSED", "success", null));
        }
        Views.detail(model, "Corrective action #" + id, "quality", "/quality/actions", "All actions", tabs("actions"),
                Views.sections(a), buttons);
        return "generic/detail";
    }

    @PostMapping("/actions/{id}/status")
    public String actionStatus(@PathVariable Long id, @RequestParam String value, RedirectAttributes ra) {
        try {
            client.patch("/api/v1/corrective-actions/{id}/status?value={value}", id, value);
            ra.addFlashAttribute("flashSuccess", "Action is now " + value + ".");
        } catch (ApiException ex) {
            ra.addFlashAttribute("flashError", ex.getMessage());
        }
        return "redirect:/quality/actions/" + id;
    }
}
