package com.aifinance.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "batches")
public class Batch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String batchName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private BatchStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploaded_by_user_id", nullable = false)
    private User uploadedBy;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private int totalRecords;
    private int pass1MatchedCount;
    private int pass2MatchedCount;
    private int manualReviewCount;
    private BigDecimal totalReconciledAmount;

    private boolean gatewayUploaded;
    private boolean bankUploaded;
    private boolean ledgerUploaded;

    public Batch() {
        this.status = BatchStatus.PENDING_FILES;
        this.createdAt = LocalDateTime.now();
        this.totalReconciledAmount = BigDecimal.ZERO;
    }

    public Batch(String batchName, User uploadedBy) {
        this();
        this.batchName = batchName;
        this.uploadedBy = uploadedBy;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getBatchName() { return batchName; }
    public void setBatchName(String batchName) { this.batchName = batchName; }

    public BatchStatus getStatus() { return status; }
    public void setStatus(BatchStatus status) { this.status = status; }

    public User getUploadedBy() { return uploadedBy; }
    public void setUploadedBy(User uploadedBy) { this.uploadedBy = uploadedBy; }

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

    public boolean areAllFilesUploaded() {
        return gatewayUploaded && bankUploaded && ledgerUploaded;
    }
}
