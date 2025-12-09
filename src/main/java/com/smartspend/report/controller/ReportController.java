package com.smartspend.report.controller;

import com.smartspend.auth.security.JwtUtils;
import com.smartspend.report.dto.MonthlySummaryDto;
import com.smartspend.report.service.ReportService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/api/reports")
@CrossOrigin("*")
public class ReportController {

    @Autowired
    private ReportService reportService;

    @Autowired
    private JwtUtils jwtUtils;

    // ==============================
    // Extract userId safely from JWT
    // ==============================
    private Long extractUserId(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            Long userId = jwtUtils.extractUserId(token);
            if (userId != null) return userId;
        }
        throw new RuntimeException("Unauthorized! Invalid or expired token.");
    }

    // ==============================
    // Dashboard Summary (for cards)
    // GET /api/reports/summary
    // ==============================
    @GetMapping("/summary")
    public ResponseEntity<?> getDashboardSummary(HttpServletRequest request) {
        Long userId = extractUserId(request);
        Map<String, Double> summary = reportService.getDashboardSummary(userId);
        return ResponseEntity.ok(summary);
    }

    // ==============================
    // Raw Expense Reports (JSON)
    // ==============================
    @GetMapping("/expenses")
    public ResponseEntity<?> getExpensesReport(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            HttpServletRequest request) {

        Long userId = extractUserId(request);
        LocalDate start = startDate != null ? LocalDate.parse(startDate) : null;
        LocalDate end = endDate != null ? LocalDate.parse(endDate) : null;

        return ResponseEntity.ok(reportService.getExpensesReport(userId, category, start, end));
    }

    // ==============================
    // PDF Download
    // ==============================
    @GetMapping("/pdf")
    public ResponseEntity<byte[]> downloadPdf(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            HttpServletRequest request) throws IOException {

        Long userId = extractUserId(request);
        LocalDate start = startDate != null ? LocalDate.parse(startDate) : null;
        LocalDate end = endDate != null ? LocalDate.parse(endDate) : null;

        ByteArrayInputStream bis = reportService.generatePdfReport(userId, category, start, end);

        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Disposition", "inline; filename=expenses_report.pdf");

        return ResponseEntity.ok()
                .headers(headers)
                .contentType(MediaType.APPLICATION_PDF)
                .body(bis.readAllBytes());
    }

    // ==============================
    // Excel Download
    // ==============================
    @GetMapping("/excel")
    public ResponseEntity<byte[]> downloadExcel(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            HttpServletRequest request) throws IOException {

        Long userId = extractUserId(request);
        LocalDate start = startDate != null ? LocalDate.parse(startDate) : null;
        LocalDate end = endDate != null ? LocalDate.parse(endDate) : null;

        ByteArrayInputStream bis = reportService.generateExcelReport(userId, category, start, end);

        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Disposition", "attachment; filename=expenses_report.xlsx");

        return ResponseEntity.ok()
                .headers(headers)
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(bis.readAllBytes());
    }

    // ==============================
    // Monthly Summary & Statistics
    // ==============================
    @GetMapping("/monthly-summary")
    public ResponseEntity<MonthlySummaryDto> getMonthlySummary(
            @RequestParam int year,
            @RequestParam int month,
            HttpServletRequest request) {

        Long userId = extractUserId(request);
        return ResponseEntity.ok(reportService.getMonthlySummary(userId, year, month));
    }

    // ==============================
    // Dashboard Summary APIs
    // ==============================
    @GetMapping("/dashboard/category-summary")
    public ResponseEntity<?> getCategorySummary(
            @RequestParam int year,
            @RequestParam int month,
            HttpServletRequest request) {

        Long userId = extractUserId(request);
        return ResponseEntity.ok(reportService.getCategorySummary(userId, year, month));
    }

    @GetMapping("/dashboard/income-expense-summary")
    public ResponseEntity<?> getIncomeExpenseSummary(
            @RequestParam int year,
            @RequestParam int month,
            HttpServletRequest request) {

        Long userId = extractUserId(request);
        return ResponseEntity.ok(reportService.getIncomeExpenseSummary(userId, year, month));
    }

    @GetMapping("/dashboard/monthly-trend")
    public ResponseEntity<?> getMonthlyTrend(
            @RequestParam int year,
            HttpServletRequest request) {

        Long userId = extractUserId(request);
        return ResponseEntity.ok(reportService.getMonthlyTrend(userId, year));
    }
}
