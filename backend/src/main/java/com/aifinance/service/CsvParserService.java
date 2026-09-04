package com.aifinance.service;

import com.aifinance.entity.*;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class CsvParserService {

    private static final int MAX_ROW_LIMIT = 50000;
    private static final DateTimeFormatter[] DATE_FORMATTERS = {
            DateTimeFormatter.ISO_LOCAL_DATE,
            DateTimeFormatter.ofPattern("yyyy-MM-dd"),
            DateTimeFormatter.ofPattern("dd/MM/yyyy"),
            DateTimeFormatter.ofPattern("MM/dd/yyyy"),
            DateTimeFormatter.ofPattern("yyyy/MM/dd")
    };
    private static final DateTimeFormatter[] DATETIME_FORMATTERS = {
            DateTimeFormatter.ISO_LOCAL_DATE_TIME,
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"),
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"),
            DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm:ss")
    };

    public void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File is empty or missing");
        }
        String filename = file.getOriginalFilename();
        if (filename == null || !filename.toLowerCase().endsWith(".csv")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File must be a valid CSV file (.csv)");
        }
    }

    public List<GatewayRecord> parseGatewayCsv(MultipartFile file, Batch batch) {
        validateFile(file);
        List<GatewayRecord> records = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8));
             CSVParser csvParser = CSVFormat.DEFAULT.builder().setHeader().setSkipHeaderRecord(true).setTrim(true).setIgnoreHeaderCase(true).build().parse(reader)) {

            Map<String, Integer> headerMap = csvParser.getHeaderMap();
            validateHeader(headerMap, "transactionid", "transaction_id", "id");
            validateHeader(headerMap, "amount");

            int rowNum = 1;
            for (CSVRecord record : csvParser) {
                rowNum++;
                if (rowNum > MAX_ROW_LIMIT) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "CSV exceeds maximum allowed row limit of " + MAX_ROW_LIMIT);
                }

                String txnId = getField(record, "transactionid", "transaction_id", "id");
                String amountStr = getField(record, "amount");
                String currency = getFieldOrDefault(record, "USD", "currency");
                String status = getFieldOrDefault(record, "SUCCESS", "status");
                String paymentMethod = getFieldOrDefault(record, "CARD", "paymentmethod", "payment_method");
                String orderId = getFieldOrDefault(record, txnId, "orderid", "order_id");
                String timestampStr = getField(record, "timestamp", "date", "created_at");
                String email = getField(record, "customeremail", "customer_email", "email");

                if (txnId == null || txnId.trim().isEmpty()) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Row " + rowNum + ": transactionId is required");
                }

                BigDecimal amount = parseBigDecimal(amountStr, rowNum, "amount");

                GatewayRecord gr = new GatewayRecord();
                gr.setBatch(batch);
                gr.setTransactionId(txnId.trim());
                gr.setAmount(amount);
                gr.setCurrency(currency != null ? currency.trim().toUpperCase() : "USD");
                gr.setStatus(status != null ? status.trim() : "SUCCESS");
                gr.setPaymentMethod(paymentMethod != null ? paymentMethod.trim() : "CARD");
                gr.setOrderId(orderId != null ? orderId.trim() : null);
                gr.setTimestamp(parseDateTime(timestampStr));
                gr.setCustomerEmail(email != null ? email.trim() : null);

                records.add(gr);
            }
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Failed to parse Gateway CSV: " + e.getMessage(), e);
        }

        return records;
    }

    public List<BankRecord> parseBankCsv(MultipartFile file, Batch batch) {
        validateFile(file);
        List<BankRecord> records = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8));
             CSVParser csvParser = CSVFormat.DEFAULT.builder().setHeader().setSkipHeaderRecord(true).setTrim(true).setIgnoreHeaderCase(true).build().parse(reader)) {

            Map<String, Integer> headerMap = csvParser.getHeaderMap();
            validateHeader(headerMap, "bankrefid", "bank_ref_id", "ref_id", "reference");
            validateHeader(headerMap, "amount");
            validateHeader(headerMap, "narration", "description");

            int rowNum = 1;
            for (CSVRecord record : csvParser) {
                rowNum++;
                if (rowNum > MAX_ROW_LIMIT) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "CSV exceeds maximum allowed row limit of " + MAX_ROW_LIMIT);
                }

                String refId = getField(record, "bankrefid", "bank_ref_id", "ref_id", "reference");
                String amountStr = getField(record, "amount");
                String type = getFieldOrDefault(record, "CREDIT", "type");
                String narration = getField(record, "narration", "description");
                String dateStr = getField(record, "date", "entry_date");
                String feeStr = getFieldOrDefault(record, "0.0", "fee", "bank_fee");

                if (refId == null || refId.trim().isEmpty()) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Row " + rowNum + ": bankRefId is required");
                }
                if (narration == null || narration.trim().isEmpty()) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Row " + rowNum + ": narration is required");
                }

                BigDecimal amount = parseBigDecimal(amountStr, rowNum, "amount");
                BigDecimal fee = parseBigDecimal(feeStr, rowNum, "fee");

                BankRecord br = new BankRecord();
                br.setBatch(batch);
                br.setBankRefId(refId.trim());
                br.setAmount(amount);
                br.setType(type != null ? type.trim().toUpperCase() : "CREDIT");
                br.setNarration(narration.trim());
                br.setDate(parseDate(dateStr));
                br.setFee(fee);

                records.add(br);
            }
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Failed to parse Bank CSV: " + e.getMessage(), e);
        }

        return records;
    }

    public List<LedgerRecord> parseLedgerCsv(MultipartFile file, Batch batch) {
        validateFile(file);
        List<LedgerRecord> records = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8));
             CSVParser csvParser = CSVFormat.DEFAULT.builder().setHeader().setSkipHeaderRecord(true).setTrim(true).setIgnoreHeaderCase(true).build().parse(reader)) {

            Map<String, Integer> headerMap = csvParser.getHeaderMap();
            validateHeader(headerMap, "ledgerentryid", "ledger_entry_id", "entry_id", "id");
            validateHeader(headerMap, "internalref", "internal_ref", "reference", "order_id");
            validateHeader(headerMap, "amount");

            int rowNum = 1;
            for (CSVRecord record : csvParser) {
                rowNum++;
                if (rowNum > MAX_ROW_LIMIT) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "CSV exceeds maximum allowed row limit of " + MAX_ROW_LIMIT);
                }

                String entryId = getField(record, "ledgerentryid", "ledger_entry_id", "entry_id", "id");
                String internalRef = getField(record, "internalref", "internal_ref", "reference", "order_id");
                String amountStr = getField(record, "amount");
                String accountCode = getFieldOrDefault(record, "1010-REVENUE", "accountcode", "account_code");
                String desc = getFieldOrDefault(record, "Ledger Entry", "description", "desc");
                String dateStr = getField(record, "entrydate", "entry_date", "date");

                if (entryId == null || entryId.trim().isEmpty()) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Row " + rowNum + ": ledgerEntryId is required");
                }
                if (internalRef == null || internalRef.trim().isEmpty()) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Row " + rowNum + ": internalRef is required");
                }

                BigDecimal amount = parseBigDecimal(amountStr, rowNum, "amount");

                LedgerRecord lr = new LedgerRecord();
                lr.setBatch(batch);
                lr.setLedgerEntryId(entryId.trim());
                lr.setInternalRef(internalRef.trim());
                lr.setAmount(amount);
                lr.setAccountCode(accountCode != null ? accountCode.trim() : null);
                lr.setDescription(desc != null ? desc.trim() : null);
                lr.setEntryDate(parseDate(dateStr));

                records.add(lr);
            }
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Failed to parse Ledger CSV: " + e.getMessage(), e);
        }

        return records;
    }

    private void validateHeader(Map<String, Integer> headerMap, String... alternatives) {
        for (String alt : alternatives) {
            for (String header : headerMap.keySet()) {
                if (header.trim().equalsIgnoreCase(alt) || header.trim().replaceAll("[_\\s-]", "").equalsIgnoreCase(alt.replaceAll("[_\\s-]", ""))) {
                    return;
                }
            }
        }
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "CSV is missing required column: '" + alternatives[0] + "'. Found headers: " + headerMap.keySet());
    }

    private String getField(CSVRecord record, String... names) {
        if (names == null || record == null || record.getParser() == null || record.getParser().getHeaderMap() == null) {
            return null;
        }
        for (String name : names) {
            if (name == null) continue;
            String cleanName = name.replaceAll("[_\\s-]", "");
            for (Map.Entry<String, Integer> entry : record.getParser().getHeaderMap().entrySet()) {
                if (entry.getKey() != null && entry.getKey().trim().replaceAll("[_\\s-]", "").equalsIgnoreCase(cleanName)) {
                    if (entry.getValue() < record.size()) {
                        String val = record.get(entry.getValue());
                        return (val != null) ? val.trim() : null;
                    }
                }
            }
        }
        return null;
    }

    private String getFieldOrDefault(CSVRecord record, String defaultVal, String... names) {
        String val = getField(record, names);
        return (val != null && !val.trim().isEmpty()) ? val : defaultVal;
    }

    private BigDecimal parseBigDecimal(String value, int rowNum, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Row " + rowNum + ": " + fieldName + " is empty");
        }
        try {
            // Remove currency symbols, commas, spaces
            String clean = value.replaceAll("[$,€£\\s]", "").trim();
            return new BigDecimal(clean);
        } catch (NumberFormatException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Row " + rowNum + ": Invalid numeric amount '" + value + "'");
        }
    }

    private LocalDate parseDate(String val) {
        if (val == null || val.trim().isEmpty()) {
            return LocalDate.now();
        }
        String clean = val.trim();
        for (DateTimeFormatter fmt : DATE_FORMATTERS) {
            try {
                return LocalDate.parse(clean, fmt);
            } catch (DateTimeParseException ignored) {}
        }
        return LocalDate.now();
    }

    private LocalDateTime parseDateTime(String val) {
        if (val == null || val.trim().isEmpty()) {
            return LocalDateTime.now();
        }
        String clean = val.trim();
        for (DateTimeFormatter fmt : DATETIME_FORMATTERS) {
            try {
                return LocalDateTime.parse(clean, fmt);
            } catch (DateTimeParseException ignored) {}
        }
        LocalDate date = parseDate(clean);
        return date.atStartOfDay();
    }
}
