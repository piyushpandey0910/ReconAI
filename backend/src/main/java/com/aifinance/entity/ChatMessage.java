package com.aifinance.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "chat_messages", indexes = {
    @Index(name = "idx_chat_batch_user", columnList = "batch_id, user_id")
})
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "batch_id", nullable = false)
    private Batch batch;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MessageSender sender;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    @Column(columnDefinition = "TEXT")
    private String promptContextSnapshot;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    public ChatMessage() {
        this.timestamp = LocalDateTime.now();
    }

    public ChatMessage(Batch batch, User user, MessageSender sender, String message, String promptContextSnapshot) {
        this();
        this.batch = batch;
        this.user = user;
        this.sender = sender;
        this.message = message;
        this.promptContextSnapshot = promptContextSnapshot;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Batch getBatch() { return batch; }
    public void setBatch(Batch batch) { this.batch = batch; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public MessageSender getSender() { return sender; }
    public void setSender(MessageSender sender) { this.sender = sender; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getPromptContextSnapshot() { return promptContextSnapshot; }
    public void setPromptContextSnapshot(String promptContextSnapshot) { this.promptContextSnapshot = promptContextSnapshot; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
}
