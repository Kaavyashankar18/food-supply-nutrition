package com.foodsupply.webui.controller;

import com.foodsupply.webui.client.BeneficiaryClient;
import com.foodsupply.webui.exception.ApiException;
import com.foodsupply.webui.util.FormUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Controller
@RequestMapping("/beneficiary")
@RequiredArgsConstructor
public class BeneficiaryController {

    private static final List<String> TYPES =
            List.of("INDIVIDUAL", "FAMILY", "SCHOOL", "ORPHANAGE", "COMMUNITY_CENTRE");
    private static final List<String> CATEGORIES =
            List.of("CHILD", "STUDENT", "PREGNANT_WOMAN", "LACTATING_MOTHER", "ELDERLY", "PATIENT");
    private static final Set<String> LONG_FIELDS = Set.of("peopleServed", "age", "householdSize");

    private final BeneficiaryClient client;

    @GetMapping
    public String list(@RequestParam(required = false) String q,
                       @RequestParam(required = false) String category,
                       @RequestParam(required = false) String type,
                       @RequestParam(required = false) String status,
                       @RequestParam(defaultValue = "0") int page,
                       Model model) {
        Map<String, Object> result = client.list(q, category, type, status, Math.max(page, 0), 20);
        model.addAttribute("page", result);
        model.addAttribute("rows", result.get("content"));
        model.addAttribute("q", q);
        model.addAttribute("category", category);
        model.addAttribute("type", type);
        model.addAttribute("status", status);
        model.addAttribute("types", TYPES);
        model.addAttribute("categories", CATEGORIES);
        model.addAttribute("base", FormUtil.listUrl("/beneficiary",
                "q", q, "category", category, "type", type, "status", status));
        return "beneficiary/list";
    }

    @GetMapping("/new")
    public String create(Model model) {
        showForm(model, new HashMap<String, Object>(), null);
        return "beneficiary/form";
    }

    @GetMapping("/{id}")
    public String view(@PathVariable Long id, Model model) {
        model.addAttribute("b", client.get(id));
        return "beneficiary/view";
    }

    @GetMapping("/{id}/edit")
    public String edit(@PathVariable Long id, Model model) {
        showForm(model, client.get(id), null);
        return "beneficiary/form";
    }

    @PostMapping("/save")
    public String save(@RequestParam Map<String, String> params, Model model, RedirectAttributes ra) {
        String id = params.get("id");
        try {
            Map<String, Object> body = FormUtil.payload(params, LONG_FIELDS, Set.of());
            Map<String, Object> saved = (id == null || id.isBlank())
                    ? client.create(body)
                    : client.update(Long.valueOf(id), body);
            ra.addFlashAttribute("flashSuccess", "Beneficiary saved.");
            return "redirect:/beneficiary/" + saved.get("id");
        } catch (ApiException ex) {
            Map<String, Object> entered = new HashMap<String, Object>(params);
            showForm(model, entered, ex.getMessage());
            return "beneficiary/form";
        }
    }

    @PostMapping("/{id}/verify")
    public String verify(@PathVariable Long id, RedirectAttributes ra) {
        return act(() -> client.verify(id), "Eligibility checked and saved.", ra, "/beneficiary/" + id);
    }

    @PostMapping("/{id}/status")
    public String status(@PathVariable Long id, @RequestParam String value, RedirectAttributes ra) {
        return act(() -> client.setStatus(id, value), "Status changed to " + value + ".", ra, "/beneficiary/" + id);
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes ra) {
        return act(() -> client.delete(id), "Beneficiary deleted.", ra, "/beneficiary");
    }

    private String act(Runnable action, String okMessage, RedirectAttributes ra, String target) {
        try {
            action.run();
            ra.addFlashAttribute("flashSuccess", okMessage);
        } catch (ApiException ex) {
            ra.addFlashAttribute("flashError", ex.getMessage());
        }
        return "redirect:" + target;
    }

    private void showForm(Model model, Map<String, Object> b, String error) {
        model.addAttribute("b", b);
        model.addAttribute("formError", error);
        model.addAttribute("genders", List.of("MALE", "FEMALE", "OTHER"));
        model.addAttribute("types", TYPES);
        model.addAttribute("categories", CATEGORIES);
    }
}
