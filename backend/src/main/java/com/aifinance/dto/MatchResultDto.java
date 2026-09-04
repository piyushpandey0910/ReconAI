package com.aifinance.dto;

import com.aifinance.entity.MatchPass;
import com.aifinance.entity.MatchResult;
import com.aifinance.entity.MatchStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class MatchResultDto {
    private Long id;
    private Long batchId;

    // Gateway info
    private Long gatewayId;
    private String gatewayTxnId;
    private BigDecimal gatewayAmount;
    private String gatewayOrderId;
    private String gatewayCustomerEmail;

    // Bank info
    private Long bankId;
    private String bankRefId;
    private BigDecimal bankAmount;
    private String bankNarration;
    private BigDecimal bankFee;

    // Ledger info
    private Long ledgerId;
    private String ledgerEntryId;
    private String ledgerInternalRef;
    private BigDecimal ledgerAmount;
    private String ledgerDescription;

    // Match metadata
    private MatchStatus matchStatus;
    private MatchPass matchPass;
    private String ruleFired;
    private Double confidence;
    private String caseType;
    private String reasoning;
    private String suggestedMatchId;
    private BigDecimal matchedAmount;
    private BigDecimal discrepancyAmount;
    private LocalDateTime createdAt;

    public MatchResultDto() {}

    public static MatchResultDto fromEntity(MatchResult m) {
        MatchResultDto dto = new MatchResultDto();
        dto.setId(m.getId());
        dto.setBatchId(m.getBatch() != null ? m.getBatch().getId() : null);

        if (m.getGatewayRecord() != null) {
            dto.setGatewayId(m.getGatewayRecord().getId());
            dto.setGatewayTxnId(m.getGatewayRecord().getTransactionId());
            dto.setGatewayAmount(m.getGatewayRecord().getAmount());
            dto.setGatewayOrderId(m.getGatewayRecord().getOrderId());
            dto.setGatewayCustomerEmail(m.getGatewayRecord().getCustomerEmail());
        }

        if (m.getBankRecord() != null) {
            dto.setBankId(m.getBankRecord().getId());
            dto.setBankRefId(m.getBankRecord().getBankRefId());
            dto.setBankAmount(m.getBankRecord().getAmount());
            dto.setBankNarration(m.getBankRecord().getNarration());
            dto.setBankFee(m.getBankRecord().getFee());
        }

        if (m.getLedgerRecord() != null) {
            dto.setLedgerId(m.getLedgerRecord().getId());
            dto.setLedgerEntryId(m.getLedgerRecord().getLedgerEntryId());
            dto.setLedgerInternalRef(m.getLedgerRecord().getInternalRef());
            dto.setLedgerAmount(m.getLedgerRecord().getAmount());
            dto.setLedgerDescription(m.getLedgerRecord().getDescription());
        }

        dto.setMatchStatus(m.getMatchStatus());
        dto.setMatchPass(m.getMatchPass());
        dto.setRuleFired(m.getRuleFired());
        dto.setConfidence(m.getConfidence());
        dto.setCaseType(m.getCaseType());
        dto.setReasoning(m.getReasoning());
        dto.setSuggestedMatchId(m.getSuggestedMatchId());
        dto.setMatchedAmount(m.getMatchedAmount());
        dto.setDiscrepancyAmount(m.getDiscrepancyAmount());
        dto.setCreatedAt(m.getCreatedAt());
        return dto;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getBatchId() { return batchId; }
    public void setBatchId(Long batchId) { this.batchId = batchId; }

    public Long getGatewayId() { return gatewayId; }
    public void setGatewayId(Long gatewayId) { this.gatewayId = gatewayId; }

    public String getGatewayTxnId() { return gatewayTxnId; }
    public void setGatewayTxnId(String gatewayTxnId) { this.gatewayTxnId = gatewayTxnId; }

    public BigDecimal getGatewayAmount() { return gatewayAmount; }
    public void setGatewayAmount(BigDecimal gatewayAmount) { this.gatewayAmount = gatewayAmount; }

    public String getGatewayOrderId() { return gatewayOrderId; }
    public void setGatewayOrderId(String gatewayOrderId) { this.gatewayOrderId = gatewayOrderId; }

    public String getGatewayCustomerEmail() { return gatewayCustomerEmail; }
    public void setGatewayCustomerEmail(String gatewayCustomerEmail) { this.gatewayCustomerEmail = gatewayCustomerEmail; }

    public Long getBankId() { return bankId; }
    public void setBankId(Long bankId) { this.bankId = bankId; }

    public String getBankRefId() { return bankRefId; }
    public void setBankRefId(String bankRefId) { this.bankRefId = bankRefId; }

    public BigDecimal getBankAmount() { return bankAmount; }
    public void setBankAmount(BigDecimal bankAmount) { this.bankAmount = bankAmount; }

    public String getBankNarration() { return bankNarration; }
    public void setBankNarration(String bankNarration) { this.bankNarration = bankNarration; }

    public BigDecimal getBankFee() { return bankFee; }
    public void setBankFee(BigDecimal bankFee) { this.bankFee = bankFee; }

    public Long getLedgerId() { return ledgerId; }
    public void setLedgerId(Long ledgerId) { this.ledgerId = ledgerId; }

    public String getLedgerEntryId() { return ledgerEntryId; }
    public void setLedgerEntryId(String ledgerEntryId) { this.ledgerEntryId = ledgerEntryId; }

    public String getLedgerInternalRef() { return ledgerInternalRef; }
    public void setLedgerInternalRef(String ledgerInternalRef) { this.ledgerInternalRef = ledgerInternalRef; }

    public BigDecimal getLedgerAmount() { return ledgerAmount; }
    public void setLedgerAmount(BigDecimal ledgerAmount) { this.ledgerAmount = ledgerAmount; }

    public String getLedgerDescription() { return ledgerDescription; }
    public void setLedgerDescription(String ledgerDescription) { this.ledgerDescription = ledgerDescription; }

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
