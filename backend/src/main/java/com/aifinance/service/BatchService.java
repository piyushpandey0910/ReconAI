package com.aifinance.service;

import com.aifinance.dto.BatchDto;
import com.aifinance.dto.CsvUploadResponse;
import com.aifinance.entity.*;
import com.aifinance.repository.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class BatchService {

    private final BatchRepository batchRepository;
    private final GatewayRecordRepository gatewayRecordRepository;
    private final BankRecordRepository bankRecordRepository;
    private final LedgerRecordRepository ledgerRecordRepository;
    private final MatchResultRepository matchResultRepository;
    private final AuditLogEntryRepository auditLogEntryRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final CsvParserService csvParserService;
    private final AuthService authService;

    public BatchService(BatchRepository batchRepository,
                        GatewayRecordRepository gatewayRecordRepository,
                        BankRecordRepository bankRecordRepository,
                        LedgerRecordRepository ledgerRecordRepository,
                        MatchResultRepository matchResultRepository,
                        AuditLogEntryRepository auditLogEntryRepository,
                        ChatMessageRepository chatMessageRepository,
                        CsvParserService csvParserService,
                        AuthService authService) {
        this.batchRepository = batchRepository;
        this.gatewayRecordRepository = gatewayRecordRepository;
        this.bankRecordRepository = bankRecordRepository;
        this.ledgerRecordRepository = ledgerRecordRepository;
        this.matchResultRepository = matchResultRepository;
        this.auditLogEntryRepository = auditLogEntryRepository;
        this.chatMessageRepository = chatMessageRepository;
        this.csvParserService = csvParserService;
        this.authService = authService;
    }

    @Transactional
    public BatchDto createBatch(String batchName) {
        User user = authService.getCurrentAuthenticatedUser();
        String name = (batchName != null && !batchName.trim().isEmpty())
                ? batchName.trim()
                : "Reconciliation Batch #" + (batchRepository.count() + 1);

        Batch batch = new Batch(name, user);
        batch = batchRepository.save(batch);
        return BatchDto.fromEntity(batch);
    }

    @Transactional
    public CsvUploadResponse uploadCsv(Long batchId, String type, MultipartFile file) {
        Batch batch = batchRepository.findById(batchId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Batch not found: " + batchId));

        String cleanType = type.toLowerCase().trim();
        int rowsProcessed;

        switch (cleanType) {
            case "gateway" -> {
                gatewayRecordRepository.deleteByBatchId(batchId);
                List<GatewayRecord> records = csvParserService.parseGatewayCsv(file, batch);
                gatewayRecordRepository.saveAll(records);
                batch.setGatewayUploaded(true);
                rowsProcessed = records.size();
            }
            case "bank" -> {
                bankRecordRepository.deleteByBatchId(batchId);
                List<BankRecord> records = csvParserService.parseBankCsv(file, batch);
                bankRecordRepository.saveAll(records);
                batch.setBankUploaded(true);
                rowsProcessed = records.size();
            }
            case "ledger" -> {
                ledgerRecordRepository.deleteByBatchId(batchId);
                List<LedgerRecord> records = csvParserService.parseLedgerCsv(file, batch);
                ledgerRecordRepository.saveAll(records);
                batch.setLedgerUploaded(true);
                rowsProcessed = records.size();
            }
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Invalid file type: '" + type + "'. Expected 'gateway', 'bank', or 'ledger'");
        }

        if (batch.areAllFilesUploaded()) {
            batch.setStatus(BatchStatus.READY_FOR_RULES);
            int gwCount = gatewayRecordRepository.findByBatchId(batchId).size();
            batch.setTotalRecords(gwCount);
        }

        batchRepository.save(batch);

        return new CsvUploadResponse(
                batchId,
                cleanType,
                rowsProcessed,
                "Successfully parsed and ingested " + rowsProcessed + " " + cleanType + " records",
                batch.areAllFilesUploaded()
        );
    }

    public Batch getBatchEntity(Long batchId) {
        return batchRepository.findById(batchId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Batch not found: " + batchId));
    }

    @Transactional(readOnly = true)
    public BatchDto getBatch(Long batchId) {
        return BatchDto.fromEntity(getBatchEntity(batchId));
    }

    @Transactional(readOnly = true)
    public List<BatchDto> getAllBatches() {
        return batchRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(BatchDto::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional
    public void deleteBatch(Long batchId) {
        Batch batch = getBatchEntity(batchId);
        auditLogEntryRepository.deleteByBatchId(batchId);
        chatMessageRepository.deleteAll(chatMessageRepository.findByBatchIdOrderByTimestampAsc(batchId));
        matchResultRepository.deleteByBatchId(batchId);
        gatewayRecordRepository.deleteByBatchId(batchId);
        bankRecordRepository.deleteByBatchId(batchId);
        ledgerRecordRepository.deleteByBatchId(batchId);
        batchRepository.delete(batch);
    }
}
