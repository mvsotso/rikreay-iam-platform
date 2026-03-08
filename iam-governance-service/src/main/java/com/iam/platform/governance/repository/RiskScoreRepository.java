package com.iam.platform.governance.repository;

import com.iam.platform.governance.entity.RiskScore;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface RiskScoreRepository extends JpaRepository<RiskScore, UUID> {

    Optional<RiskScore> findTopByUserIdOrderByCalculatedAtDesc(String userId);

    Page<RiskScore> findByScoreGreaterThanEqualOrderByScoreDesc(int threshold, Pageable pageable);
}
