package com.aifinance.dto;

import com.aifinance.entity.MatchStatus;
import jakarta.validation.constraints.NotNull;

public class ManualReviewActionRequest {
    @NotNull(message = "Match status is required")
    private MatchStatus status; // MATCHED or DISCREPANCY

    private String reasoning;
    private Long bankRecordId;
    private Long ledgerRecordId;

    public ManualReviewActionRequest() {}

    public MatchStatus getStatus() { return status; }
    public void setStatus(MatchStatus status) { this.status = status; }

    public String getReasoning() { return reasoning; }
    public void setReasoning(String reasoning) { this.reasoning = reasoning; }

    public Long getBankRecordId() { return bankRecordId; }
    public void setBankRecordId(Long bankRecordId) { this.bankRecordId = bankRecordId; }

    public Long getLedgerRecordId() { return ledgerRecordId; }
    public void setLedgerRecordId(Long ledgerRecordId) { this.ledgerRecordId = ledgerRecordId; }
}
