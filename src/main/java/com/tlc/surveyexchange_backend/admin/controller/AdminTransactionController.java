package com.tlc.surveyexchange_backend.admin.controller;

import com.tlc.surveyexchange_backend.admin.service.AdminService;
import com.tlc.surveyexchange_backend.model.TransactionType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/admin/transactions")
@RequiredArgsConstructor
public class AdminTransactionController {

    private final AdminService adminService;

    @GetMapping
    public String list(
            @RequestParam(required = false) String type,
            @RequestParam(defaultValue = "0") int page,
            Model model) {
        TransactionType txType = null;
        if (type != null && !type.isBlank()) {
            try { txType = TransactionType.valueOf(type); } catch (IllegalArgumentException ignored) {}
        }
        model.addAttribute("transactions", adminService.getTransactions(txType, page, 20));
        model.addAttribute("type", type);
        model.addAttribute("types", TransactionType.values());
        return "admin/transactions/list";
    }
}
