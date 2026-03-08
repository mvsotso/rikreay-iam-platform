package com.iam.platform.notification.consumer;

import com.iam.platform.common.constants.KafkaTopics;
import com.iam.platform.common.dto.NotificationCommandDto;
import com.iam.platform.notification.enums.ChannelType;
import com.iam.platform.notification.service.NotificationDispatcher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationKafkaConsumer {

    private final NotificationDispatcher dispatcher;

    @KafkaListener(
            topics = KafkaTopics.NOTIFICATION_COMMANDS,
            containerFactory = "notificationKafkaListenerContainerFactory"
    )
    public void handleNotificationCommand(NotificationCommandDto command) {
        log.info("Received notification command: channel={}, recipient={}",
                command.getChannelType(), command.getRecipient());

        try {
            ChannelType channelType = mapChannelType(command.getChannelType());
            String body = command.getBody() != null ? command.getBody() : "";
            String subject = command.getSubject() != null ? command.getSubject() : "RikReay IAM Notification";

            dispatcher.dispatch(channelType, command.getRecipient(), subject, body, null);
        } catch (Exception e) {
            log.error("Failed to process notification command: recipient={}",
                    command.getRecipient(), e);
        }
    }

    @KafkaListener(
            topics = KafkaTopics.ALERT_TRIGGERS,
            containerFactory = "notificationKafkaListenerContainerFactory"
    )
    public void handleAlertTrigger(NotificationCommandDto alert) {
        log.info("Received alert trigger: channel={}, recipient={}",
                alert.getChannelType(), alert.getRecipient());

        try {
            ChannelType channelType = mapChannelType(alert.getChannelType());
            String subject = "[ALERT] " + (alert.getSubject() != null ? alert.getSubject() : "Platform Alert");
            String body = alert.getBody() != null ? alert.getBody() : "";

            dispatcher.dispatch(channelType, alert.getRecipient(), subject, body, null);
        } catch (Exception e) {
            log.error("Failed to process alert trigger: recipient={}",
                    alert.getRecipient(), e);
        }
    }

    private ChannelType mapChannelType(NotificationCommandDto.ChannelType commandType) {
        return switch (commandType) {
            case EMAIL -> ChannelType.EMAIL;
            case SMS -> ChannelType.SMS;
            case TELEGRAM -> ChannelType.TELEGRAM;
        };
    }
}
