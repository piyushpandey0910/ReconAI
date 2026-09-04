package com.aifinance.dto;

import java.math.BigDecimal;
import java.util.Map;

public class BatchMetricsDto {
    private Long batchId;
    private String batchName;
    private String status;
    private int totalRecords;
    private int matchedCount;
    private int pass1MatchedCount;
    private int pass2MatchedCount;
    private int manualReviewCount;
    private double matchRatePercentage;
    private BigDecimal totalReconciledAmount;
    private BigDecimal totalDiscrepancyAmount;
    private Map<String, Long> exceptionBreakdown;
    private Map<String, Long> statusBreakdown;
    private Map<String, Long> passBreakdown;

    public BatchMetricsDto() {}

    public Long getBatchId() { return batchId; }
    public void setBatchId(Long batchId) { this.batchId = batchId; }

    public String getBatchName() { return batchName; }
    public void setBatchName(String batchName) { this.batchName = batchName; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public int getTotalRecords() { return totalRecords; }
    public void setTotalRecords(int totalRecords) { this.totalRecords = totalRecords; }

    public int getMatchedCount() { return matchedCount; }
    public void setMatchedCount(int matchedCount) { this.matchedCount = matchedCount; }

    public int getPass1MatchedCount() { return pass1MatchedCount; }
    public void setPass1MatchedCount(int pass1MatchedCount) { this.pass1MatchedCount = pass1MatchedCount; }

    public int getPass2MatchedCount() { return pass2MatchedCount; }
    public void setPass2MatchedCount(int pass2MatchedCount) { this.pass2MatchedCount = pass2MatchedCount; }

    public int getManualReviewCount() { return manualReviewCount; }
    public void setManualReviewCount(int manualReviewCount) { this.manualReviewCount = manualReviewCount; }

    public double getMatchRatePercentage() { return matchRatePercentage; }
    public void setMatchRatePercentage(double matchRatePercentage) { this.matchRatePercentage = matchRatePercentage; }

    public BigDecimal getTotalReconciledAmount() { return totalReconciledAmount; }
    public void setTotalReconciledAmount(BigDecimal totalReconciledAmount) { this.totalReconciledAmount = totalReconciledAmount; }

    public BigDecimal getTotalDiscrepancyAmount() { return totalDiscrepancyAmount; }
    public void setTotalDiscrepancyAmount(BigDecimal totalDiscrepancyAmount) { this.totalDiscrepancyAmount = totalDiscrepancyAmount; }

    public Map<String, Long> getExceptionBreakdown() { return exceptionBreakdown; }
    public void setExceptionBreakdown(Map<String, Long> exceptionBreakdown) { this.exceptionBreakdown = exceptionBreakdown; }

    public Map<String, Long> getStatusBreakdown() { return statusBreakdown; }
    public void setStatusBreakdown(Map<String, Long> statusBreakdown) { this.statusBreakdown = statusBreakdown; }

    public Map<String, Long> getPassBreakdown() { return passBreakdown; }
    public void setPassBreakdown(Map<String, Long> passBreakdown) { this.passBreakdown = passBreakdown; }
}
