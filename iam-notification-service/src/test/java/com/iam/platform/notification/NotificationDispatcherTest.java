package com.iam.platform.notification;

import com.iam.platform.notification.entity.NotificationLog;
import com.iam.platform.notification.enums.ChannelType;
import com.iam.platform.notification.enums.NotificationStatus;
import com.iam.platform.notification.repository.NotificationLogRepository;
import com.iam.platform.notification.service.EmailService;
import com.iam.platform.notification.service.NotificationDispatcher;
import com.iam.platform.notification.service.SmsService;
import com.iam.platform.notification.service.TelegramService;
import jakarta.mail.MessagingException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for NotificationDispatcher — dispatch logic routing
 * to the correct channel service and logging results.
 */
@ExtendWith(MockitoExtension.class)
class NotificationDispatcherTest {

    @Mock
    private EmailService emailService;

    @Mock
    private SmsService smsService;

    @Mock
    private TelegramService telegramService;

    @Mock
    private NotificationLogRepository logRepository;

    @InjectMocks
    private NotificationDispatcher dispatcher;

    @Test
    @DisplayName("dispatch EMAIL — should call emailService.sendEmail and log SENT")
    void dispatch_email_shouldCallEmailServiceAndLogSent() throws Exception {
        NotificationLog savedLog = NotificationLog.builder()
                .id(UUID.randomUUID())
                .channelType(ChannelType.EMAIL)
                .recipient("user@test.com")
                .subject("Test Subject")
                .status(NotificationStatus.PENDING)
                .build();
        when(logRepository.save(any(NotificationLog.class))).thenReturn(savedLog);
        doNothing().when(emailService).sendEmail(any(), any(), any());

        dispatcher.dispatch(ChannelType.EMAIL, "user@test.com", "Test Subject", "<p>Hello</p>", null);

        verify(emailService).sendEmail("user@test.com", "Test Subject", "<p>Hello</p>");
        // save called twice: once for PENDING, once for SENT
        verify(logRepository, times(2)).save(any(NotificationLog.class));
    }

    @Test
    @DisplayName("dispatch SMS — should call smsService.sendSms")
    void dispatch_sms_shouldCallSmsService() throws Exception {
        NotificationLog savedLog = NotificationLog.builder()
                .id(UUID.randomUUID())
                .channelType(ChannelType.SMS)
                .recipient("+855123456789")
                .status(NotificationStatus.PENDING)
                .build();
        when(logRepository.save(any(NotificationLog.class))).thenReturn(savedLog);
        doNothing().when(smsService).sendSms(any(), any());

        dispatcher.dispatch(ChannelType.SMS, "+855123456789", null, "Your OTP is 123456", null);

        verify(smsService).sendSms("+855123456789", "Your OTP is 123456");
    }

    @Test
    @DisplayName("dispatch TELEGRAM — should call telegramService.sendMessage")
    void dispatch_telegram_shouldCallTelegramService() throws Exception {
        NotificationLog savedLog = NotificationLog.builder()
                .id(UUID.randomUUID())
                .channelType(ChannelType.TELEGRAM)
                .recipient("@admin_chat")
                .status(NotificationStatus.PENDING)
                .build();
        when(logRepository.save(any(NotificationLog.class))).thenReturn(savedLog);
        doNothing().when(telegramService).sendMessage(any(), any());

        dispatcher.dispatch(ChannelType.TELEGRAM, "@admin_chat", null, "Alert triggered", null);

        verify(telegramService).sendMessage("@admin_chat", "Alert triggered");
    }

    @Test
    @DisplayName("dispatch — email failure should log FAILED with error message")
    void dispatch_emailFailure_shouldLogFailed() throws Exception {
        NotificationLog savedLog = NotificationLog.builder()
                .id(UUID.randomUUID())
                .channelType(ChannelType.EMAIL)
                .recipient("user@test.com")
                .subject("Alert")
                .status(NotificationStatus.PENDING)
                .build();
        when(logRepository.save(any(NotificationLog.class))).thenReturn(savedLog);
        doThrow(new MessagingException("SMTP connection refused"))
                .when(emailService).sendEmail(any(), any(), any());

        dispatcher.dispatch(ChannelType.EMAIL, "user@test.com", "Alert", "Body", null);

        ArgumentCaptor<NotificationLog> captor = ArgumentCaptor.forClass(NotificationLog.class);
        verify(logRepository, times(2)).save(captor.capture());

        NotificationLog finalLog = captor.getAllValues().get(1);
        assertThat(finalLog.getStatus()).isEqualTo(NotificationStatus.FAILED);
        assertThat(finalLog.getErrorMessage()).isEqualTo("SMTP connection refused");
    }

    @Test
    @DisplayName("dispatch — should set templateId when provided")
    void dispatch_withTemplateId_shouldSetTemplateId() throws Exception {
        UUID templateId = UUID.randomUUID();
        NotificationLog savedLog = NotificationLog.builder()
                .id(UUID.randomUUID())
                .channelType(ChannelType.EMAIL)
                .recipient("user@test.com")
                .templateId(templateId)
                .status(NotificationStatus.PENDING)
                .build();
        when(logRepository.save(any(NotificationLog.class))).thenReturn(savedLog);
        doNothing().when(emailService).sendEmail(any(), any(), any());

        dispatcher.dispatch(ChannelType.EMAIL, "user@test.com", "Subject", "Body", templateId);

        ArgumentCaptor<NotificationLog> captor = ArgumentCaptor.forClass(NotificationLog.class);
        verify(logRepository, times(2)).save(captor.capture());

        NotificationLog initialLog = captor.getAllValues().get(0);
        assertThat(initialLog.getTemplateId()).isEqualTo(templateId);
    }
}
