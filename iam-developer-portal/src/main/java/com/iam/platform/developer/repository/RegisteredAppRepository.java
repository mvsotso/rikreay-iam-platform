package com.iam.platform.developer.repository;

import com.iam.platform.developer.entity.RegisteredApp;
import com.iam.platform.developer.enums.AppStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RegisteredAppRepository extends JpaRepository<RegisteredApp, UUID> {

    List<RegisteredApp> findByOwnerId(String ownerId);

    Page<RegisteredApp> findByOwnerId(String ownerId, Pageable pageable);

    Optional<RegisteredApp> findByClientId(String clientId);

    List<RegisteredApp> findByStatus(AppStatus status);

    long countByOwnerId(String ownerId);
}
