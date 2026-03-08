package com.iam.platform.notification;

import com.iam.platform.notification.entity.NotificationTemplate;
import com.iam.platform.notification.repository.NotificationTemplateRepository;
import com.iam.platform.notification.service.TemplateService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
class NotificationServiceTest {

    @Autowired
    private TemplateService templateService;

    @MockitoBean
    private NotificationTemplateRepository templateRepository;

    @Test
    @DisplayName("Template rendering should replace variables with ${} syntax")
    void templateRendering() {
        NotificationTemplate template = NotificationTemplate.builder()
                .id(UUID.randomUUID())
                .name("welcome")
                .subject("Welcome")
                .bodyTemplate("Hello ${name}, your account ${accountId} is ready.")
                .build();

        when(templateRepository.findByName("welcome")).thenReturn(Optional.of(template));

        Map<String, String> vars = Map.of("name", "Sokha", "accountId", "ACC-001");
        String rendered = templateService.renderTemplate("welcome", vars);

        assertThat(rendered).contains("Sokha");
        assertThat(rendered).contains("ACC-001");
        assertThat(rendered).doesNotContain("${name}");
        assertThat(rendered).doesNotContain("${accountId}");
    }

    @Test
    @DisplayName("Template rendering with missing template should throw")
    void templateRenderingMissing() {
        when(templateRepository.findByName("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> templateService.renderTemplate("missing", Map.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Template not found");
    }

    @Test
    @DisplayName("Template rendering with no variables should return body as-is")
    void templateRenderingNoVars() {
        NotificationTemplate template = NotificationTemplate.builder()
                .id(UUID.randomUUID())
                .name("simple")
                .subject("Simple")
                .bodyTemplate("This is a simple notification.")
                .build();

        when(templateRepository.findByName("simple")).thenReturn(Optional.of(template));

        String rendered = templateService.renderTemplate("simple", Map.of());

        assertThat(rendered).isEqualTo("This is a simple notification.");
    }
}
