package com.aifinance.repository;

import com.aifinance.entity.GatewayRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GatewayRecordRepository extends JpaRepository<GatewayRecord, Long> {
    List<GatewayRecord> findByBatchId(Long batchId);
    void deleteByBatchId(Long batchId);
}
