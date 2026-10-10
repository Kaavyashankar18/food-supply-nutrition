package com.foodsupply.webui.controller;

import com.foodsupply.webui.client.BeneficiaryClient;
import com.foodsupply.webui.client.ProgrammeClient;
import com.foodsupply.webui.dto.Action;
import com.foodsupply.webui.dto.Field;
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
@RequestMapping("/programme")
@RequiredArgsConstructor
public class ProgrammeController {

    private static final String API = "/api/v1/programmes";
    private static final String[] CATEGORIES = {"CHILD", "STUDENT", "PREGNANT_WOMAN", "LACTATING_MOTHER", "ELDERLY", "PATIENT"};

    private final ProgrammeClient client;
    private final BeneficiaryClient beneficiaries;

    @GetMapping
    public String list(@RequestParam(required = false) String q, @RequestParam(required = false) String status,
                       @RequestParam(required = false) String targetCategory,
                       @RequestParam(defaultValue = "0") int page, Model model) {
        Map<String, Object> result = client.page(API, Math.max(page, 0), 20, "q", q, "status", status,
                "targetCategory", targetCategory);
        List<Map<String, Object>> rows = Views.rows(result);
        List<Field> filters = List.of(Field.text("q", "Search", q),
                Field.select("status", "Status", status, Opts.of("Any status", "DRAFT", "ACTIVE", "COMPLETED", "CANCELLED")),
                Field.select("targetCategory", "Target", targetCategory, Opts.of("Any category", CATEGORIES)));
        Views.list(model, "Nutrition programmes", "programme", null, filters, "/programme", result, rows,
                Views.columns(rows, "id", "name", "type", "targetCategory", "status", "startDate", "endDate"),
                "/programme", "id", "/programme/new", "New programme",
                FormUtil.listUrl("/programme", "q", q, "status", status, "targetCategory", targetCategory), null);
        return "generic/list";
    }

    private List<Field> fields(Map<String, ?> v) {
        return List.of(Field.hidden("id", Opts.str(v.get("id"))),
                Field.text("name", "Programme name", Opts.str(v.get("name"))).req(),
                Field.text("type", "Type", Opts.str(v.get("type"))).req().hint("e.g. SCHOOL_MEAL"),
                Field.select("targetCategory", "Target category", Opts.str(v.get("targetCategory")), Opts.of(null, CATEGORIES)),
                Field.select("status", "Status", Opts.str(v.get("status")).isEmpty() ? "DRAFT" : Opts.str(v.get("status")),
                        Opts.of(null, "DRAFT", "ACTIVE", "COMPLETED", "CANCELLED")),
                Field.date("startDate", "Start date", Opts.str(v.get("startDate"))).req(),
                Field.date("endDate", "End date", Opts.str(v.get("endDate"))).req(),
                Field.area("objectives", "Objectives", Opts.str(v.get("objectives"))));
    }

    @GetMapping("/new")
    public String create(Model model) {
        Views.form(model, "New programme", "programme", "/programme", "/programme/save", fields(Map.of()), null, "Save programme");
        return "generic/form";
    }

    @GetMapping("/{id}/edit")
    public String edit(@PathVariable Long id, Model model) {
        Views.form(model, "Edit programme", "programme", "/programme/" + id, "/programme/save",
                fields(client.get(API + "/{id}", id)), null, "Save programme");
        return "generic/form";
    }

    @PostMapping("/save")
    public String save(@RequestParam Map<String, String> params, Model model, RedirectAttributes ra) {
        String id = params.get("id");
        try {
            Map<String, Object> body = FormUtil.payload(params, Set.of(), Set.of());
            Map<String, Object> saved = Opts.blank(id) ? client.post(API, body) : client.put(API + "/{id}", body, id);
            ra.addFlashAttribute("flashSuccess", "Programme saved.");
            return "redirect:/programme/" + saved.get("id");
        } catch (ApiException ex) {
            Views.form(model, Opts.blank(id) ? "New programme" : "Edit programme", "programme", "/programme",
                    "/programme/save", fields(params), ex.getMessage(), "Save programme");
            return "generic/form";
        }
    }

    @GetMapping("/{id}")
    public String view(@PathVariable Long id, Model model) {
        Map<String, Object> p = client.get(API + "/{id}", id);
        Map<String, Object> all = new LinkedHashMap<>(p);
        all.put("participants", client.items(API + "/" + id + "/participants"));
        all.put("mealPlans", client.items(API + "/" + id + "/meal-plans"));
        String from = LocalDate.now().toString();
        String to = LocalDate.now().plusDays(6).toString();
        List<Action> actions = new ArrayList<>();
        actions.add(Action.link("Total food required", "/programme/" + id + "/requirements?from=" + from + "&to=" + to, "primary"));
        actions.add(Action.link("Enrol beneficiary", "/programme/" + id + "/enrol", "outline-primary"));
        actions.add(Action.link("Plan distribution", "/distribution/plans/new?programmeId=" + id, "outline-success"));
        actions.add(Action.link("Edit", "/programme/" + id + "/edit", "outline-secondary"));
        if (!"ACTIVE".equals(Opts.str(p.get("status")))) {
            actions.add(Action.post("Activate", "/programme/" + id + "/status?value=ACTIVE", "outline-secondary", null));
        }
        if (!"COMPLETED".equals(Opts.str(p.get("status")))) {
            actions.add(Action.post("Mark completed", "/programme/" + id + "/status?value=COMPLETED", "outline-secondary",
                    "Mark this programme as completed?"));
        }
        Views.detail(model, Opts.str(p.get("name")), "programme", "/programme", "All programmes", null,
                Views.sections(all), actions);
        return "generic/detail";
    }

    @PostMapping("/{id}/status")
    public String status(@PathVariable Long id, @RequestParam String value, RedirectAttributes ra) {
        try {
            client.patch(API + "/{id}/status?value={value}", id, value);
            ra.addFlashAttribute("flashSuccess", "Programme is now " + value + ".");
        } catch (ApiException ex) {
            ra.addFlashAttribute("flashError", ex.getMessage());
        }
        return "redirect:/programme/" + id;
    }

    @GetMapping("/{id}/requirements")
    public String requirements(@PathVariable Long id, @RequestParam(required = false) String from,
                               @RequestParam(required = false) String to, Model model) {
        String f = Opts.blank(from) ? LocalDate.now().toString() : from;
        String t = Opts.blank(to) ? LocalDate.now().plusDays(6).toString() : to;
        Map<String, Object> req = client.get(API + "/{id}/requirements?from={from}&to={to}", id, f, t);
        Views.detail(model, "Food required: " + f + " to " + t, "programme", "/programme/" + id, "Programme", null,
                Views.sections(req), List.of(Action.link("Plan distribution", "/distribution/plans/new?programmeId=" + id
                        + "&startDate=" + f + "&endDate=" + t, "success")));
        model.addAttribute("rangeUrl", "/programme/" + id + "/requirements");
        model.addAttribute("rangeFrom", f);
        model.addAttribute("rangeTo", t);
        return "generic/detail";
    }

    @GetMapping("/{id}/enrol")
    public String enrolForm(@PathVariable Long id, Model model) {
        showEnrol(model, id, null);
        return "generic/form";
    }

    private void showEnrol(Model model, Long id, String error) {
        List<Map<String, Object>> people = new ArrayList<>();
        for (String type : new String[]{"SCHOOL", "ORPHANAGE", "COMMUNITY_CENTRE"}) {
            people.addAll(Views.rows(beneficiaries.list(null, null, type, "ACTIVE", 0, 200)));
        }
        List<Map<String, Object>> labelled = new ArrayList<>();
        for (Map<String, Object> b : people) {
            Map<String, Object> copy = new LinkedHashMap<>(b);
            copy.put("label", Opts.str(b.get("fullName")) + " (" + Opts.str(b.get("peopleServed")) + " people)");
            labelled.add(copy);
        }
        Views.form(model, "Enrol a beneficiary", "programme", "/programme/" + id, "/programme/" + id + "/enrol",
                List.of(Field.select("beneficiaryId", "Institution", "", Opts.from(labelled, null, "id", "label")).req()),
                error, "Enrol");
    }

    @PostMapping("/{id}/enrol")
    public String enrol(@PathVariable Long id, @RequestParam String beneficiaryId, Model model, RedirectAttributes ra) {
        try {
            client.post(API + "/{id}/participants", Map.of("beneficiaryId", Opts.toLong("beneficiaryId", beneficiaryId)), id);
            ra.addFlashAttribute("flashSuccess", "Beneficiary enrolled.");
            return "redirect:/programme/" + id;
        } catch (ApiException ex) {
            showEnrol(model, id, ex.getMessage());
            return "generic/form";
        }
    }
}
