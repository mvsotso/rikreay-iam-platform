package com.iam.platform.governance.repository;

import com.iam.platform.governance.entity.LifecycleWorkflow;
import com.iam.platform.governance.enums.WorkflowType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface WorkflowRepository extends JpaRepository<LifecycleWorkflow, UUID> {

    List<LifecycleWorkflow> findByTypeAndEnabled(WorkflowType type, boolean enabled);
}
