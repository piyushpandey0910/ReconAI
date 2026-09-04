package com.aifinance.service;

import com.aifinance.dto.BatchMetricsDto;
import com.aifinance.entity.Batch;
import com.aifinance.entity.MatchPass;
import com.aifinance.entity.MatchResult;
import com.aifinance.entity.MatchStatus;
import com.aifinance.repository.BatchRepository;
import com.aifinance.repository.MatchResultRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class MetricsService {

    private final BatchRepository batchRepository;
    private final MatchResultRepository matchResultRepository;

    public MetricsService(BatchRepository batchRepository, MatchResultRepository matchResultRepository) {
        this.batchRepository = batchRepository;
        this.matchResultRepository = matchResultRepository;
    }

    public BatchMetricsDto getBatchMetrics(Long batchId) {
        Batch batch = batchRepository.findById(batchId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Batch not found: " + batchId));

        List<MatchResult> records = matchResultRepository.findByBatchId(batchId);
        int total = records.size();

        int matchedCount = 0;
        int pass1Matched = 0;
        int pass2Matched = 0;
        int manualReview = 0;
        BigDecimal reconciledAmt = BigDecimal.ZERO;
        BigDecimal discrepancyAmt = BigDecimal.ZERO;

        Map<String, Long> exceptionBreakdown = new HashMap<>();
        Map<String, Long> statusBreakdown = new HashMap<>();
        Map<String, Long> passBreakdown = new HashMap<>();

        for (MatchResult m : records) {
            String status = m.getMatchStatus().name();
            statusBreakdown.put(status, statusBreakdown.getOrDefault(status, 0L) + 1);

            if (m.getMatchPass() != null) {
                String pass = m.getMatchPass().name();
                passBreakdown.put(pass, passBreakdown.getOrDefault(pass, 0L) + 1);
            }

            if (m.getMatchStatus() == MatchStatus.MATCHED) {
                matchedCount++;
                if (m.getMatchPass() == MatchPass.PASS1_RULES) pass1Matched++;
                if (m.getMatchPass() == MatchPass.PASS2_AI) pass2Matched++;
                if (m.getMatchedAmount() != null) {
                    reconciledAmt = reconciledAmt.add(m.getMatchedAmount());
                }
            } else if (m.getMatchStatus() == MatchStatus.MANUAL_REVIEW || m.getMatchStatus() == MatchStatus.NEEDS_REVIEW) {
                manualReview++;
                String caseType = (m.getCaseType() != null && !m.getCaseType().isEmpty()) ? m.getCaseType() : "UNCLASSIFIED";
                exceptionBreakdown.put(caseType, exceptionBreakdown.getOrDefault(caseType, 0L) + 1);
            }

            if (m.getDiscrepancyAmount() != null && m.getDiscrepancyAmount().compareTo(BigDecimal.ZERO) > 0) {
                discrepancyAmt = discrepancyAmt.add(m.getDiscrepancyAmount());
            }
        }

        double matchRate = (total > 0)
                ? BigDecimal.valueOf((double) matchedCount / total * 100.0).setScale(2, RoundingMode.HALF_UP).doubleValue()
                : 0.0;

        BatchMetricsDto dto = new BatchMetricsDto();
        dto.setBatchId(batch.getId());
        dto.setBatchName(batch.getBatchName());
        dto.setStatus(batch.getStatus().name());
        dto.setTotalRecords(total);
        dto.setMatchedCount(matchedCount);
        dto.setPass1MatchedCount(pass1Matched);
        dto.setPass2MatchedCount(pass2Matched);
        dto.setManualReviewCount(manualReview);
        dto.setMatchRatePercentage(matchRate);
        dto.setTotalReconciledAmount(reconciledAmt);
        dto.setTotalDiscrepancyAmount(discrepancyAmt);
        dto.setExceptionBreakdown(exceptionBreakdown);
        dto.setStatusBreakdown(statusBreakdown);
        dto.setPassBreakdown(passBreakdown);

        return dto;
    }
}
