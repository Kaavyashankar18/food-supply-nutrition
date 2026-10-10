package com.foodsupply.webui.controller;

import com.foodsupply.webui.client.DistributionClient;
import com.foodsupply.webui.client.ProgrammeClient;
import com.foodsupply.webui.dto.Action;
import com.foodsupply.webui.dto.Field;
import com.foodsupply.webui.dto.Option;
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

@Controller
@RequestMapping("/distribution")
@RequiredArgsConstructor
public class DistributionController {

    private static final String PLANS = "/api/v1/distribution-plans";

    private final DistributionClient client;
    private final ProgrammeClient programmes;

    private List<Tab> tabs(String active) {
        return List.of(new Tab("Plans", "/distribution", "plans".equals(active)),
                new Tab("Centres", "/distribution/centres", "centres".equals(active)),
                new Tab("Delivery records", "/distribution/records", "records".equals(active)));
    }

    @GetMapping
    public String plans(@RequestParam(required = false) String q, @RequestParam(required = false) String status,
                        @RequestParam(defaultValue = "0") int page, Model model) {
        Map<String, Object> result = client.page(PLANS, Math.max(page, 0), 20, "q", q, "status", status);
        List<Map<String, Object>> rows = Views.rows(result);
        List<Field> filters = List.of(Field.text("q", "Search", q),
                Field.select("status", "Status", status, Opts.of("Any status", "PLANNED", "RESERVED", "DISPATCHED",
                        "DELIVERED", "PARTIAL", "FAILED", "CANCELLED")));
        Map<String, Object> summary = client.get(PLANS + "/summary");
        Views.list(model, "Distribution plans", "distribution", tabs("plans"), filters, "/distribution", result, rows,
                Views.columns(rows, "planNumber", "programmeName", "centreName", "startDate", "endDate", "status"),
                "/distribution/plans", "id", "/distribution/plans/new", "New distribution plan",
                FormUtil.listUrl("/distribution", "q", q, "status", status),
                Views.cards(summary, "total", "planned", "reserved", "dispatched", "delivered"));
        return "generic/list";
    }

    @GetMapping("/centres")
    public String centres(@RequestParam(defaultValue = "0") int page, Model model) {
        Map<String, Object> result = client.page("/api/v1/distribution-centres", Math.max(page, 0), 50);
        List<Map<String, Object>> rows = Views.rows(result);
        Views.list(model, "Distribution centres", "distribution", tabs("centres"), List.of(), "/distribution/centres",
                result, rows, Views.columns(rows, "name", "address", "contactPerson", "phone", "status"),
                null, "id", null, null, "/distribution/centres", null);
        return "generic/list";
    }

    @GetMapping("/records")
    public String records(@RequestParam(required = false) String beneficiaryId, @RequestParam(required = false) String planId,
                          @RequestParam(required = false) String status, @RequestParam(defaultValue = "0") int page,
                          Model model) {
        Map<String, Object> result = client.page("/api/v1/distribution-records", Math.max(page, 0), 20,
                "beneficiaryId", beneficiaryId, "planId", planId, "status", status);
        List<Map<String, Object>> rows = Views.rows(result);
        List<Field> filters = List.of(Field.text("beneficiaryId", "Beneficiary id", beneficiaryId),
                Field.text("planId", "Plan id", planId), Field.text("status", "Status", status));
        Views.list(model, "Delivery records", "distribution", tabs("records"), filters, "/distribution/records", result,
                rows, Views.columns(rows, "planId", "beneficiaryId", "beneficiaryName", "foodItemName", "quantity",
                        "deliveredOn", "status"),
                null, "id", null, null,
                FormUtil.listUrl("/distribution/records", "beneficiaryId", beneficiaryId, "planId", planId, "status", status),
                null);
        return "generic/list";
    }

    // ---------- new plan: preview, create, raise procurement ----------

    @GetMapping("/plans/new")
    public String newPlan(@RequestParam(required = false) String programmeId, @RequestParam(required = false) String startDate,
                          @RequestParam(required = false) String endDate, Model model) {
        Map<String, String> v = new LinkedHashMap<>();
        v.put("programmeId", programmeId);
        v.put("startDate", Opts.blank(startDate) ? LocalDate.now().toString() : startDate);
        v.put("endDate", Opts.blank(endDate) ? LocalDate.now().plusDays(6).toString() : endDate);
        showPlanForm(model, v, null, null, null);
        return "distribution/plan-form";
    }

    private void showPlanForm(Model model, Map<String, String> v, String error, String info, Map<String, Object> result) {
        List<Map<String, Object>> progs = Views.rows(programmes.page("/api/v1/programmes", 0, 100));
        List<Map<String, Object>> centres = client.items("/api/v1/distribution-centres?size=100");
        List<Option> centreOptions = Opts.from(centres, null, "id", "name");
        model.addAttribute("title", "New distribution plan");
        model.addAttribute("active", "distribution");
        model.addAttribute("programmeOptions", Opts.from(progs, null, "id", "name"));
        model.addAttribute("centreOptions", centreOptions);
        model.addAttribute("v", v);
        model.addAttribute("formError", error);
        model.addAttribute("info", info);
        model.addAttribute("sections", result == null ? new ArrayList<Object>() : Views.sections(result));
    }

    private Map<String, Object> body(Map<String, String> p) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("programmeId", Opts.toLong("programmeId", p.getOrDefault("programmeId", "")));
        body.put("centreId", Opts.toLong("centreId", p.getOrDefault("centreId", "")));
        body.put("startDate", p.get("startDate"));
        body.put("endDate", p.get("endDate"));
        if (!Opts.blank(p.get("remarks"))) {
            body.put("remarks", p.get("remarks"));
        }
        return body;
    }

    @PostMapping("/plans/preview")
    public String preview(@RequestParam Map<String, String> p, Model model) {
        try {
            Map<String, Object> result = client.post(PLANS + "/generate", body(p));
            showPlanForm(model, p, null, "Preview only - nothing has been saved or reserved.", result);
        } catch (ApiException ex) {
            showPlanForm(model, p, ex.getMessage(), null, null);
        }
        return "distribution/plan-form";
    }

    @PostMapping("/plans/create")
    public String create(@RequestParam Map<String, String> p, Model model, RedirectAttributes ra) {
        try {
            Map<String, Object> saved = client.post(PLANS, body(p));
            ra.addFlashAttribute("flashSuccess", "Plan created and stock reserved.");
            return "redirect:/distribution/plans/" + saved.get("id");
        } catch (ApiException ex) {
            showPlanForm(model, p, ex.getMessage(), null, null);
            return "distribution/plan-form";
        }
    }

    @PostMapping("/plans/raise-procurement")
    public String raise(@RequestParam Map<String, String> p, Model model) {
        try {
            Map<String, Object> result = client.post(PLANS + "/raise-procurement", body(p));
            showPlanForm(model, p, null, "Shortage procurement request raised. Approve it under Procurement.", result);
        } catch (ApiException ex) {
            showPlanForm(model, p, ex.getMessage(), null, null);
        }
        return "distribution/plan-form";
    }

    // ---------- plan view and workflow ----------

    @GetMapping("/plans/{id}")
    public String view(@PathVariable Long id, Model model) {
        Map<String, Object> plan = client.get(PLANS + "/{id}", id);
        String status = Opts.str(plan.get("status"));
        List<Action> actions = new ArrayList<>();
        if ("RESERVED".equals(status) || "PLANNED".equals(status)) {
            actions.add(Action.post("Dispatch", "/distribution/plans/" + id + "/dispatch", "primary", "Dispatch this plan?"));
            actions.add(Action.post("Cancel plan", "/distribution/plans/" + id + "/cancel", "outline-danger",
                    "Cancel the plan and release the reserved stock?"));
        }
        if ("DISPATCHED".equals(status)) {
            actions.add(Action.post("Confirm full delivery", "/distribution/plans/" + id + "/deliver", "success",
                    "Confirm that everything was delivered?"));
        }
        Views.detail(model, "Plan " + Opts.str(plan.get("planNumber")), "distribution", "/distribution", "All plans",
                tabs("plans"), Views.sections(plan), actions);
        return "generic/detail";
    }

    @PostMapping("/plans/{id}/{action}")
    public String workflow(@PathVariable Long id, @PathVariable String action, RedirectAttributes ra) {
        try {
            if (!List.of("dispatch", "deliver", "cancel").contains(action)) {
                throw new ApiException(400, "Unknown action");
            }
            client.post(PLANS + "/{id}/" + action, null, id);
            ra.addFlashAttribute("flashSuccess", Map.of("dispatch", "Plan dispatched.", "deliver",
                    "Delivery confirmed.", "cancel", "Plan cancelled and stock released.").get(action));
        } catch (ApiException ex) {
            ra.addFlashAttribute("flashError", ex.getMessage());
        }
        return "redirect:/distribution/plans/" + id;
    }
}
