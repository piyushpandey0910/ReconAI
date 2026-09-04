package com.aifinance.service;

import com.aifinance.entity.Batch;
import com.aifinance.entity.GatewayRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CsvParserServiceTest {

    private CsvParserService csvParserService;
    private Batch testBatch;

    @BeforeEach
    void setUp() {
        csvParserService = new CsvParserService();
        testBatch = new Batch();
        testBatch.setId(1L);
    }

    @Test
    void testParseValidGatewayCsv() {
        String csvContent = "transactionId,amount,currency,status,paymentMethod,orderId,timestamp,customerEmail\n" +
                "GW_101,150.50,USD,SUCCESS,CARD,ORD_201,2026-08-15 10:00:00,john@example.com\n" +
                "GW_102,300.00,USD,SUCCESS,ACH,ORD_202,2026-08-15 11:00:00,mary@example.com\n";

        MockMultipartFile file = new MockMultipartFile("file", "gateway.csv", "text/csv", csvContent.getBytes(StandardCharsets.UTF_8));
        List<GatewayRecord> records = csvParserService.parseGatewayCsv(file, testBatch);

        assertEquals(2, records.size());
        assertEquals("GW_101", records.get(0).getTransactionId());
        assertEquals(new BigDecimal("150.50"), records.get(0).getAmount());
        assertEquals("ORD_201", records.get(0).getOrderId());
    }

    @Test
    void testRejectEmptyFile() {
        MockMultipartFile emptyFile = new MockMultipartFile("file", "empty.csv", "text/csv", new byte[0]);
        assertThrows(ResponseStatusException.class, () -> csvParserService.parseGatewayCsv(emptyFile, testBatch));
    }

    @Test
    void testRejectMissingHeader() {
        String invalidCsv = "col1,col2\n1,2\n";
        MockMultipartFile file = new MockMultipartFile("file", "invalid.csv", "text/csv", invalidCsv.getBytes(StandardCharsets.UTF_8));
        assertThrows(ResponseStatusException.class, () -> csvParserService.parseGatewayCsv(file, testBatch));
    }
}
