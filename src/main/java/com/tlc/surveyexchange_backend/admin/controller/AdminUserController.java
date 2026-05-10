package com.tlc.surveyexchange_backend.admin.controller;

import com.tlc.surveyexchange_backend.admin.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.UUID;

@Controller
@RequestMapping("/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final AdminService adminService;

    @GetMapping
    public String list(
            @RequestParam(defaultValue = "") String search,
            @RequestParam(defaultValue = "0") int page,
            Model model) {
        model.addAttribute("users",
                adminService.getUsers(search.isBlank() ? null : search, page, 20));
        model.addAttribute("search", search);
        return "admin/users/list";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable UUID id, Model model) {
        model.addAttribute("user", adminService.getUserById(id));
        model.addAttribute("surveyCount", adminService.getUserSurveyCount(id));
        model.addAttribute("responseCount", adminService.getUserResponseCount(id));
        model.addAttribute("transactions", adminService.getUserTransactions(id));
        return "admin/users/detail";
    }
}
