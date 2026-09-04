package com.aifinance.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "ledger_records", indexes = {
    @Index(name = "idx_ledger_batch", columnList = "batch_id"),
    @Index(name = "idx_ledger_entry_id", columnList = "ledgerEntryId"),
    @Index(name = "idx_ledger_internal_ref", columnList = "internalRef")
})
public class LedgerRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "batch_id", nullable = false)
    private Batch batch;

    @Column(nullable = false, length = 100)
    private String ledgerEntryId;

    @Column(nullable = false, length = 100)
    private String internalRef;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(length = 50)
    private String accountCode;

    @Column(length = 500)
    private String description;

    private LocalDate entryDate;

    public LedgerRecord() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Batch getBatch() { return batch; }
    public void setBatch(Batch batch) { this.batch = batch; }

    public String getLedgerEntryId() { return ledgerEntryId; }
    public void setLedgerEntryId(String ledgerEntryId) { this.ledgerEntryId = ledgerEntryId; }

    public String getInternalRef() { return internalRef; }
    public void setInternalRef(String internalRef) { this.internalRef = internalRef; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getAccountCode() { return accountCode; }
    public void setAccountCode(String accountCode) { this.accountCode = accountCode; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public LocalDate getEntryDate() { return entryDate; }
    public void setEntryDate(LocalDate entryDate) { this.entryDate = entryDate; }
}
