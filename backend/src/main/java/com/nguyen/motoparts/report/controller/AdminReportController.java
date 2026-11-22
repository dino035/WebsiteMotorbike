package com.nguyen.motoparts.report.controller;

import com.nguyen.motoparts.report.dto.SalesReportResponse;
import com.nguyen.motoparts.report.service.SalesReportService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/** Báo cáo doanh thu cho Admin (UC-23). Chỉ ADMIN (SecurityConfig chặn /api/admin/**). */
@RestController
@RequestMapping("/api/admin/reports")
public class AdminReportController {

    private final SalesReportService reports;

    public AdminReportController(SalesReportService reports) {
        this.reports = reports;
    }

    @GetMapping("/sales")
    public SalesReportResponse sales(
            @RequestParam(name = "from", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(name = "to", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return reports.sales(from, to);
    }
}