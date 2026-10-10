package com.foodsupply.webui.exception;

import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(DownstreamServiceException.class)
    public String handleDownstream(DownstreamServiceException ex, Model model, HttpServletResponse response) {
        log.warn("Downstream problem: {}", ex.getMessage());
        response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
        model.addAttribute("title", "Service unavailable");
        model.addAttribute("message", ex.getMessage());
        return "error/service-down";
    }

    @ExceptionHandler(ApiException.class)
    public String handleApi(ApiException ex, Model model, HttpServletResponse response) {
        log.warn("Backend rejected request ({}): {}", ex.getStatus(), ex.getMessage());
        response.setStatus(ex.getStatus());
        model.addAttribute("title", "Request could not be completed");
        model.addAttribute("message", ex.getMessage());
        return "error/service-down";
    }
}
