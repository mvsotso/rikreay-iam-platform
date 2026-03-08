package com.iam.platform.common.config;

import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Shared OpenAPI configuration with JWT bearer security scheme.
 * Auto-configured for all services via iam-common dependency.
 */
@Configuration
@SecurityScheme(
        name = "bearer-jwt",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT",
        description = "Keycloak JWT access token (realm: iam-platform)"
)
public class OpenApiConfig {

    @Value("${spring.application.name:iam-service}")
    private String applicationName;

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("RikReay IAM Platform — " + applicationName)
                        .version("1.0.0")
                        .description("Cambodia's national Identity, Access Management & Interoperability platform. "
                                + "RBAC-secured REST API with Keycloak JWT authentication.")
                        .contact(new Contact()
                                .name("RikReay IAM Platform")
                                .url("https://github.com/mvsotso/rikreay-iam-platform"))
                        .license(new License()
                                .name("Proprietary")
                                .url("https://github.com/mvsotso/rikreay-iam-platform")));
    }
}
