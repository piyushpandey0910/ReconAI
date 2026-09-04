package com.aifinance.dto;

import com.aifinance.entity.Batch;
import com.aifinance.entity.BatchStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class BatchDto {
    private Long id;
    private String batchName;
    private BatchStatus status;
    private String uploadedBy;
    private LocalDateTime createdAt;
    private int totalRecords;
    private int pass1MatchedCount;
    private int pass2MatchedCount;
    private int manualReviewCount;
    private BigDecimal totalReconciledAmount;
    private boolean gatewayUploaded;
    private boolean bankUploaded;
    private boolean ledgerUploaded;
    private boolean readyForRules;

    public BatchDto() {}

    public static BatchDto fromEntity(Batch batch) {
        BatchDto dto = new BatchDto();
        dto.setId(batch.getId());
        dto.setBatchName(batch.getBatchName());
        dto.setStatus(batch.getStatus());
        dto.setUploadedBy(batch.getUploadedBy() != null ? batch.getUploadedBy().getUsername() : "Unknown");
        dto.setCreatedAt(batch.getCreatedAt());
        dto.setTotalRecords(batch.getTotalRecords());
        dto.setPass1MatchedCount(batch.getPass1MatchedCount());
        dto.setPass2MatchedCount(batch.getPass2MatchedCount());
        dto.setManualReviewCount(batch.getManualReviewCount());
        dto.setTotalReconciledAmount(batch.getTotalReconciledAmount());
        dto.setGatewayUploaded(batch.isGatewayUploaded());
        dto.setBankUploaded(batch.isBankUploaded());
        dto.setLedgerUploaded(batch.isLedgerUploaded());
        dto.setReadyForRules(batch.areAllFilesUploaded());
        return dto;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getBatchName() { return batchName; }
    public void setBatchName(String batchName) { this.batchName = batchName; }

    public BatchStatus getStatus() { return status; }
    public void setStatus(BatchStatus status) { this.status = status; }

    public String getUploadedBy() { return uploadedBy; }
    public void setUploadedBy(String uploadedBy) { this.uploadedBy = uploadedBy; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public int getTotalRecords() { return totalRecords; }
    public void setTotalRecords(int totalRecords) { this.totalRecords = totalRecords; }

    public int getPass1MatchedCount() { return pass1MatchedCount; }
    public void setPass1MatchedCount(int pass1MatchedCount) { this.pass1MatchedCount = pass1MatchedCount; }

    public int getPass2MatchedCount() { return pass2MatchedCount; }
    public void setPass2MatchedCount(int pass2MatchedCount) { this.pass2MatchedCount = pass2MatchedCount; }

    public int getManualReviewCount() { return manualReviewCount; }
    public void setManualReviewCount(int manualReviewCount) { this.manualReviewCount = manualReviewCount; }

    public BigDecimal getTotalReconciledAmount() { return totalReconciledAmount; }
    public void setTotalReconciledAmount(BigDecimal totalReconciledAmount) { this.totalReconciledAmount = totalReconciledAmount; }

    public boolean isGatewayUploaded() { return gatewayUploaded; }
    public void setGatewayUploaded(boolean gatewayUploaded) { this.gatewayUploaded = gatewayUploaded; }

    public boolean isBankUploaded() { return bankUploaded; }
    public void setBankUploaded(boolean bankUploaded) { this.bankUploaded = bankUploaded; }

    public boolean isLedgerUploaded() { return ledgerUploaded; }
    public void setLedgerUploaded(boolean ledgerUploaded) { this.ledgerUploaded = ledgerUploaded; }

    public boolean isReadyForRules() { return readyForRules; }
    public void setReadyForRules(boolean readyForRules) { this.readyForRules = readyForRules; }
}
