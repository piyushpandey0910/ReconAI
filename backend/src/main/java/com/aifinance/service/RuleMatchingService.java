package com.aifinance.service;

import com.aifinance.entity.*;
import com.aifinance.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
public class RuleMatchingService {

    private static final Logger log = LoggerFactory.getLogger(RuleMatchingService.class);

    private final BatchRepository batchRepository;
    private final GatewayRecordRepository gatewayRecordRepository;
    private final BankRecordRepository bankRecordRepository;
    private final LedgerRecordRepository ledgerRecordRepository;
    private final MatchResultRepository matchResultRepository;
    private final AuditLogEntryRepository auditLogEntryRepository;

    public RuleMatchingService(BatchRepository batchRepository,
                               GatewayRecordRepository gatewayRecordRepository,
                               BankRecordRepository bankRecordRepository,
                               LedgerRecordRepository ledgerRecordRepository,
                               MatchResultRepository matchResultRepository,
                               AuditLogEntryRepository auditLogEntryRepository) {
        this.batchRepository = batchRepository;
        this.gatewayRecordRepository = gatewayRecordRepository;
        this.bankRecordRepository = bankRecordRepository;
        this.ledgerRecordRepository = ledgerRecordRepository;
        this.matchResultRepository = matchResultRepository;
        this.auditLogEntryRepository = auditLogEntryRepository;
    }

    @Transactional
    public List<MatchResult> reconcilePass1(Long batchId) {
        Batch batch = batchRepository.findById(batchId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Batch not found: " + batchId));

        if (!batch.areAllFilesUploaded()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Batch is not ready for reconciliation. All 3 files (Gateway, Bank, Ledger) must be uploaded.");
        }

        // Clean up previous results for this batch
        auditLogEntryRepository.deleteByBatchId(batchId);
        matchResultRepository.deleteByBatchId(batchId);

        List<GatewayRecord> gatewayRecords = gatewayRecordRepository.findByBatchId(batchId);
        List<BankRecord> bankRecords = bankRecordRepository.findByBatchId(batchId);
        List<LedgerRecord> ledgerRecords = ledgerRecordRepository.findByBatchId(batchId);

        Set<Long> matchedBankIds = new HashSet<>();
        Set<Long> matchedLedgerIds = new HashSet<>();

        List<MatchResult> results = new ArrayList<>();
        int pass1Matched = 0;
        BigDecimal totalReconciled = BigDecimal.ZERO;

        log.info("Starting Pass 1 (Deterministic Rules) for Batch {}: {} GW records, {} Bank records, {} Ledger records",
                batchId, gatewayRecords.size(), bankRecords.size(), ledgerRecords.size());

        for (GatewayRecord gw : gatewayRecords) {
            MatchResult match = null;

            // ==========================================
            // RULE 1: EXACT ID AND AMOUNT MATCH
            // ==========================================
            BankRecord bankMatch = findExactIdAndAmountBank(gw, bankRecords, matchedBankIds);
            LedgerRecord ledgerMatch = findExactIdAndAmountLedger(gw, ledgerRecords, matchedLedgerIds);

            if (bankMatch != null && ledgerMatch != null) {
                match = new MatchResult();
                match.setBatch(batch);
                match.setGatewayRecord(gw);
                match.setBankRecord(bankMatch);
                match.setLedgerRecord(ledgerMatch);
                match.setMatchStatus(MatchStatus.MATCHED);
                match.setMatchPass(MatchPass.PASS1_RULES);
                match.setRuleFired("EXACT_ID_AND_AMOUNT");
                match.setConfidence(1.0);
                match.setCaseType("EXACT_MATCH");
                match.setMatchedAmount(gw.getAmount());
                match.setDiscrepancyAmount(BigDecimal.ZERO);
                match.setReasoning("Exact match on Order ID [" + gw.getOrderId() + "] and Amount [$" + gw.getAmount() + "] across Gateway, Bank, and Ledger.");

                matchedBankIds.add(bankMatch.getId());
                matchedLedgerIds.add(ledgerMatch.getId());
            }

            // ==========================================
            // RULE 2: AMOUNT + DATE WINDOW MATCH (+/- 2 DAYS)
            // ==========================================
            if (match == null) {
                BankRecord bankDateMatch = findAmountAndDateBank(gw, bankRecords, matchedBankIds, 2);
                LedgerRecord ledgerDateMatch = findAmountAndDateLedger(gw, ledgerRecords, matchedLedgerIds, 2);

                if (bankDateMatch != null && ledgerDateMatch != null) {
                    match = new MatchResult();
                    match.setBatch(batch);
                    match.setGatewayRecord(gw);
                    match.setBankRecord(bankDateMatch);
                    match.setLedgerRecord(ledgerDateMatch);
                    match.setMatchStatus(MatchStatus.MATCHED);
                    match.setMatchPass(MatchPass.PASS1_RULES);
                    match.setRuleFired("AMOUNT_DATE_WINDOW");
                    match.setConfidence(0.90);
                    match.setCaseType("DATE_WINDOW_MATCH");
                    match.setMatchedAmount(gw.getAmount());
                    match.setDiscrepancyAmount(BigDecimal.ZERO);
                    match.setReasoning("Amount match [$" + gw.getAmount() + "] within 2-day settlement window. Bank: " + bankDateMatch.getDate() + ", Ledger: " + ledgerDateMatch.getEntryDate());

                    matchedBankIds.add(bankDateMatch.getId());
                    matchedLedgerIds.add(ledgerDateMatch.getId());
                }
            }

            // ==========================================
            // RULE 3: FUZZY NARRATION + FEE TOLERANCE
            // ==========================================
            if (match == null) {
                BankRecord bankFuzzyMatch = findFuzzyBankMatch(gw, bankRecords, matchedBankIds);
                LedgerRecord ledgerFuzzyMatch = findFuzzyLedgerMatch(gw, ledgerRecords, matchedLedgerIds);

                if (bankFuzzyMatch != null) {
                    BigDecimal diff = gw.getAmount().subtract(bankFuzzyMatch.getAmount()).abs();
                    match = new MatchResult();
                    match.setBatch(batch);
                    match.setGatewayRecord(gw);
                    match.setBankRecord(bankFuzzyMatch);
                    match.setLedgerRecord(ledgerFuzzyMatch); // may be null if ledger has variance
                    match.setMatchStatus(MatchStatus.MATCHED);
                    match.setMatchPass(MatchPass.PASS1_RULES);
                    match.setRuleFired("FUZZY_NARRATION_AMOUNT");
                    match.setConfidence(0.80);
                    match.setCaseType("FEE_TOLERANCE_MATCH");
                    match.setMatchedAmount(bankFuzzyMatch.getAmount());
                    match.setDiscrepancyAmount(diff);
                    match.setReasoning("Fuzzy narration match with standard fee variance [Fee: $" + diff + "]. Bank narration: '" + bankFuzzyMatch.getNarration() + "'");

                    matchedBankIds.add(bankFuzzyMatch.getId());
                    if (ledgerFuzzyMatch != null) {
                        matchedLedgerIds.add(ledgerFuzzyMatch.getId());
                    }
                }
            }

            // ==========================================
            // UNMATCHED -> NEEDS_REVIEW (PASS 2 CANDIDATE)
            // ==========================================
            if (match == null) {
                match = new MatchResult();
                match.setBatch(batch);
                match.setGatewayRecord(gw);
                match.setMatchStatus(MatchStatus.NEEDS_REVIEW);
                match.setMatchPass(MatchPass.PASS1_RULES);
                match.setRuleFired("UNMATCHED_PASS1");
                match.setConfidence(0.0);
                match.setCaseType("AMBIGUOUS_RECORD");
                match.setMatchedAmount(BigDecimal.ZERO);
                match.setDiscrepancyAmount(gw.getAmount());
                match.setReasoning("Deterministic rules could not match this record. Leftover for Pass 2 AI classification.");
            } else {
                pass1Matched++;
                totalReconciled = totalReconciled.add(match.getMatchedAmount());
            }

            match = matchResultRepository.save(match);
            results.add(match);

            // Log to Audit Log
            AuditLogEntry audit = new AuditLogEntry(
                    batch,
                    match,
                    AuditSource.RULE_ENGINE,
                    match.getMatchStatus() == MatchStatus.MATCHED ? "RULE_MATCH" : "RULE_NEEDS_REVIEW",
                    match.getRuleFired(),
                    match.getConfidence(),
                    createInputSnapshot(gw),
                    createOutputSnapshot(match),
                    match.getReasoning()
            );
            auditLogEntryRepository.save(audit);
        }

        // Update Batch Summary
        batch.setStatus(BatchStatus.RULES_COMPLETED);
        batch.setPass1MatchedCount(pass1Matched);
        batch.setTotalReconciledAmount(totalReconciled);
        batch.setTotalRecords(gatewayRecords.size());
        batchRepository.save(batch);

        log.info("Pass 1 completed for Batch {}: {} matched, {} needs review",
                batchId, pass1Matched, (gatewayRecords.size() - pass1Matched));

        return results;
    }

    // Helper matchers
    private BankRecord findExactIdAndAmountBank(GatewayRecord gw, List<BankRecord> bankRecords, Set<Long> matchedIds) {
        String orderId = gw.getOrderId() != null ? gw.getOrderId().toLowerCase() : "";
        String txnId = gw.getTransactionId().toLowerCase();

        for (BankRecord b : bankRecords) {
            if (matchedIds.contains(b.getId())) continue;
            if (isAmountEqual(b.getAmount(), gw.getAmount())) {
                String narr = b.getNarration().toLowerCase();
                if ((!orderId.isEmpty() && narr.contains(orderId)) || narr.contains(txnId) || b.getBankRefId().equalsIgnoreCase(orderId)) {
                    return b;
                }
            }
        }
        return null;
    }

    private LedgerRecord findExactIdAndAmountLedger(GatewayRecord gw, List<LedgerRecord> ledgerRecords, Set<Long> matchedIds) {
        String orderId = gw.getOrderId() != null ? gw.getOrderId().toLowerCase() : "";
        String txnId = gw.getTransactionId().toLowerCase();

        for (LedgerRecord l : ledgerRecords) {
            if (matchedIds.contains(l.getId())) continue;
            if (isAmountEqual(l.getAmount(), gw.getAmount())) {
                String ref = l.getInternalRef().toLowerCase();
                if ((!orderId.isEmpty() && ref.contains(orderId)) || ref.contains(txnId) || l.getLedgerEntryId().equalsIgnoreCase(orderId)) {
                    return l;
                }
            }
        }
        return null;
    }

    private BankRecord findAmountAndDateBank(GatewayRecord gw, List<BankRecord> bankRecords, Set<Long> matchedIds, int maxDayDiff) {
        LocalDate gwDate = gw.getTimestamp() != null ? gw.getTimestamp().toLocalDate() : LocalDate.now();

        for (BankRecord b : bankRecords) {
            if (matchedIds.contains(b.getId())) continue;
            if (isAmountEqual(b.getAmount(), gw.getAmount())) {
                LocalDate bDate = b.getDate() != null ? b.getDate() : LocalDate.now();
                long days = Math.abs(ChronoUnit.DAYS.between(gwDate, bDate));
                if (days <= maxDayDiff) {
                    return b;
                }
            }
        }
        return null;
    }

    private LedgerRecord findAmountAndDateLedger(GatewayRecord gw, List<LedgerRecord> ledgerRecords, Set<Long> matchedIds, int maxDayDiff) {
        LocalDate gwDate = gw.getTimestamp() != null ? gw.getTimestamp().toLocalDate() : LocalDate.now();

        for (LedgerRecord l : ledgerRecords) {
            if (matchedIds.contains(l.getId())) continue;
            if (isAmountEqual(l.getAmount(), gw.getAmount())) {
                LocalDate lDate = l.getEntryDate() != null ? l.getEntryDate() : LocalDate.now();
                long days = Math.abs(ChronoUnit.DAYS.between(gwDate, lDate));
                if (days <= maxDayDiff) {
                    return l;
                }
            }
        }
        return null;
    }

    private BankRecord findFuzzyBankMatch(GatewayRecord gw, List<BankRecord> bankRecords, Set<Long> matchedIds) {
        String emailName = "";
        if (gw.getCustomerEmail() != null && gw.getCustomerEmail().contains("@")) {
            emailName = gw.getCustomerEmail().substring(0, gw.getCustomerEmail().indexOf('@')).toLowerCase();
        }

        for (BankRecord b : bankRecords) {
            if (matchedIds.contains(b.getId())) continue;

            // Check if amount is within 3% fee tolerance
            BigDecimal diff = gw.getAmount().subtract(b.getAmount()).abs();
            BigDecimal tolerance = gw.getAmount().multiply(new BigDecimal("0.035")); // max 3.5% fee

            if (diff.compareTo(tolerance) <= 0 || isAmountEqual(b.getAmount().add(b.getFee()), gw.getAmount())) {
                String narr = b.getNarration().toLowerCase();
                if (!emailName.isEmpty() && narr.contains(emailName)) {
                    return b;
                }
                if (gw.getOrderId() != null && calculateSimilarity(gw.getOrderId().toLowerCase(), narr) > 0.70) {
                    return b;
                }
            }
        }
        return null;
    }

    private LedgerRecord findFuzzyLedgerMatch(GatewayRecord gw, List<LedgerRecord> ledgerRecords, Set<Long> matchedIds) {
        for (LedgerRecord l : ledgerRecords) {
            if (matchedIds.contains(l.getId())) continue;
            BigDecimal diff = gw.getAmount().subtract(l.getAmount()).abs();
            BigDecimal tolerance = gw.getAmount().multiply(new BigDecimal("0.035"));
            if (diff.compareTo(tolerance) <= 0) {
                return l;
            }
        }
        return null;
    }

    private boolean isAmountEqual(BigDecimal a, BigDecimal b) {
        if (a == null || b == null) return false;
        return a.setScale(2, RoundingMode.HALF_UP).compareTo(b.setScale(2, RoundingMode.HALF_UP)) == 0;
    }

    private double calculateSimilarity(String s1, String s2) {
        if (s1.equals(s2)) return 1.0;
        int maxLen = Math.max(s1.length(), s2.length());
        if (maxLen == 0) return 1.0;
        int distance = computeLevenshteinDistance(s1, s2);
        return 1.0 - ((double) distance / maxLen);
    }

    private int computeLevenshteinDistance(String a, String b) {
        int[] costs = new int[b.length() + 1];
        for (int j = 0; j <= b.length(); j++) costs[j] = j;
        for (int i = 1; i <= a.length(); i++) {
            costs[0] = i;
            int nw = i - 1;
            for (int j = 1; j <= b.length(); j++) {
                int cj = Math.min(1 + Math.min(costs[j], costs[j - 1]),
                        a.charAt(i - 1) == b.charAt(j - 1) ? nw : nw + 1);
                nw = costs[j];
                costs[j] = cj;
            }
        }
        return costs[b.length()];
    }

    private String createInputSnapshot(GatewayRecord gw) {
        return "{\"transactionId\":\"" + gw.getTransactionId() +
                "\",\"orderId\":\"" + gw.getOrderId() +
                "\",\"amount\":" + gw.getAmount() +
                ",\"customerEmail\":\"" + gw.getCustomerEmail() + "\"}";
    }

    private String createOutputSnapshot(MatchResult m) {
        return "{\"status\":\"" + m.getMatchStatus() +
                "\",\"rule\":\"" + m.getRuleFired() +
                "\",\"confidence\":" + m.getConfidence() +
                ",\"bankId\":" + (m.getBankRecord() != null ? m.getBankRecord().getId() : null) +
                ",\"ledgerId\":" + (m.getLedgerRecord() != null ? m.getLedgerRecord().getId() : null) + "}";
    }
}
