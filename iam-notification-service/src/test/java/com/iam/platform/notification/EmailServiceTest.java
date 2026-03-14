package com.iam.platform.notification;

import com.iam.platform.notification.service.EmailService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for EmailService — mocked SMTP transport.
 * Validates email sending, template rendering, and error handling.
 */
@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private TemplateEngine templateEngine;

    @InjectMocks
    private EmailService emailService;

    @Test
    @DisplayName("sendEmail — should compose and send MimeMessage")
    void sendEmail_shouldSendMimeMessage() throws Exception {
        MimeMessage mimeMessage = mock(MimeMessage.class);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        emailService.sendEmail("admin@rikreay.gov.kh", "Test Alert", "<h1>Alert</h1>");

        verify(mailSender).send(mimeMessage);
    }

    @Test
    @DisplayName("sendEmail — SMTP failure should propagate MessagingException")
    void sendEmail_smtpFailure_shouldThrow() throws Exception {
        MimeMessage mimeMessage = mock(MimeMessage.class);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        doThrow(new RuntimeException("Connection refused")).when(mailSender).send(any(MimeMessage.class));

        assertThatThrownBy(() ->
                emailService.sendEmail("user@test.com", "Subject", "<p>Body</p>"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Connection refused");
    }

    @Test
    @DisplayName("renderTemplate — should delegate to Thymeleaf engine")
    void renderTemplate_shouldDelegateToThymeleaf() {
        when(templateEngine.process(eq("welcome"), any(Context.class)))
                .thenReturn("<p>Welcome, John!</p>");

        String result = emailService.renderTemplate("welcome", Map.of("name", "John"));

        assertThat(result).isEqualTo("<p>Welcome, John!</p>");
        verify(templateEngine).process(eq("welcome"), any(Context.class));
    }

    @Test
    @DisplayName("sendTemplatedEmail — should render template then send")
    void sendTemplatedEmail_shouldRenderAndSend() throws Exception {
        MimeMessage mimeMessage = mock(MimeMessage.class);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        when(templateEngine.process(eq("alert-template"), any(Context.class)))
                .thenReturn("<p>Alert: High CPU</p>");

        emailService.sendTemplatedEmail("ops@rikreay.gov.kh", "System Alert",
                "alert-template", Map.of("issue", "High CPU"));

        verify(templateEngine).process(eq("alert-template"), any(Context.class));
        verify(mailSender).send(mimeMessage);
    }

    @Test
    @DisplayName("renderTemplate — empty variables map should work")
    void renderTemplate_emptyVariables_shouldWork() {
        when(templateEngine.process(eq("simple"), any(Context.class)))
                .thenReturn("<p>Simple template</p>");

        String result = emailService.renderTemplate("simple", Map.of());

        assertThat(result).isEqualTo("<p>Simple template</p>");
    }
}
