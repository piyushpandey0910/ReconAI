package com.aifinance.service;

import com.aifinance.dto.BatchMetricsDto;
import com.aifinance.entity.Batch;
import com.aifinance.entity.MatchResult;
import com.aifinance.entity.MatchStatus;
import com.aifinance.repository.BatchRepository;
import com.aifinance.repository.MatchResultRepository;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

@Service
public class ReportService {

    private final BatchRepository batchRepository;
    private final MatchResultRepository matchResultRepository;
    private final MetricsService metricsService;

    public ReportService(BatchRepository batchRepository,
                         MatchResultRepository matchResultRepository,
                         MetricsService metricsService) {
        this.batchRepository = batchRepository;
        this.matchResultRepository = matchResultRepository;
        this.metricsService = metricsService;
    }

    public byte[] generateExcelReport(Long batchId) {
        Batch batch = batchRepository.findById(batchId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Batch not found: " + batchId));

        BatchMetricsDto metrics = metricsService.getBatchMetrics(batchId);
        List<MatchResult> allRecords = matchResultRepository.findByBatchId(batchId);

        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            // Cell Styles
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle titleStyle = createTitleStyle(workbook);
            CellStyle dataStyle = workbook.createCellStyle();
            dataStyle.setBorderBottom(BorderStyle.THIN);
            dataStyle.setBorderTop(BorderStyle.THIN);
            dataStyle.setBorderLeft(BorderStyle.THIN);
            dataStyle.setBorderRight(BorderStyle.THIN);

            // Sheet 1: Executive Summary
            createSummarySheet(workbook, batch, metrics, titleStyle, headerStyle, dataStyle);

            // Sheet 2: Matched Records
            createRecordsSheet(workbook, "Matched Records",
                    allRecords.stream().filter(r -> r.getMatchStatus() == MatchStatus.MATCHED).toList(),
                    headerStyle, dataStyle);

            // Sheet 3: Exceptions
            createRecordsSheet(workbook, "Exceptions",
                    allRecords.stream().filter(r -> r.getMatchStatus() == MatchStatus.DISCREPANCY || r.getMatchStatus() == MatchStatus.NEEDS_REVIEW).toList(),
                    headerStyle, dataStyle);

            // Sheet 4: Manual Review
            createRecordsSheet(workbook, "Manual Review Queue",
                    allRecords.stream().filter(r -> r.getMatchStatus() == MatchStatus.MANUAL_REVIEW).toList(),
                    headerStyle, dataStyle);

            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to generate Excel report: " + e.getMessage());
        }
    }

    private void createSummarySheet(Workbook wb, Batch batch, BatchMetricsDto metrics,
                                    CellStyle titleStyle, CellStyle headerStyle, CellStyle dataStyle) {
        Sheet sheet = wb.createSheet("Executive Summary");
        sheet.setColumnWidth(0, 8000);
        sheet.setColumnWidth(1, 10000);

        int rowIdx = 0;
        Row titleRow = sheet.createRow(rowIdx++);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue("FINANCIAL RECONCILIATION REPORT");
        titleCell.setCellStyle(titleStyle);
        rowIdx++;

        addSummaryRow(sheet, rowIdx++, "Batch Name", batch.getBatchName(), headerStyle, dataStyle);
        addSummaryRow(sheet, rowIdx++, "Batch Status", batch.getStatus().name(), headerStyle, dataStyle);
        addSummaryRow(sheet, rowIdx++, "Uploaded By", batch.getUploadedBy() != null ? batch.getUploadedBy().getUsername() : "N/A", headerStyle, dataStyle);
        addSummaryRow(sheet, rowIdx++, "Created At", batch.getCreatedAt().toString(), headerStyle, dataStyle);
        rowIdx++;

        addSummaryRow(sheet, rowIdx++, "Total Records Processed", String.valueOf(metrics.getTotalRecords()), headerStyle, dataStyle);
        addSummaryRow(sheet, rowIdx++, "Overall Match Rate", metrics.getMatchRatePercentage() + "%", headerStyle, dataStyle);
        addSummaryRow(sheet, rowIdx++, "Pass 1 Matched (Rules)", String.valueOf(metrics.getPass1MatchedCount()), headerStyle, dataStyle);
        addSummaryRow(sheet, rowIdx++, "Pass 2 Matched (Groq AI)", String.valueOf(metrics.getPass2MatchedCount()), headerStyle, dataStyle);
        addSummaryRow(sheet, rowIdx++, "Manual Review Pending", String.valueOf(metrics.getManualReviewCount()), headerStyle, dataStyle);
        addSummaryRow(sheet, rowIdx++, "Total Reconciled Volume", "$" + metrics.getTotalReconciledAmount(), headerStyle, dataStyle);
        addSummaryRow(sheet, rowIdx++, "Unresolved Discrepancies", "$" + metrics.getTotalDiscrepancyAmount(), headerStyle, dataStyle);
    }

    private void addSummaryRow(Sheet sheet, int rowIdx, String label, String value, CellStyle hStyle, CellStyle dStyle) {
        Row row = sheet.createRow(rowIdx);
        Cell c0 = row.createCell(0);
        c0.setCellValue(label);
        c0.setCellStyle(hStyle);

        Cell c1 = row.createCell(1);
        c1.setCellValue(value);
        c1.setCellStyle(dStyle);
    }

    private void createRecordsSheet(Workbook wb, String sheetName, List<MatchResult> records,
                                    CellStyle headerStyle, CellStyle dataStyle) {
        Sheet sheet = wb.createSheet(sheetName);
        String[] headers = {
                "Match ID", "Status", "Pass", "Rule / Model", "Confidence",
                "Case Type", "GW Txn ID", "GW Amount", "Bank Ref ID",
                "Bank Amount", "Bank Fee", "Ledger Entry ID", "Ledger Amount",
                "Reasoning"
        };

        Row headerRow = sheet.createRow(0);
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
            sheet.setColumnWidth(i, 4500);
        }
        sheet.setColumnWidth(13, 12000); // reasoning column

        int rowIdx = 1;
        for (MatchResult r : records) {
            Row row = sheet.createRow(rowIdx++);
            int c = 0;
            createCell(row, c++, String.valueOf(r.getId()), dataStyle);
            createCell(row, c++, r.getMatchStatus().name(), dataStyle);
            createCell(row, c++, r.getMatchPass() != null ? r.getMatchPass().name() : "", dataStyle);
            createCell(row, c++, r.getRuleFired() != null ? r.getRuleFired() : "", dataStyle);
            createCell(row, c++, r.getConfidence() != null ? String.format("%.2f", r.getConfidence()) : "0.00", dataStyle);
            createCell(row, c++, r.getCaseType() != null ? r.getCaseType() : "", dataStyle);

            createCell(row, c++, r.getGatewayRecord() != null ? r.getGatewayRecord().getTransactionId() : "", dataStyle);
            createCell(row, c++, r.getGatewayRecord() != null ? "$" + r.getGatewayRecord().getAmount() : "", dataStyle);

            createCell(row, c++, r.getBankRecord() != null ? r.getBankRecord().getBankRefId() : "", dataStyle);
            createCell(row, c++, r.getBankRecord() != null ? "$" + r.getBankRecord().getAmount() : "", dataStyle);
            createCell(row, c++, r.getBankRecord() != null ? "$" + r.getBankRecord().getFee() : "$0.00", dataStyle);

            createCell(row, c++, r.getLedgerRecord() != null ? r.getLedgerRecord().getLedgerEntryId() : "", dataStyle);
            createCell(row, c++, r.getLedgerRecord() != null ? "$" + r.getLedgerRecord().getAmount() : "", dataStyle);

            createCell(row, c++, r.getReasoning() != null ? r.getReasoning() : "", dataStyle);
        }
    }

    private void createCell(Row row, int colIdx, String val, CellStyle style) {
        Cell cell = row.createCell(colIdx);
        cell.setCellValue(val);
        cell.setCellStyle(style);
    }

    private CellStyle createHeaderStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        Font font = wb.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.WHITE.getIndex());
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setAlignment(HorizontalAlignment.LEFT);
        style.setBorderBottom(BorderStyle.MEDIUM);
        return style;
    }

    private CellStyle createTitleStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        Font font = wb.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 14);
        font.setColor(IndexedColors.DARK_BLUE.getIndex());
        style.setFont(font);
        return style;
    }
}
