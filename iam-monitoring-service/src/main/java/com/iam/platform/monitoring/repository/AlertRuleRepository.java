package com.iam.platform.monitoring.repository;

import com.iam.platform.monitoring.entity.AlertRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AlertRuleRepository extends JpaRepository<AlertRule, UUID> {

    List<AlertRule> findByEnabled(boolean enabled);

    List<AlertRule> findByServiceTarget(String serviceTarget);
}
