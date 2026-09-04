package com.aifinance.controller;

import com.aifinance.dto.AuditLogEntryDto;
import com.aifinance.dto.BatchMetricsDto;
import com.aifinance.entity.AuditLogEntry;
import com.aifinance.repository.AuditLogEntryRepository;
import com.aifinance.service.MetricsService;
import com.aifinance.service.ReportService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/batches/{batchId}")
public class ReportingAndMetricsController {

    private final MetricsService metricsService;
    private final ReportService reportService;
    private final AuditLogEntryRepository auditLogEntryRepository;

    public ReportingAndMetricsController(MetricsService metricsService,
                                        ReportService reportService,
                                        AuditLogEntryRepository auditLogEntryRepository) {
        this.metricsService = metricsService;
        this.reportService = reportService;
        this.auditLogEntryRepository = auditLogEntryRepository;
    }

    @GetMapping("/metrics")
    @PreAuthorize("hasAnyRole('ADMIN', 'ANALYST', 'VIEWER')")
    public ResponseEntity<BatchMetricsDto> getMetrics(@PathVariable("batchId") Long batchId) {
        return ResponseEntity.ok(metricsService.getBatchMetrics(batchId));
    }

    @GetMapping("/audit")
    @PreAuthorize("hasAnyRole('ADMIN', 'ANALYST', 'VIEWER')")
    public ResponseEntity<List<AuditLogEntryDto>> getAuditTrail(@PathVariable("batchId") Long batchId) {
        List<AuditLogEntry> entries = auditLogEntryRepository.findByBatchIdOrderByTimestampDesc(batchId);
        List<AuditLogEntryDto> dtos = entries.stream()
                .map(AuditLogEntryDto::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/report")
    @PreAuthorize("hasAnyRole('ADMIN', 'ANALYST', 'VIEWER')")
    public ResponseEntity<byte[]> downloadReport(@PathVariable("batchId") Long batchId) {
        byte[] excelBytes = reportService.generateExcelReport(batchId);

        String filename = "Reconciliation_Report_Batch_" + batchId + ".xlsx";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(excelBytes);
    }
}
