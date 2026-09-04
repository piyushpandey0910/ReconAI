package com.aifinance.service;

import com.aifinance.entity.*;
import com.aifinance.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class RuleMatchingServiceTest {

    @Mock private BatchRepository batchRepository;
    @Mock private GatewayRecordRepository gatewayRecordRepository;
    @Mock private BankRecordRepository bankRecordRepository;
    @Mock private LedgerRecordRepository ledgerRecordRepository;
    @Mock private MatchResultRepository matchResultRepository;
    @Mock private AuditLogEntryRepository auditLogEntryRepository;

    @InjectMocks
    private RuleMatchingService ruleMatchingService;

    private Batch batch;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        batch = new Batch();
        batch.setId(1L);
        batch.setGatewayUploaded(true);
        batch.setBankUploaded(true);
        batch.setLedgerUploaded(true);
        when(batchRepository.findById(1L)).thenReturn(Optional.of(batch));
        when(matchResultRepository.save(any(MatchResult.class))).thenAnswer(i -> i.getArgument(0));
    }

    @Test
    void testExactMatchRule() {
        GatewayRecord gw = new GatewayRecord();
        gw.setId(10L);
        gw.setTransactionId("GW_100");
        gw.setOrderId("ORD_999");
        gw.setAmount(new BigDecimal("250.00"));
        gw.setTimestamp(LocalDateTime.of(2026, 8, 15, 12, 0));

        BankRecord bank = new BankRecord();
        bank.setId(20L);
        bank.setBankRefId("BNK_200");
        bank.setAmount(new BigDecimal("250.00"));
        bank.setNarration("SETTLEMENT ORD_999 CLEAR");
        bank.setDate(LocalDate.of(2026, 8, 15));

        LedgerRecord ledger = new LedgerRecord();
        ledger.setId(30L);
        ledger.setLedgerEntryId("LED_300");
        ledger.setInternalRef("ORD_999");
        ledger.setAmount(new BigDecimal("250.00"));
        ledger.setEntryDate(LocalDate.of(2026, 8, 15));

        when(gatewayRecordRepository.findByBatchId(1L)).thenReturn(List.of(gw));
        when(bankRecordRepository.findByBatchId(1L)).thenReturn(List.of(bank));
        when(ledgerRecordRepository.findByBatchId(1L)).thenReturn(List.of(ledger));

        List<MatchResult> results = ruleMatchingService.reconcilePass1(1L);

        assertEquals(1, results.size());
        MatchResult res = results.get(0);
        assertEquals(MatchStatus.MATCHED, res.getMatchStatus());
        assertEquals("EXACT_ID_AND_AMOUNT", res.getRuleFired());
        assertEquals(1.0, res.getConfidence());
        assertEquals("EXACT_MATCH", res.getCaseType());
        verify(auditLogEntryRepository, times(1)).save(any(AuditLogEntry.class));
    }

    @Test
    void testUnmatchedFallbackToNeedsReview() {
        GatewayRecord gw = new GatewayRecord();
        gw.setId(10L);
        gw.setTransactionId("GW_ORPHAN");
        gw.setOrderId("ORD_MISSING");
        gw.setAmount(new BigDecimal("999.00"));

        when(gatewayRecordRepository.findByBatchId(1L)).thenReturn(List.of(gw));
        when(bankRecordRepository.findByBatchId(1L)).thenReturn(List.of());
        when(ledgerRecordRepository.findByBatchId(1L)).thenReturn(List.of());

        List<MatchResult> results = ruleMatchingService.reconcilePass1(1L);

        assertEquals(1, results.size());
        MatchResult res = results.get(0);
        assertEquals(MatchStatus.NEEDS_REVIEW, res.getMatchStatus());
        assertEquals("UNMATCHED_PASS1", res.getRuleFired());
        assertEquals(0.0, res.getConfidence());
    }
}
