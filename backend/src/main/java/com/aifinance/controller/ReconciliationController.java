package com.aifinance.controller;

import com.aifinance.dto.ManualReviewActionRequest;
import com.aifinance.dto.MatchResultDto;
import com.aifinance.entity.*;
import com.aifinance.repository.*;
import com.aifinance.service.AiReconciliationService;
import com.aifinance.service.RuleMatchingService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/batches/{batchId}")
public class ReconciliationController {

    private final RuleMatchingService ruleMatchingService;
    private final AiReconciliationService aiReconciliationService;
    private final MatchResultRepository matchResultRepository;
    private final BankRecordRepository bankRecordRepository;
    private final LedgerRecordRepository ledgerRecordRepository;
    private final AuditLogEntryRepository auditLogEntryRepository;
    private final BatchRepository batchRepository;

    public ReconciliationController(RuleMatchingService ruleMatchingService,
                                    AiReconciliationService aiReconciliationService,
                                    MatchResultRepository matchResultRepository,
                                    BankRecordRepository bankRecordRepository,
                                    LedgerRecordRepository ledgerRecordRepository,
                                    AuditLogEntryRepository auditLogEntryRepository,
                                    BatchRepository batchRepository) {
        this.ruleMatchingService = ruleMatchingService;
        this.aiReconciliationService = aiReconciliationService;
        this.matchResultRepository = matchResultRepository;
        this.bankRecordRepository = bankRecordRepository;
        this.ledgerRecordRepository = ledgerRecordRepository;
        this.auditLogEntryRepository = auditLogEntryRepository;
        this.batchRepository = batchRepository;
    }

    @PostMapping("/reconcile/rules")
    @PreAuthorize("hasAnyRole('ADMIN', 'ANALYST')")
    public ResponseEntity<Map<String, Object>> reconcilePass1(@PathVariable("batchId") Long batchId) {
        List<MatchResult> results = ruleMatchingService.reconcilePass1(batchId);
        long matched = results.stream().filter(r -> r.getMatchStatus() == MatchStatus.MATCHED).count();
        long needsReview = results.size() - matched;

        return ResponseEntity.ok(Map.of(
                "batchId", batchId,
                "pass", "PASS1_RULES",
                "totalEvaluated", results.size(),
                "matchedCount", matched,
                "needsReviewCount", needsReview,
                "message", "Pass 1 Deterministic Rules completed successfully."
        ));
    }

    @PostMapping("/reconcile/ai")
    @PreAuthorize("hasAnyRole('ADMIN', 'ANALYST')")
    public ResponseEntity<Map<String, Object>> reconcilePass2(@PathVariable("batchId") Long batchId) {
        List<MatchResult> results = aiReconciliationService.reconcilePass2(batchId);
        long newlyMatched = results.stream().filter(r -> r.getMatchStatus() == MatchStatus.MATCHED).count();
        long manualReview = results.stream().filter(r -> r.getMatchStatus() == MatchStatus.MANUAL_REVIEW).count();

        return ResponseEntity.ok(Map.of(
                "batchId", batchId,
                "pass", "PASS2_AI",
                "aiEvaluatedCount", results.size(),
                "aiMatchedCount", newlyMatched,
                "manualReviewCount", manualReview,
                "message", "Pass 2 AI Classification completed successfully."
        ));
    }

    @GetMapping("/records")
    @PreAuthorize("hasAnyRole('ADMIN', 'ANALYST', 'VIEWER')")
    public ResponseEntity<List<MatchResultDto>> getRecords(
            @PathVariable("batchId") Long batchId,
            @RequestParam(name = "status", required = false) String statusStr,
            @RequestParam(name = "search", required = false) String search) {

        List<MatchResult> list = matchResultRepository.findByBatchId(batchId);

        if (statusStr != null && !statusStr.trim().isEmpty() && !statusStr.equalsIgnoreCase("ALL")) {
            try {
                MatchStatus filterStatus = MatchStatus.valueOf(statusStr.toUpperCase().trim());
                list = list.stream().filter(m -> m.getMatchStatus() == filterStatus).toList();
            } catch (IllegalArgumentException ignored) {}
        }

        if (search != null && !search.trim().isEmpty()) {
            String q = search.toLowerCase().trim();
            list = list.stream().filter(m -> {
                boolean gwMatch = m.getGatewayRecord() != null &&
                        (m.getGatewayRecord().getTransactionId().toLowerCase().contains(q) ||
                         (m.getGatewayRecord().getOrderId() != null && m.getGatewayRecord().getOrderId().toLowerCase().contains(q)) ||
                         (m.getGatewayRecord().getCustomerEmail() != null && m.getGatewayRecord().getCustomerEmail().toLowerCase().contains(q)));
                boolean bankMatch = m.getBankRecord() != null &&
                        (m.getBankRecord().getBankRefId().toLowerCase().contains(q) ||
                         m.getBankRecord().getNarration().toLowerCase().contains(q));
                boolean ruleMatch = m.getRuleFired() != null && m.getRuleFired().toLowerCase().contains(q);
                return gwMatch || bankMatch || ruleMatch;
            }).toList();
        }

        List<MatchResultDto> dtoList = list.stream()
                .map(MatchResultDto::fromEntity)
                .collect(Collectors.toList());

        return ResponseEntity.ok(dtoList);
    }

    @PostMapping("/manual-review/{matchResultId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ANALYST')")
    @Transactional
    public ResponseEntity<MatchResultDto> overrideManualReview(
            @PathVariable("batchId") Long batchId,
            @PathVariable("matchResultId") Long matchResultId,
            @Valid @RequestBody ManualReviewActionRequest request) {

        MatchResult match = matchResultRepository.findById(matchResultId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Match result not found: " + matchResultId));

        if (!match.getBatch().getId().equals(batchId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Match result does not belong to batch: " + batchId);
        }

        match.setMatchStatus(request.getStatus());
        match.setMatchPass(MatchPass.MANUAL);
        match.setConfidence(1.0);
        match.setRuleFired("MANUAL_ANALYST_OVERRIDE");
        if (request.getReasoning() != null && !request.getReasoning().trim().isEmpty()) {
            match.setReasoning(request.getReasoning().trim());
        }

        if (request.getBankRecordId() != null) {
            bankRecordRepository.findById(request.getBankRecordId()).ifPresent(match::setBankRecord);
        }
        if (request.getLedgerRecordId() != null) {
            ledgerRecordRepository.findById(request.getLedgerRecordId()).ifPresent(match::setLedgerRecord);
        }

        match = matchResultRepository.save(match);

        // Update batch manual count & metrics
        Batch batch = match.getBatch();
        long manualCount = matchResultRepository.countByBatchIdAndStatus(batchId, MatchStatus.MANUAL_REVIEW);
        batch.setManualReviewCount((int) manualCount);
        batchRepository.save(batch);

        // Log audit entry
        AuditLogEntry audit = new AuditLogEntry(
                batch,
                match,
                AuditSource.USER_MANUAL,
                "MANUAL_DECISION_COMMIT",
                "ANALYST_OVERRIDE",
                1.0,
                "{\"action\":\"OVERRIDE_TO_" + request.getStatus() + "\"}",
                "{\"status\":\"" + match.getMatchStatus() + "\"}",
                request.getReasoning() != null ? request.getReasoning() : "Manual override by financial analyst."
        );
        auditLogEntryRepository.save(audit);

        return ResponseEntity.ok(MatchResultDto.fromEntity(match));
    }
}
