package com.aifinance.dto;

import java.time.LocalDateTime;

public class ChatResponseDto {
    private Long id;
    private String answer;
    private String sender = "ASSISTANT";
    private LocalDateTime timestamp;
    private Double confidence;
    private String modelUsed;

    public ChatResponseDto() {
        this.timestamp = LocalDateTime.now();
    }

    public ChatResponseDto(String answer, String modelUsed) {
        this();
        this.answer = answer;
        this.modelUsed = modelUsed;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getAnswer() { return answer; }
    public void setAnswer(String answer) { this.answer = answer; }

    public String getSender() { return sender; }
    public void setSender(String sender) { this.sender = sender; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }

    public Double getConfidence() { return confidence; }
    public void setConfidence(Double confidence) { this.confidence = confidence; }

    public String getModelUsed() { return modelUsed; }
    public void setModelUsed(String modelUsed) { this.modelUsed = modelUsed; }
}
