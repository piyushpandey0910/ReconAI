package com.aifinance.dto;

public class CsvUploadResponse {
    private Long batchId;
    private String fileType;
    private int rowsProcessed;
    private String message;
    private boolean allFilesUploaded;

    public CsvUploadResponse() {}

    public CsvUploadResponse(Long batchId, String fileType, int rowsProcessed, String message, boolean allFilesUploaded) {
        this.batchId = batchId;
        this.fileType = fileType;
        this.rowsProcessed = rowsProcessed;
        this.message = message;
        this.allFilesUploaded = allFilesUploaded;
    }

    public Long getBatchId() { return batchId; }
    public void setBatchId(Long batchId) { this.batchId = batchId; }

    public String getFileType() { return fileType; }
    public void setFileType(String fileType) { this.fileType = fileType; }

    public int getRowsProcessed() { return rowsProcessed; }
    public void setRowsProcessed(int rowsProcessed) { this.rowsProcessed = rowsProcessed; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public boolean isAllFilesUploaded() { return allFilesUploaded; }
    public void setAllFilesUploaded(boolean allFilesUploaded) { this.allFilesUploaded = allFilesUploaded; }
}
