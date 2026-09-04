package com.aifinance.controller;

import com.aifinance.dto.BatchDto;
import com.aifinance.dto.CsvUploadResponse;
import com.aifinance.service.BatchService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/batches")
public class BatchController {

    private final BatchService batchService;

    public BatchController(BatchService batchService) {
        this.batchService = batchService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'ANALYST')")
    public ResponseEntity<BatchDto> createBatch(@RequestBody(required = false) Map<String, String> payload) {
        String batchName = (payload != null) ? payload.get("batchName") : null;
        return ResponseEntity.ok(batchService.createBatch(batchName));
    }

    @PostMapping("/{id}/upload/{type}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ANALYST')")
    public ResponseEntity<CsvUploadResponse> uploadCsv(
            @PathVariable("id") Long batchId,
            @PathVariable("type") String type,
            @RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(batchService.uploadCsv(batchId, type, file));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'ANALYST', 'VIEWER')")
    public ResponseEntity<List<BatchDto>> getAllBatches() {
        return ResponseEntity.ok(batchService.getAllBatches());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ANALYST', 'VIEWER')")
    public ResponseEntity<BatchDto> getBatch(@PathVariable("id") Long batchId) {
        return ResponseEntity.ok(batchService.getBatch(batchId));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> deleteBatch(@PathVariable("id") Long batchId) {
        batchService.deleteBatch(batchId);
        return ResponseEntity.ok(Map.of("message", "Batch " + batchId + " deleted successfully"));
    }
}
