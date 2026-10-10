package com.foodsupply.webui.controller;

import com.foodsupply.webui.client.FoodClient;
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
@RequestMapping("/food")
@RequiredArgsConstructor
public class FoodController {

    private static final List<String> UNITS = List.of("KG", "LITRE", "PIECE");
    private static final Set<String> LONG_FIELDS = Set.of("categoryId");
    private static final Set<String> DECIMAL_FIELDS = Set.of("gramsPerUnit", "caloriesPer100g", "proteinPer100g",
            "carbsPer100g", "fatPer100g", "fibrePer100g", "recommendedQtyPerPersonPerDay");

    private final FoodClient client;

    @GetMapping
    public String list(@RequestParam(required = false) String q,
                       @RequestParam(required = false) String categoryId,
                       @RequestParam(required = false) String unit,
                       @RequestParam(required = false) String active,
                       @RequestParam(defaultValue = "0") int page,
                       Model model) {
        Map<String, Object> result = client.list(q, categoryId, unit, active, Math.max(page, 0), 20);
        model.addAttribute("page", result);
        model.addAttribute("rows", result.get("content"));
        model.addAttribute("q", q);
        model.addAttribute("categoryId", categoryId);
        model.addAttribute("unit", unit);
        model.addAttribute("active", active);
        model.addAttribute("units", UNITS);
        model.addAttribute("categories", client.categories());
        model.addAttribute("base", FormUtil.listUrl("/food",
                "q", q, "categoryId", categoryId, "unit", unit, "active", active));
        return "food/list";
    }

    @GetMapping("/new")
    public String create(Model model) {
        showForm(model, new HashMap<String, Object>(), null);
        return "food/form";
    }

    @GetMapping("/{id}")
    public String view(@PathVariable Long id, Model model) {
        model.addAttribute("f", client.get(id));
        return "food/view";
    }

    @GetMapping("/{id}/edit")
    public String edit(@PathVariable Long id, Model model) {
        showForm(model, client.get(id), null);
        return "food/form";
    }

    @PostMapping("/save")
    public String save(@RequestParam Map<String, String> params, Model model, RedirectAttributes ra) {
        String id = params.get("id");
        try {
            Map<String, Object> body = FormUtil.payload(params, LONG_FIELDS, DECIMAL_FIELDS);
            Map<String, Object> saved = (id == null || id.isBlank())
                    ? client.create(body)
                    : client.update(Long.valueOf(id), body);
            ra.addFlashAttribute("flashSuccess", "Food item saved.");
            return "redirect:/food/" + saved.get("id");
        } catch (ApiException ex) {
            Map<String, Object> entered = new HashMap<String, Object>(params);
            showForm(model, entered, ex.getMessage());
            return "food/form";
        }
    }

    @PostMapping("/{id}/active")
    public String active(@PathVariable Long id, @RequestParam boolean value, RedirectAttributes ra) {
        try {
            client.setActive(id, value);
            ra.addFlashAttribute("flashSuccess", value ? "Item activated." : "Item deactivated.");
        } catch (ApiException ex) {
            ra.addFlashAttribute("flashError", ex.getMessage());
        }
        return "redirect:/food/" + id;
    }

    @GetMapping("/categories")
    public String categories(Model model) {
        model.addAttribute("rows", client.categories());
        return "food/categories";
    }

    @PostMapping("/categories")
    public String addCategory(@RequestParam String name, RedirectAttributes ra) {
        try {
            client.createCategory(name.trim());
            ra.addFlashAttribute("flashSuccess", "Category added.");
        } catch (ApiException ex) {
            ra.addFlashAttribute("flashError", ex.getMessage());
        }
        return "redirect:/food/categories";
    }

    @PostMapping("/categories/{id}/delete")
    public String deleteCategory(@PathVariable Long id, RedirectAttributes ra) {
        try {
            client.deleteCategory(id);
            ra.addFlashAttribute("flashSuccess", "Category deleted.");
        } catch (ApiException ex) {
            ra.addFlashAttribute("flashError", ex.getMessage());
        }
        return "redirect:/food/categories";
    }

    private void showForm(Model model, Map<String, Object> f, String error) {
        model.addAttribute("f", f);
        model.addAttribute("formError", error);
        model.addAttribute("units", UNITS);
        model.addAttribute("categories", client.categories());
    }
}
