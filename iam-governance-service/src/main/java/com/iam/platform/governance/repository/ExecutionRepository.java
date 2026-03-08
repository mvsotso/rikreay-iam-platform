package com.iam.platform.governance.repository;

import com.iam.platform.governance.entity.WorkflowExecution;
import com.iam.platform.governance.enums.ExecutionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ExecutionRepository extends JpaRepository<WorkflowExecution, UUID> {

    Page<WorkflowExecution> findByStatus(ExecutionStatus status, Pageable pageable);

    Page<WorkflowExecution> findByWorkflowId(UUID workflowId, Pageable pageable);
}
