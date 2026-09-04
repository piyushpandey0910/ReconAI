package com.aifinance.repository;

import com.aifinance.entity.AuditLogEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuditLogEntryRepository extends JpaRepository<AuditLogEntry, Long> {
    List<AuditLogEntry> findByBatchIdOrderByTimestampDesc(Long batchId);
    List<AuditLogEntry> findByMatchResultIdOrderByTimestampDesc(Long matchResultId);
    void deleteByBatchId(Long batchId);
}
