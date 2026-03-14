package com.iam.platform.common.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ApiResponseTest {

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @Test
    void ok_withData_setsSuccessAndData() {
        ApiResponse<String> response = ApiResponse.ok("test-data");

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getData()).isEqualTo("test-data");
        assertThat(response.getMessage()).isEqualTo("Operation completed");
        assertThat(response.getTimestamp()).isNotNull();
        assertThat(response.getRequestId()).isNotNull();
        assertThat(response.getErrorCode()).isNull();
    }

    @Test
    void ok_withMessage_setsCustomMessage() {
        ApiResponse<String> response = ApiResponse.ok("data", "Custom message");

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getMessage()).isEqualTo("Custom message");
    }

    @Test
    void error_withMessage_setsFailure() {
        ApiResponse<Void> response = ApiResponse.error("Something went wrong");

        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getMessage()).isEqualTo("Something went wrong");
        assertThat(response.getTimestamp()).isNotNull();
        assertThat(response.getRequestId()).isNotNull();
        assertThat(response.getData()).isNull();
    }

    @Test
    void error_withErrorCode_setsCode() {
        ApiResponse<Void> response = ApiResponse.error("Not found", "RESOURCE_NOT_FOUND");

        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getErrorCode()).isEqualTo("RESOURCE_NOT_FOUND");
    }

    @Test
    void requestId_isUniquePerCall() {
        ApiResponse<String> r1 = ApiResponse.ok("a");
        ApiResponse<String> r2 = ApiResponse.ok("b");

        assertThat(r1.getRequestId()).isNotEqualTo(r2.getRequestId());
    }

    @Test
    void jsonSerialization_excludesNullFields() throws Exception {
        ApiResponse<String> response = ApiResponse.ok("data");
        String json = objectMapper.writeValueAsString(response);

        assertThat(json).contains("\"success\":true");
        assertThat(json).contains("\"data\":\"data\"");
        assertThat(json).doesNotContain("\"errorCode\"");
        assertThat(json).doesNotContain("\"errors\"");
    }

    @Test
    void fieldError_builder_works() {
        ApiResponse.FieldError fieldError = ApiResponse.FieldError.builder()
                .field("email")
                .message("must not be blank")
                .build();

        assertThat(fieldError.getField()).isEqualTo("email");
        assertThat(fieldError.getMessage()).isEqualTo("must not be blank");
    }

    @Test
    void errorWithFieldErrors_serializable() throws Exception {
        ApiResponse<Void> response = ApiResponse.<Void>builder()
                .success(false)
                .message("Validation failed")
                .errorCode("VALIDATION_ERROR")
                .errors(List.of(
                        ApiResponse.FieldError.builder().field("name").message("required").build()
                ))
                .build();

        String json = objectMapper.writeValueAsString(response);
        assertThat(json).contains("\"errors\"");
        assertThat(json).contains("\"field\":\"name\"");
    }
}
