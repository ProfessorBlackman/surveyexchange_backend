package com.tlc.surveyexchange_backend.admin.controller;

import com.tlc.surveyexchange_backend.admin.service.AdminService;
import com.tlc.surveyexchange_backend.model.SurveyStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.UUID;

@Controller
@RequestMapping("/admin/surveys")
@RequiredArgsConstructor
public class AdminSurveyController {

    private final AdminService adminService;

    @GetMapping
    public String list(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "") String search,
            @RequestParam(defaultValue = "0") int page,
            Model model) {
        SurveyStatus surveyStatus = null;
        if (status != null && !status.isBlank()) {
            try { surveyStatus = SurveyStatus.valueOf(status); } catch (IllegalArgumentException ignored) {}
        }
        model.addAttribute("surveys",
                adminService.getSurveys(surveyStatus, search.isBlank() ? null : search, page, 20));
        model.addAttribute("status", status);
        model.addAttribute("search", search);
        model.addAttribute("statuses", SurveyStatus.values());
        return "admin/surveys/list";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable UUID id, Model model) {
        model.addAttribute("survey", adminService.getSurveyById(id));
        return "admin/surveys/detail";
    }

    @PostMapping("/{id}/disable")
    public String disable(@PathVariable UUID id, RedirectAttributes ra) {
        adminService.disableSurvey(id);
        ra.addFlashAttribute("successMsg", "Survey has been closed.");
        return "redirect:/admin/surveys";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable UUID id, RedirectAttributes ra) {
        adminService.deleteSurvey(id);
        ra.addFlashAttribute("successMsg", "Survey has been deleted.");
        return "redirect:/admin/surveys";
    }
}
