package com.iam.platform.notification.repository;

import com.iam.platform.notification.entity.NotificationTemplate;
import com.iam.platform.notification.enums.ChannelType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface NotificationTemplateRepository extends JpaRepository<NotificationTemplate, UUID> {

    Optional<NotificationTemplate> findByName(String name);

    List<NotificationTemplate> findByChannelType(ChannelType channelType);

    Page<NotificationTemplate> findByChannelType(ChannelType channelType, Pageable pageable);

    boolean existsByName(String name);
}
