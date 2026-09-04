package com.aifinance.repository;

import com.aifinance.entity.LedgerRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LedgerRecordRepository extends JpaRepository<LedgerRecord, Long> {
    List<LedgerRecord> findByBatchId(Long batchId);
    void deleteByBatchId(Long batchId);
}
