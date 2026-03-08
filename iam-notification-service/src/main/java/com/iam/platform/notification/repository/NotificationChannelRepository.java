package com.iam.platform.notification.repository;

import com.iam.platform.notification.entity.NotificationChannel;
import com.iam.platform.notification.enums.ChannelType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface NotificationChannelRepository extends JpaRepository<NotificationChannel, UUID> {

    List<NotificationChannel> findByChannelType(ChannelType channelType);

    List<NotificationChannel> findByEnabledTrue();

    Optional<NotificationChannel> findByChannelName(String channelName);

    Optional<NotificationChannel> findByChannelTypeAndEnabledTrue(ChannelType channelType);

    boolean existsByChannelName(String channelName);
}
