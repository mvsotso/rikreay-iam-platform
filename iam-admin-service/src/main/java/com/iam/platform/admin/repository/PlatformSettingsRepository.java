package com.iam.platform.admin.repository;

import com.iam.platform.admin.entity.PlatformSettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PlatformSettingsRepository extends JpaRepository<PlatformSettings, UUID> {

    Optional<PlatformSettings> findBySettingKey(String settingKey);

    List<PlatformSettings> findByCategory(String category);

    boolean existsBySettingKey(String settingKey);
}
