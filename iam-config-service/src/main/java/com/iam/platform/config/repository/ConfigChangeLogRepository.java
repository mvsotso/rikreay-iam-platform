package com.iam.platform.config.repository;

import com.iam.platform.config.entity.ConfigChangeLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ConfigChangeLogRepository extends JpaRepository<ConfigChangeLog, UUID> {

    Page<ConfigChangeLog> findByApplicationAndProfileOrderByVersionDesc(
            String application, String profile, Pageable pageable);

    Page<ConfigChangeLog> findAllByOrderByCreatedAtDesc(Pageable pageable);

    Optional<ConfigChangeLog> findByVersion(Long version);

    @Query(value = "SELECT nextval('config_version_seq')", nativeQuery = true)
    Long getNextVersion();

    Optional<ConfigChangeLog> findTopByApplicationAndProfileOrderByVersionDesc(
            String application, String profile);
}
