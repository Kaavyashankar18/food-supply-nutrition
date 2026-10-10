package com.foodsupply.webui.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DemoController {

    @GetMapping("/demo")
    public String demo(Model model) {
        model.addAttribute("title", "Demo walkthrough");
        model.addAttribute("active", "demo");
        return "demo/index";
    }
}
