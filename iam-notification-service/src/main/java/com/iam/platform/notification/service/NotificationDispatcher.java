package com.iam.platform.notification.service;

import com.iam.platform.notification.entity.NotificationLog;
import com.iam.platform.notification.enums.ChannelType;
import com.iam.platform.notification.enums.NotificationStatus;
import com.iam.platform.notification.repository.NotificationLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationDispatcher {

    private final EmailService emailService;
    private final SmsService smsService;
    private final TelegramService telegramService;
    private final NotificationLogRepository logRepository;

    public void dispatch(ChannelType channelType, String recipient, String subject,
                          String body, UUID templateId) {
        NotificationLog notificationLog = NotificationLog.builder()
                .templateId(templateId)
                .channelType(channelType)
                .recipient(recipient)
                .subject(subject)
                .status(NotificationStatus.PENDING)
                .build();
        notificationLog = logRepository.save(notificationLog);

        try {
            switch (channelType) {
                case EMAIL -> emailService.sendEmail(recipient, subject, body);
                case SMS -> smsService.sendSms(recipient, body);
                case TELEGRAM -> telegramService.sendMessage(recipient, body);
            }

            notificationLog.setStatus(NotificationStatus.SENT);
            notificationLog.setSentAt(Instant.now());
            log.info("Notification sent via {}: recipient={}", channelType, recipient);
        } catch (Exception e) {
            notificationLog.setStatus(NotificationStatus.FAILED);
            notificationLog.setErrorMessage(e.getMessage());
            log.error("Notification failed via {}: recipient={}", channelType, recipient, e);
        }

        logRepository.save(notificationLog);
    }

    public void dispatchWithTemplate(ChannelType channelType, String recipient, String subject,
                                       String templateName, Map<String, String> variables,
                                       TemplateService templateService) {
        String renderedBody = templateService.renderTemplate(templateName, variables);
        dispatch(channelType, recipient, subject, renderedBody, null);
    }
}
