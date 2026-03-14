package com.iam.platform.common.security;

import com.iam.platform.common.dto.ApiResponse;
import com.iam.platform.common.exception.AccessDeniedException;
import com.iam.platform.common.exception.IamPlatformException;
import com.iam.platform.common.exception.ResourceNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;
    private HttpServletRequest request;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
        request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/api/v1/test");
    }

    @Test
    void handleResourceNotFound_returns404() {
        ResourceNotFoundException ex = new ResourceNotFoundException("User not found");

        ResponseEntity<ApiResponse<Void>> response = handler.handleResourceNotFound(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isFalse();
        assertThat(response.getBody().getErrorCode()).isEqualTo("RESOURCE_NOT_FOUND");
        assertThat(response.getBody().getMessage()).isEqualTo("User not found");
        assertThat(response.getBody().getTimestamp()).isNotNull();
        assertThat(response.getBody().getRequestId()).isNotNull();
        assertThat(response.getBody().getPath()).isEqualTo("/api/v1/test");
    }

    @Test
    void handleAccessDenied_returns403() {
        AccessDeniedException ex = new AccessDeniedException("Insufficient permissions");

        ResponseEntity<ApiResponse<Void>> response = handler.handleAccessDenied(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody().getErrorCode()).isEqualTo("ACCESS_DENIED");
        assertThat(response.getBody().getMessage()).isEqualTo("Insufficient permissions");
    }

    @Test
    void handleSpringAccessDenied_returns403() {
        org.springframework.security.access.AccessDeniedException ex =
                new org.springframework.security.access.AccessDeniedException("Access is denied");

        ResponseEntity<ApiResponse<Void>> response = handler.handleSpringAccessDenied(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody().getErrorCode()).isEqualTo("ACCESS_DENIED");
        assertThat(response.getBody().getMessage()).isEqualTo("Access denied");
    }

    @Test
    void handleValidation_returns400WithFieldErrors() throws NoSuchMethodException {
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "target");
        bindingResult.addError(new FieldError("target", "email", "must not be blank"));
        bindingResult.addError(new FieldError("target", "name", "size must be between 1 and 100"));
        MethodParameter methodParameter = new MethodParameter(
                String.class.getMethod("toString"), -1);
        MethodArgumentNotValidException ex =
                new MethodArgumentNotValidException(methodParameter, bindingResult);

        ResponseEntity<ApiResponse<Void>> response = handler.handleValidation(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().getErrorCode()).isEqualTo("VALIDATION_ERROR");
        assertThat(response.getBody().getErrors()).hasSize(2);
        assertThat(response.getBody().getErrors())
                .extracting(ApiResponse.FieldError::getField)
                .containsExactlyInAnyOrder("email", "name");
    }

    @Test
    void handlePlatformException_returns500() {
        IamPlatformException ex = new IamPlatformException("Keycloak connection failed");

        ResponseEntity<ApiResponse<Void>> response = handler.handlePlatformException(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody().getErrorCode()).isEqualTo("PLATFORM_ERROR");
        assertThat(response.getBody().getMessage()).isEqualTo("Keycloak connection failed");
    }

    @Test
    void handleGenericException_returns500WithGenericMessage() {
        Exception ex = new RuntimeException("Unexpected NPE");

        ResponseEntity<ApiResponse<Void>> response = handler.handleGenericException(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody().getErrorCode()).isEqualTo("INTERNAL_ERROR");
        assertThat(response.getBody().getMessage()).isEqualTo("An unexpected error occurred");
    }

    @Test
    void allResponses_haveConsistentEnvelope() {
        ResourceNotFoundException ex = new ResourceNotFoundException("test");
        ResponseEntity<ApiResponse<Void>> response = handler.handleResourceNotFound(ex, request);
        ApiResponse<Void> body = response.getBody();

        assertThat(body.isSuccess()).isFalse();
        assertThat(body.getTimestamp()).isNotNull();
        assertThat(body.getRequestId()).isNotNull();
        assertThat(body.getPath()).isNotNull();
        assertThat(body.getErrorCode()).isNotNull();
        assertThat(body.getMessage()).isNotNull();
    }
}
