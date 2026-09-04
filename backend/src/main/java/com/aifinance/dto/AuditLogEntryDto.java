package com.aifinance.dto;

import com.aifinance.entity.AuditLogEntry;
import com.aifinance.entity.AuditSource;

import java.time.LocalDateTime;

public class AuditLogEntryDto {
    private Long id;
    private Long batchId;
    private Long matchResultId;
    private AuditSource source;
    private String action;
    private String ruleOrModel;
    private Double confidence;
    private String inputSnapshot;
    private String outputSnapshot;
    private String reasoning;
    private LocalDateTime timestamp;

    public AuditLogEntryDto() {}

    public static AuditLogEntryDto fromEntity(AuditLogEntry entry) {
        AuditLogEntryDto dto = new AuditLogEntryDto();
        dto.setId(entry.getId());
        dto.setBatchId(entry.getBatch() != null ? entry.getBatch().getId() : null);
        dto.setMatchResultId(entry.getMatchResult() != null ? entry.getMatchResult().getId() : null);
        dto.setSource(entry.getSource());
        dto.setAction(entry.getAction());
        dto.setRuleOrModel(entry.getRuleOrModel());
        dto.setConfidence(entry.getConfidence());
        dto.setInputSnapshot(entry.getInputSnapshot());
        dto.setOutputSnapshot(entry.getOutputSnapshot());
        dto.setReasoning(entry.getReasoning());
        dto.setTimestamp(entry.getTimestamp());
        return dto;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getBatchId() { return batchId; }
    public void setBatchId(Long batchId) { this.batchId = batchId; }

    public Long getMatchResultId() { return matchResultId; }
    public void setMatchResultId(Long matchResultId) { this.matchResultId = matchResultId; }

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
