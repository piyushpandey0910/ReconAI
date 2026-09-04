package com.aifinance.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "match_results", indexes = {
    @Index(name = "idx_match_batch", columnList = "batch_id"),
    @Index(name = "idx_match_status", columnList = "matchStatus")
})
public class MatchResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "batch_id", nullable = false)
    private Batch batch;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "gateway_record_id")
    private GatewayRecord gatewayRecord;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "bank_record_id")
    private BankRecord bankRecord;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "ledger_record_id")
    private LedgerRecord ledgerRecord;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private MatchStatus matchStatus;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private MatchPass matchPass;

    @Column(length = 100)
    private String ruleFired;

    private Double confidence;

    @Column(length = 100)
    private String caseType;

    @Column(length = 2000)
    private String reasoning;

    @Column(length = 100)
    private String suggestedMatchId;

    @Column(precision = 19, scale = 4)
    private BigDecimal matchedAmount;

    @Column(precision = 19, scale = 4)
    private BigDecimal discrepancyAmount;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    public MatchResult() {
        this.createdAt = LocalDateTime.now();
        this.matchedAmount = BigDecimal.ZERO;
        this.discrepancyAmount = BigDecimal.ZERO;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Batch getBatch() { return batch; }
    public void setBatch(Batch batch) { this.batch = batch; }

    public GatewayRecord getGatewayRecord() { return gatewayRecord; }
    public void setGatewayRecord(GatewayRecord gatewayRecord) { this.gatewayRecord = gatewayRecord; }

    public BankRecord getBankRecord() { return bankRecord; }
    public void setBankRecord(BankRecord bankRecord) { this.bankRecord = bankRecord; }

    public LedgerRecord getLedgerRecord() { return ledgerRecord; }
    public void setLedgerRecord(LedgerRecord ledgerRecord) { this.ledgerRecord = ledgerRecord; }

    public MatchStatus getMatchStatus() { return matchStatus; }
    public void setMatchStatus(MatchStatus matchStatus) { this.matchStatus = matchStatus; }

    public MatchPass getMatchPass() { return matchPass; }
    public void setMatchPass(MatchPass matchPass) { this.matchPass = matchPass; }

    public String getRuleFired() { return ruleFired; }
    public void setRuleFired(String ruleFired) { this.ruleFired = ruleFired; }

    public Double getConfidence() { return confidence; }
    public void setConfidence(Double confidence) { this.confidence = confidence; }

    public String getCaseType() { return caseType; }
    public void setCaseType(String caseType) { this.caseType = caseType; }

    public String getReasoning() { return reasoning; }
    public void setReasoning(String reasoning) { this.reasoning = reasoning; }

    public String getSuggestedMatchId() { return suggestedMatchId; }
    public void setSuggestedMatchId(String suggestedMatchId) { this.suggestedMatchId = suggestedMatchId; }

    public BigDecimal getMatchedAmount() { return matchedAmount; }
    public void setMatchedAmount(BigDecimal matchedAmount) { this.matchedAmount = matchedAmount; }

    public BigDecimal getDiscrepancyAmount() { return discrepancyAmount; }
    public void setDiscrepancyAmount(BigDecimal discrepancyAmount) { this.discrepancyAmount = discrepancyAmount; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
