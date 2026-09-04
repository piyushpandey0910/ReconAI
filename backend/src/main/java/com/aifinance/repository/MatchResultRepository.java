package com.aifinance.repository;

import com.aifinance.entity.MatchPass;
import com.aifinance.entity.MatchResult;
import com.aifinance.entity.MatchStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MatchResultRepository extends JpaRepository<MatchResult, Long> {
    List<MatchResult> findByBatchId(Long batchId);
    List<MatchResult> findByBatchIdAndMatchStatus(Long batchId, MatchStatus status);
    List<MatchResult> findByBatchIdAndMatchPass(Long batchId, MatchPass pass);
    void deleteByBatchId(Long batchId);

    @Query("SELECT COUNT(m) FROM MatchResult m WHERE m.batch.id = :batchId AND m.matchStatus = :status")
    long countByBatchIdAndStatus(@Param("batchId") Long batchId, @Param("status") MatchStatus status);

    @Query("SELECT COUNT(m) FROM MatchResult m WHERE m.batch.id = :batchId AND m.matchPass = :pass")
    long countByBatchIdAndPass(@Param("batchId") Long batchId, @Param("pass") MatchPass pass);
}
