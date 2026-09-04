package com.aifinance.repository;

import com.aifinance.entity.BankRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BankRecordRepository extends JpaRepository<BankRecord, Long> {
    List<BankRecord> findByBatchId(Long batchId);
    void deleteByBatchId(Long batchId);
}
