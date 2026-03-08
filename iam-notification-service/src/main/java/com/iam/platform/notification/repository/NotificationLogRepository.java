package com.iam.platform.notification.repository;

import com.iam.platform.notification.entity.NotificationLog;
import com.iam.platform.notification.enums.ChannelType;
import com.iam.platform.notification.enums.NotificationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.UUID;

@Repository
public interface NotificationLogRepository extends JpaRepository<NotificationLog, UUID> {

    Page<NotificationLog> findByStatus(NotificationStatus status, Pageable pageable);

    Page<NotificationLog> findByChannelType(ChannelType channelType, Pageable pageable);

    Page<NotificationLog> findByRecipient(String recipient, Pageable pageable);

    Page<NotificationLog> findAllByOrderByCreatedAtDesc(Pageable pageable);

    long countByStatus(NotificationStatus status);

    void deleteByCreatedAtBefore(Instant cutoff);
}
