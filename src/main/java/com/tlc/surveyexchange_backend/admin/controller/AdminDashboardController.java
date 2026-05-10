package com.tlc.surveyexchange_backend.admin.controller;

import com.tlc.surveyexchange_backend.admin.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminDashboardController {

    private final AdminService adminService;

    @GetMapping("/login")
    public String loginPage() {
        return "admin/login";
    }

    @GetMapping({"", "/"})
    public String dashboard(Model model) {
        model.addAttribute("stats", adminService.getDashboardStats());

        List<AdminService.DayCount> dayCounts = adminService.getResponsesLast7Days();
        model.addAttribute("chartLabels",
                dayCounts.stream().map(dc -> dc.date().toString()).toList());
        model.addAttribute("chartValues",
                dayCounts.stream().map(AdminService.DayCount::count).toList());

        return "admin/dashboard";
    }
}
