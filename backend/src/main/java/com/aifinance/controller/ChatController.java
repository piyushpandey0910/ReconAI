package com.aifinance.controller;

import com.aifinance.dto.ChatMessageDto;
import com.aifinance.dto.ChatRequestDto;
import com.aifinance.dto.ChatResponseDto;
import com.aifinance.service.ChatService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/batches/{batchId}/chat")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'ANALYST', 'VIEWER')")
    public ResponseEntity<ChatResponseDto> chat(
            @PathVariable("batchId") Long batchId,
            @Valid @RequestBody ChatRequestDto request) {
        return ResponseEntity.ok(chatService.chat(batchId, request));
    }

    @GetMapping("/history")
    @PreAuthorize("hasAnyRole('ADMIN', 'ANALYST', 'VIEWER')")
    public ResponseEntity<List<ChatMessageDto>> getHistory(@PathVariable("batchId") Long batchId) {
        return ResponseEntity.ok(chatService.getChatHistory(batchId));
    }
}
