package com.iam.platform.config.repository;

import com.iam.platform.config.entity.FeatureFlag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FeatureFlagRepository extends JpaRepository<FeatureFlag, UUID> {

    Optional<FeatureFlag> findByFlagKeyAndEnvironment(String flagKey, String environment);

    List<FeatureFlag> findByEnvironment(String environment);

    List<FeatureFlag> findByEnabledAndEnvironment(boolean enabled, String environment);

    Page<FeatureFlag> findByEnvironment(String environment, Pageable pageable);

    boolean existsByFlagKeyAndEnvironment(String flagKey, String environment);
}
