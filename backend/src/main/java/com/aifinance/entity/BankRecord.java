package com.aifinance.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "bank_records", indexes = {
    @Index(name = "idx_bank_batch", columnList = "batch_id"),
    @Index(name = "idx_bank_ref_id", columnList = "bankRefId")
})
public class BankRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "batch_id", nullable = false)
    private Batch batch;

    @Column(nullable = false, length = 100)
    private String bankRefId;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(nullable = false, length = 20)
    private String type; // CREDIT or DEBIT

    @Column(nullable = false, length = 500)
    private String narration;

    private LocalDate date;

    @Column(precision = 19, scale = 4)
    private BigDecimal fee;

    public BankRecord() {
        this.fee = BigDecimal.ZERO;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Batch getBatch() { return batch; }
    public void setBatch(Batch batch) { this.batch = batch; }

    public String getBankRefId() { return bankRefId; }
    public void setBankRefId(String bankRefId) { this.bankRefId = bankRefId; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getNarration() { return narration; }
    public void setNarration(String narration) { this.narration = narration; }

    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }

    public BigDecimal getFee() { return fee; }
    public void setFee(BigDecimal fee) { this.fee = fee; }
}
