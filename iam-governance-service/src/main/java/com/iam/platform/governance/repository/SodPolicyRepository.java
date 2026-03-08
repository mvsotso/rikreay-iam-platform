package com.iam.platform.governance.repository;

import com.iam.platform.governance.entity.SodPolicy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SodPolicyRepository extends JpaRepository<SodPolicy, UUID> {

    List<SodPolicy> findByEnabled(boolean enabled);
}
