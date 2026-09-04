package com.aifinance.dto;

import com.aifinance.entity.ChatMessage;
import com.aifinance.entity.MessageSender;

import java.time.LocalDateTime;

public class ChatMessageDto {
    private Long id;
    private Long batchId;
    private String username;
    private MessageSender sender;
    private String message;
    private LocalDateTime timestamp;

    public ChatMessageDto() {}

    public static ChatMessageDto fromEntity(ChatMessage msg) {
        ChatMessageDto dto = new ChatMessageDto();
        dto.setId(msg.getId());
        dto.setBatchId(msg.getBatch() != null ? msg.getBatch().getId() : null);
        dto.setUsername(msg.getUser() != null ? msg.getUser().getUsername() : "System");
        dto.setSender(msg.getSender());
        dto.setMessage(msg.getMessage());
        dto.setTimestamp(msg.getTimestamp());
        return dto;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getBatchId() { return batchId; }
    public void setBatchId(Long batchId) { this.batchId = batchId; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public MessageSender getSender() { return sender; }
    public void setSender(MessageSender sender) { this.sender = sender; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
}
