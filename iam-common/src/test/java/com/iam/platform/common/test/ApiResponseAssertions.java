package com.iam.platform.common.test;

import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.ResultMatcher;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Custom assertions for validating ApiResponse<T> envelope structure.
 * Ensures all responses follow the standard contract.
 */
public final class ApiResponseAssertions {

    private ApiResponseAssertions() {}

    /**
     * Assert a successful API response with proper envelope.
     */
    public static ResultActions assertApiSuccess(ResultActions result) throws Exception {
        return result
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.requestId").exists())
                .andExpect(jsonPath("$.data").exists());
    }

    /**
     * Assert a successful API response with 201 Created.
     */
    public static ResultActions assertApiCreated(ResultActions result) throws Exception {
        return result
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.requestId").exists());
    }

    /**
     * Assert an error API response with expected status and error code.
     */
    public static ResultActions assertApiError(ResultActions result, int httpStatus, String errorCode) throws Exception {
        return result
                .andExpect(status().is(httpStatus))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value(errorCode))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.requestId").exists());
    }

    /**
     * Assert a paginated response with standard Spring Data structure.
     */
    public static ResultActions assertPagedResponse(ResultActions result) throws Exception {
        return result
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.pageable").exists())
                .andExpect(jsonPath("$.data.totalElements").exists())
                .andExpect(jsonPath("$.data.totalPages").exists());
    }

    /**
     * Assert 401 Unauthorized response.
     */
    public static ResultActions assertUnauthorized(ResultActions result) throws Exception {
        return result.andExpect(status().isUnauthorized());
    }

    /**
     * Assert 403 Forbidden response.
     */
    public static ResultActions assertForbidden(ResultActions result) throws Exception {
        return result.andExpect(status().isForbidden());
    }

    /**
     * Assert 400 Bad Request with validation errors.
     */
    public static ResultActions assertValidationError(ResultActions result) throws Exception {
        return result
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors").isArray())
                .andExpect(jsonPath("$.errors").isNotEmpty());
    }

    /**
     * ResultMatcher for checking success envelope structure.
     */
    public static ResultMatcher isApiSuccess() {
        return result -> {
            jsonPath("$.success").value(true).match(result);
            jsonPath("$.timestamp").exists().match(result);
            jsonPath("$.requestId").exists().match(result);
        };
    }
}
