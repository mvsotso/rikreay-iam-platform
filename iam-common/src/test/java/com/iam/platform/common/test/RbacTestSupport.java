package com.iam.platform.common.test;

import org.junit.jupiter.params.provider.Arguments;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

/**
 * Helper for generating RBAC matrix test data.
 * Produces ParameterizedTest MethodSource data for role × endpoint combinations.
 */
public final class RbacTestSupport {

    private RbacTestSupport() {}

    /**
     * Generates test arguments for roles that SHOULD have access (expect 2xx).
     *
     * @param method      HTTP method (GET, POST, PUT, DELETE)
     * @param endpoint    URL path
     * @param allowedRoles roles that should succeed
     * @return stream of Arguments(method, endpoint, role, true)
     */
    public static Stream<Arguments> allowedRoles(String method, String endpoint, String... allowedRoles) {
        return Arrays.stream(allowedRoles)
                .map(role -> Arguments.of(method, endpoint, role, true));
    }

    /**
     * Generates test arguments for roles that SHOULD be denied (expect 403).
     *
     * @param method      HTTP method
     * @param endpoint    URL path
     * @param deniedRoles roles that should get 403
     * @return stream of Arguments(method, endpoint, role, false)
     */
    public static Stream<Arguments> deniedRoles(String method, String endpoint, String... deniedRoles) {
        return Arrays.stream(deniedRoles)
                .map(role -> Arguments.of(method, endpoint, role, false));
    }

    /**
     * Generates a full RBAC matrix: allowed roles get (role, true), all others get (role, false).
     *
     * @param method       HTTP method
     * @param endpoint     URL path
     * @param allowedRoles roles that should succeed
     * @return stream of Arguments(method, endpoint, role, shouldSucceed)
     */
    public static Stream<Arguments> fullMatrix(String method, String endpoint, String... allowedRoles) {
        List<String> allowed = Arrays.asList(allowedRoles);
        return Arrays.stream(TestConstants.ALL_ROLES)
                .map(role -> Arguments.of(method, endpoint, role, allowed.contains(role)));
    }

    /**
     * Combines multiple endpoint RBAC matrices into one stream.
     * Useful for @MethodSource that covers multiple endpoints.
     */
    public static Stream<Arguments> combineMatrices(Stream<Arguments>... matrices) {
        Stream<Arguments> result = Stream.empty();
        for (Stream<Arguments> matrix : matrices) {
            result = Stream.concat(result, matrix);
        }
        return result;
    }

    /**
     * Returns all roles EXCEPT the specified ones.
     * Useful for building denied-role lists from allowed-role lists.
     */
    public static String[] rolesExcept(String... excludedRoles) {
        List<String> excluded = Arrays.asList(excludedRoles);
        return Arrays.stream(TestConstants.ALL_ROLES)
                .filter(role -> !excluded.contains(role))
                .toArray(String[]::new);
    }
}
