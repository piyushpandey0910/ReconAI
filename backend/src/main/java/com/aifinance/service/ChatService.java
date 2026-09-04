package com.aifinance.service;

import com.aifinance.dto.BatchMetricsDto;
import com.aifinance.dto.ChatMessageDto;
import com.aifinance.dto.ChatRequestDto;
import com.aifinance.dto.ChatResponseDto;
import com.aifinance.entity.*;
import com.aifinance.repository.ChatMessageRepository;
import com.aifinance.repository.MatchResultRepository;
import com.aifinance.security.RateLimiterService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ChatService {

    private static final Logger log = LoggerFactory.getLogger(ChatService.class);

    private final ChatMessageRepository chatMessageRepository;
    private final MatchResultRepository matchResultRepository;
    private final BatchService batchService;
    private final MetricsService metricsService;
    private final AuthService authService;
    private final RateLimiterService rateLimiterService;
    private final RestClient restClient;

    public ChatService(ChatMessageRepository chatMessageRepository,
                       MatchResultRepository matchResultRepository,
                       BatchService batchService,
                       MetricsService metricsService,
                       AuthService authService,
                       RateLimiterService rateLimiterService,
                       @Value("${ai.service.url:http://localhost:8000}") String aiServiceUrl,
                       @Value("${ai.service.timeout-seconds:15}") int timeoutSeconds) {

        this.chatMessageRepository = chatMessageRepository;
        this.matchResultRepository = matchResultRepository;
        this.batchService = batchService;
        this.metricsService = metricsService;
        this.authService = authService;
        this.rateLimiterService = rateLimiterService;

        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(timeoutSeconds));
        requestFactory.setReadTimeout(Duration.ofSeconds(timeoutSeconds));

        this.restClient = RestClient.builder()
                .baseUrl(aiServiceUrl)
                .requestFactory(requestFactory)
                .build();
    }

    @Transactional
    public ChatResponseDto chat(Long batchId, ChatRequestDto request) {
        User user = authService.getCurrentAuthenticatedUser();
        if (!rateLimiterService.allowAiRequest(user.getUsername())) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                    "AI chat rate limit exceeded. Please wait a moment before sending more messages.");
        }

        Batch batch = batchService.getBatchEntity(batchId);
        BatchMetricsDto metrics = metricsService.getBatchMetrics(batchId);

        // Fetch recent exceptions and sample manual review items to ground the prompt
        List<MatchResult> exceptions = matchResultRepository.findByBatchIdAndMatchStatus(batchId, MatchStatus.MANUAL_REVIEW);
        if (exceptions.isEmpty()) {
            exceptions = matchResultRepository.findByBatchIdAndMatchStatus(batchId, MatchStatus.NEEDS_REVIEW);
        }

        List<Map<String, Object>> sampleExceptions = new ArrayList<>();
        for (MatchResult m : exceptions.stream().limit(15).toList()) {
            Map<String, Object> map = new HashMap<>();
            if (m.getGatewayRecord() != null) {
                map.put("transaction_id", m.getGatewayRecord().getTransactionId());
                map.put("order_id", m.getGatewayRecord().getOrderId());
                map.put("amount", m.getGatewayRecord().getAmount());
            }
            map.put("status", m.getMatchStatus().name());
            map.put("case_type", m.getCaseType());
            map.put("reasoning", m.getReasoning());
            map.put("confidence", m.getConfidence());
            sampleExceptions.add(map);
        }

        Map<String, Object> batchSummary = new HashMap<>();
        batchSummary.put("batch_id", batch.getId());
        batchSummary.put("batch_name", batch.getBatchName());
        batchSummary.put("status", batch.getStatus().name());
        batchSummary.put("total_records", metrics.getTotalRecords());
        batchSummary.put("matched_count", metrics.getMatchedCount());
        batchSummary.put("pass1_matched_count", metrics.getPass1MatchedCount());
        batchSummary.put("pass2_matched_count", metrics.getPass2MatchedCount());
        batchSummary.put("manual_review_count", metrics.getManualReviewCount());
        batchSummary.put("match_rate_percentage", metrics.getMatchRatePercentage());
        batchSummary.put("total_reconciled_amount", metrics.getTotalReconciledAmount());
        batchSummary.put("total_discrepancy_amount", metrics.getTotalDiscrepancyAmount());
        batchSummary.put("exception_breakdown", metrics.getExceptionBreakdown());
        batchSummary.put("sample_exceptions", sampleExceptions);

        // Fetch history
        List<ChatMessage> history = chatMessageRepository.findByBatchIdAndUserIdOrderByTimestampAsc(batchId, user.getId());
        List<Map<String, String>> historyPayload = history.stream().map(h -> {
            Map<String, String> m = new HashMap<>();
            m.put("role", h.getSender() == MessageSender.USER ? "user" : "assistant");
            m.put("content", h.getMessage());
            return m;
        }).collect(Collectors.toList());

        // Save User Message
        ChatMessage userMsg = new ChatMessage(batch, user, MessageSender.USER, request.getMessage(), null);
        chatMessageRepository.save(userMsg);

        // Forward to Python AI microservice
        Map<String, Object> aiRequestBody = new HashMap<>();
        aiRequestBody.put("question", request.getMessage());
        aiRequestBody.put("batch_summary", batchSummary);
        aiRequestBody.put("conversation_history", historyPayload);

        ChatResponseDto responseDto = new ChatResponseDto();
        try {
            log.info("Forwarding chat question to AI microservice for batch {}: '{}'", batchId, request.getMessage());
            Map<String, Object> aiResponse = restClient.post()
                    .uri("/chat")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(aiRequestBody)
                    .retrieve()
                    .body(new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {});

            String answer = (aiResponse != null && aiResponse.containsKey("answer"))
                    ? (String) aiResponse.get("answer")
                    : "I was unable to retrieve an answer for this batch.";
            String model = (aiResponse != null && aiResponse.containsKey("model_used"))
                    ? (String) aiResponse.get("model_used")
                    : "Groq-LLM";
            Double conf = (aiResponse != null && aiResponse.containsKey("confidence"))
                    ? ((Number) aiResponse.get("confidence")).doubleValue()
                    : 1.0;

            responseDto.setAnswer(answer);
            responseDto.setModelUsed(model);
            responseDto.setConfidence(conf);
        } catch (Exception e) {
            log.warn("AI Chat service failed: {}. Falling back to internal grounded answer.", e.getMessage());
            String fallback = "Currently unable to reach the AI service (" + e.getMessage() +
                    "). For Batch '" + batch.getBatchName() + "', " + metrics.getMatchedCount() +
                    " of " + metrics.getTotalRecords() + " records have matched (" +
                    metrics.getMatchRatePercentage() + "% match rate). Reconciled amount: $" +
                    metrics.getTotalReconciledAmount() + ".";
            responseDto.setAnswer(fallback);
            responseDto.setModelUsed("Internal-Grounded-Fallback");
            responseDto.setConfidence(0.9);
        }

        // Save Assistant Message
        ChatMessage assistantMsg = new ChatMessage(batch, user, MessageSender.ASSISTANT, responseDto.getAnswer(), batchSummary.toString());
        assistantMsg = chatMessageRepository.save(assistantMsg);
        responseDto.setId(assistantMsg.getId());

        return responseDto;
    }

    public List<ChatMessageDto> getChatHistory(Long batchId) {
        User user = authService.getCurrentAuthenticatedUser();
        List<ChatMessage> list = chatMessageRepository.findByBatchIdAndUserIdOrderByTimestampAsc(batchId, user.getId());
        return list.stream().map(ChatMessageDto::fromEntity).collect(Collectors.toList());
    }
}
