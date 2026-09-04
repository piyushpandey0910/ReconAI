package com.aifinance.repository;

import com.aifinance.entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    List<ChatMessage> findByBatchIdAndUserIdOrderByTimestampAsc(Long batchId, Long userId);
    List<ChatMessage> findByBatchIdOrderByTimestampAsc(Long batchId);
}
