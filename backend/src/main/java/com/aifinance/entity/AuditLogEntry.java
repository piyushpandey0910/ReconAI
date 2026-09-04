package com.aifinance.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "audit_log_entries", indexes = {
    @Index(name = "idx_audit_batch", columnList = "batch_id"),
    @Index(name = "idx_audit_source", columnList = "source")
})
public class AuditLogEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "batch_id", nullable = false)
    private Batch batch;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "match_result_id")
    private MatchResult matchResult;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private AuditSource source;

    @Column(nullable = false, length = 100)
    private String action;

    @Column(length = 100)
    private String ruleOrModel;

    private Double confidence;

    @Column(columnDefinition = "TEXT")
    private String inputSnapshot;

    @Column(columnDefinition = "TEXT")
    private String outputSnapshot;

    @Column(columnDefinition = "TEXT")
    private String reasoning;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    public AuditLogEntry() {
        this.timestamp = LocalDateTime.now();
    }

    public AuditLogEntry(Batch batch, MatchResult matchResult, AuditSource source, String action,
                         String ruleOrModel, Double confidence, String inputSnapshot,
                         String outputSnapshot, String reasoning) {
        this();
        this.batch = batch;
        this.matchResult = matchResult;
        this.source = source;
        this.action = action;
        this.ruleOrModel = ruleOrModel;
        this.confidence = confidence;
        this.inputSnapshot = inputSnapshot;
        this.outputSnapshot = outputSnapshot;
        this.reasoning = reasoning;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Batch getBatch() { return batch; }
    public void setBatch(Batch batch) { this.batch = batch; }

    public MatchResult getMatchResult() { return matchResult; }
    public void setMatchResult(MatchResult matchResult) { this.matchResult = matchResult; }

    public AuditSource getSource() { return source; }
    public void setSource(AuditSource source) { this.source = source; }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public String getRuleOrModel() { return ruleOrModel; }
    public void setRuleOrModel(String ruleOrModel) { this.ruleOrModel = ruleOrModel; }

    public Double getConfidence() { return confidence; }
    public void setConfidence(Double confidence) { this.confidence = confidence; }

    public String getInputSnapshot() { return inputSnapshot; }
    public void setInputSnapshot(String inputSnapshot) { this.inputSnapshot = inputSnapshot; }

    public String getOutputSnapshot() { return outputSnapshot; }
    public void setOutputSnapshot(String outputSnapshot) { this.outputSnapshot = outputSnapshot; }

    public String getReasoning() { return reasoning; }
    public void setReasoning(String reasoning) { this.reasoning = reasoning; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
}
