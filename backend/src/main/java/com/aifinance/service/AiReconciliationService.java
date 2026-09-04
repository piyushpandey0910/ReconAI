package com.aifinance.service;

import com.aifinance.entity.*;
import com.aifinance.repository.*;
import com.aifinance.security.RateLimiterService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.*;

@Service
public class AiReconciliationService {

    private static final Logger log = LoggerFactory.getLogger(AiReconciliationService.class);

    private final BatchRepository batchRepository;
    private final MatchResultRepository matchResultRepository;
    private final BankRecordRepository bankRecordRepository;
    private final LedgerRecordRepository ledgerRecordRepository;
    private final AuditLogEntryRepository auditLogEntryRepository;
    private final AuthService authService;
    private final RateLimiterService rateLimiterService;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    @Value("${ai.reconciliation.confidence-threshold:0.7}")
    private double confidenceThreshold;

    public AiReconciliationService(
            BatchRepository batchRepository,
            MatchResultRepository matchResultRepository,
            BankRecordRepository bankRecordRepository,
            LedgerRecordRepository ledgerRecordRepository,
            AuditLogEntryRepository auditLogEntryRepository,
            AuthService authService,
            RateLimiterService rateLimiterService,
            @Value("${ai.service.url:http://localhost:8000}") String aiServiceUrl,
            @Value("${ai.service.timeout-seconds:15}") int timeoutSeconds) {

        this.batchRepository = batchRepository;
        this.matchResultRepository = matchResultRepository;
        this.bankRecordRepository = bankRecordRepository;
        this.ledgerRecordRepository = ledgerRecordRepository;
        this.auditLogEntryRepository = auditLogEntryRepository;
        this.authService = authService;
        this.rateLimiterService = rateLimiterService;
        this.objectMapper = new ObjectMapper();

        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(timeoutSeconds));
        requestFactory.setReadTimeout(Duration.ofSeconds(timeoutSeconds));

        this.restClient = RestClient.builder()
                .baseUrl(aiServiceUrl)
                .requestFactory(requestFactory)
                .build();
    }

    @Transactional
    public List<MatchResult> reconcilePass2(Long batchId) {
        User currentUser = authService.getCurrentAuthenticatedUser();
        if (!rateLimiterService.allowAiRequest(currentUser.getUsername())) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                    "AI rate limit exceeded. Please wait before triggering another AI batch reconciliation.");
        }

        Batch batch = batchRepository.findById(batchId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Batch not found: " + batchId));

        if (batch.getStatus() != BatchStatus.RULES_COMPLETED && batch.getStatus() != BatchStatus.AI_COMPLETED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Please execute Pass 1 (Rules) before triggering Pass 2 (AI).");
        }

        List<MatchResult> needsReviewResults = matchResultRepository.findByBatchIdAndMatchStatus(batchId, MatchStatus.NEEDS_REVIEW);
        if (needsReviewResults.isEmpty()) {
            log.info("No records in NEEDS_REVIEW status for Batch {}. Skipping AI pass.", batchId);
            batch.setStatus(BatchStatus.AI_COMPLETED);
            batchRepository.save(batch);
            return Collections.emptyList();
        }

        // Get matched bank and ledger IDs to find candidate pools
        List<MatchResult> allMatches = matchResultRepository.findByBatchId(batchId);
        Set<Long> matchedBankIds = new HashSet<>();
        Set<Long> matchedLedgerIds = new HashSet<>();
        for (MatchResult m : allMatches) {
            if (m.getBankRecord() != null) matchedBankIds.add(m.getBankRecord().getId());
            if (m.getLedgerRecord() != null) matchedLedgerIds.add(m.getLedgerRecord().getId());
        }

        List<BankRecord> candidateBanks = bankRecordRepository.findByBatchId(batchId)
                .stream()
                .filter(b -> !matchedBankIds.contains(b.getId()))
                .toList();

        List<LedgerRecord> candidateLedgers = ledgerRecordRepository.findByBatchId(batchId)
                .stream()
                .filter(l -> !matchedLedgerIds.contains(l.getId()))
                .toList();

        // Build payload for FastAPI microservice
        List<Map<String, Object>> ambiguousPayload = new ArrayList<>();
        for (MatchResult mr : needsReviewResults) {
            GatewayRecord gw = mr.getGatewayRecord();
            if (gw != null) {
                Map<String, Object> map = new HashMap<>();
                map.put("transactionId", gw.getTransactionId());
                map.put("orderId", gw.getOrderId());
                map.put("amount", gw.getAmount());
                map.put("customerEmail", gw.getCustomerEmail());
                map.put("timestamp", gw.getTimestamp());
                ambiguousPayload.add(map);
            }
        }

        List<Map<String, Object>> bankPayload = new ArrayList<>();
        for (BankRecord b : candidateBanks) {
            Map<String, Object> map = new HashMap<>();
            map.put("bankRefId", b.getBankRefId());
            map.put("amount", b.getAmount());
            map.put("narration", b.getNarration());
            map.put("date", b.getDate());
            map.put("fee", b.getFee());
            bankPayload.add(map);
        }

        List<Map<String, Object>> ledgerPayload = new ArrayList<>();
        for (LedgerRecord l : candidateLedgers) {
            Map<String, Object> map = new HashMap<>();
            map.put("ledgerEntryId", l.getLedgerEntryId());
            map.put("internalRef", l.getInternalRef());
            map.put("amount", l.getAmount());
            map.put("description", l.getDescription());
            map.put("entryDate", l.getEntryDate());
            ledgerPayload.add(map);
        }

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("ambiguous_records", ambiguousPayload);
        requestBody.put("candidate_bank_records", bankPayload);
        requestBody.put("candidate_ledger_records", ledgerPayload);

        Map<String, Object> aiResponse = null;
        try {
            log.info("Sending {} ambiguous records to Python AI microservice for Batch {}...", ambiguousPayload.size(), batchId);
            aiResponse = restClient.post()
                    .uri("/match-ambiguous")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestBody)
                    .retrieve()
                    .body(new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            log.warn("Python AI service call failed: {}. Gracefully falling back to MANUAL_REVIEW queue.", e.getMessage());
            handleAiFailureGracefully(batch, needsReviewResults, e.getMessage());
            return needsReviewResults;
        }

        // Process AI matches
        applyAiMatches(batch, needsReviewResults, candidateBanks, candidateLedgers, aiResponse);
        return needsReviewResults;
    }

    private void applyAiMatches(Batch batch, List<MatchResult> needsReviewResults,
                                List<BankRecord> candidateBanks, List<LedgerRecord> candidateLedgers,
                                Map<String, Object> aiResponse) {
        String modelUsed = (aiResponse != null && aiResponse.containsKey("model_used"))
                ? (String) aiResponse.get("model_used")
                : "Groq-LLM";

        List<Map<String, Object>> matchesList = Collections.emptyList();
        if (aiResponse != null && aiResponse.containsKey("matches")) {
            matchesList = (List<Map<String, Object>>) aiResponse.get("matches");
        }

        Map<String, Map<String, Object>> matchByRecordId = new HashMap<>();
        for (Map<String, Object> m : matchesList) {
            String recId = String.valueOf(m.get("record_id"));
            matchByRecordId.put(recId, m);
        }

        int pass2Matched = 0;
        int manualReview = 0;
        BigDecimal additionalReconciled = BigDecimal.ZERO;

        for (MatchResult mr : needsReviewResults) {
            GatewayRecord gw = mr.getGatewayRecord();
            if (gw == null) continue;

            Map<String, Object> aiMatch = matchByRecordId.get(gw.getTransactionId());
            if (aiMatch == null) {
                aiMatch = matchByRecordId.get(gw.getOrderId());
            }

            double confidence = 0.0;
            String suggestedMatchId = null;
            String caseType = "UNRESOLVED";
            String reasoning = "No clear match identified by AI.";

            if (aiMatch != null) {
                confidence = ((Number) aiMatch.getOrDefault("confidence", 0.0)).doubleValue();
                suggestedMatchId = (String) aiMatch.get("suggested_match_id");
                caseType = (String) aiMatch.getOrDefault("case_type", "AMBIGUOUS");
                reasoning = (String) aiMatch.getOrDefault("reasoning", "");
            }

            mr.setMatchPass(MatchPass.PASS2_AI);
            mr.setConfidence(confidence);
            mr.setCaseType(caseType);
            mr.setReasoning(reasoning);
            mr.setRuleFired(modelUsed);
            mr.setSuggestedMatchId(suggestedMatchId);

            // Server-side enforcement of confidence threshold
            if (confidence >= confidenceThreshold && suggestedMatchId != null && !suggestedMatchId.trim().isEmpty()) {
                // Find matching bank or ledger record
                BankRecord matchedBank = findBankByIdOrRef(candidateBanks, suggestedMatchId);
                LedgerRecord matchedLedger = findLedgerByIdOrRef(candidateLedgers, suggestedMatchId);

                mr.setBankRecord(matchedBank);
                mr.setLedgerRecord(matchedLedger);
                mr.setMatchStatus(MatchStatus.MATCHED);
                mr.setMatchedAmount(matchedBank != null ? matchedBank.getAmount() : gw.getAmount());
                if (matchedBank != null) {
                    mr.setDiscrepancyAmount(gw.getAmount().subtract(matchedBank.getAmount()).abs());
                }

                pass2Matched++;
                additionalReconciled = additionalReconciled.add(mr.getMatchedAmount());
            } else {
                mr.setMatchStatus(MatchStatus.MANUAL_REVIEW);
                manualReview++;
            }

            matchResultRepository.save(mr);

            // Audit entry
            AuditLogEntry audit = new AuditLogEntry(
                    batch,
                    mr,
                    AuditSource.AI_MODEL,
                    mr.getMatchStatus() == MatchStatus.MATCHED ? "AI_AUTO_MATCH" : "AI_MANUAL_REVIEW_ROUTED",
                    modelUsed,
                    confidence,
                    "{\"transactionId\":\"" + gw.getTransactionId() + "\",\"amount\":" + gw.getAmount() + "}",
                    "{\"status\":\"" + mr.getMatchStatus() + "\",\"caseType\":\"" + caseType + "\",\"confidence\":" + confidence + "}",
                    reasoning
            );
            auditLogEntryRepository.save(audit);
        }

        batch.setPass2MatchedCount(pass2Matched);
        batch.setManualReviewCount(manualReview);
        batch.setTotalReconciledAmount(batch.getTotalReconciledAmount().add(additionalReconciled));
        batch.setStatus(BatchStatus.AI_COMPLETED);
        batchRepository.save(batch);

        log.info("Pass 2 (AI) completed for Batch {}: {} resolved, {} manual review",
                batch.getId(), pass2Matched, manualReview);
    }

    private void handleAiFailureGracefully(Batch batch, List<MatchResult> needsReviewResults, String error) {
        int manualCount = 0;
        for (MatchResult mr : needsReviewResults) {
            mr.setMatchStatus(MatchStatus.MANUAL_REVIEW);
            mr.setMatchPass(MatchPass.PASS2_AI);
            mr.setConfidence(0.0);
            mr.setCaseType("AI_SERVICE_UNAVAILABLE");
            mr.setReasoning("AI microservice was unreachable (" + error + "). Gracefully routed to manual review queue.");
            matchResultRepository.save(mr);
            manualCount++;

            AuditLogEntry audit = new AuditLogEntry(
                    batch,
                    mr,
                    AuditSource.SYSTEM,
                    "AI_SERVICE_TIMEOUT_FALLBACK",
                    "FALLBACK_HANDLER",
                    0.0,
                    "{}",
                    "{\"status\":\"MANUAL_REVIEW\"}",
                    "Service fallback: " + error
            );
            auditLogEntryRepository.save(audit);
        }

        batch.setManualReviewCount(manualCount);
        batch.setStatus(BatchStatus.AI_COMPLETED);
        batchRepository.save(batch);
    }

    private BankRecord findBankByIdOrRef(List<BankRecord> list, String ref) {
        if (ref == null) return null;
        for (BankRecord b : list) {
            if (b.getBankRefId().equalsIgnoreCase(ref) || String.valueOf(b.getId()).equals(ref)) {
                return b;
            }
        }
        return null;
    }

    private LedgerRecord findLedgerByIdOrRef(List<LedgerRecord> list, String ref) {
        if (ref == null) return null;
        for (LedgerRecord l : list) {
            if (l.getInternalRef().equalsIgnoreCase(ref) || l.getLedgerEntryId().equalsIgnoreCase(ref) || String.valueOf(l.getId()).equals(ref)) {
                return l;
            }
        }
        return null;
    }
}
