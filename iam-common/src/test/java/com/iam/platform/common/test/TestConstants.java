package com.iam.platform.common.test;

/**
 * Shared test constants for all IAM modules.
 * Provides role names, test usernames, and reusable identifiers.
 */
public final class TestConstants {

    private TestConstants() {}

    // ====== Realm Roles ======
    public static final String ROLE_IAM_ADMIN = "iam-admin";
    public static final String ROLE_TENANT_ADMIN = "tenant-admin";
    public static final String ROLE_SECTOR_ADMIN = "sector-admin";
    public static final String ROLE_SERVICE_MANAGER = "service-manager";
    public static final String ROLE_AUDITOR = "auditor";
    public static final String ROLE_API_ACCESS = "api-access";
    public static final String ROLE_INTERNAL_USER = "internal-user";
    public static final String ROLE_EXTERNAL_USER = "external-user";
    public static final String ROLE_CONFIG_ADMIN = "config-admin";
    public static final String ROLE_OPS_ADMIN = "ops-admin";
    public static final String ROLE_GOVERNANCE_ADMIN = "governance-admin";
    public static final String ROLE_DEVELOPER = "developer";
    public static final String ROLE_REPORT_VIEWER = "report-viewer";

    /** All 13 realm roles */
    public static final String[] ALL_ROLES = {
            ROLE_IAM_ADMIN, ROLE_TENANT_ADMIN, ROLE_SECTOR_ADMIN,
            ROLE_SERVICE_MANAGER, ROLE_AUDITOR, ROLE_API_ACCESS,
            ROLE_INTERNAL_USER, ROLE_EXTERNAL_USER, ROLE_CONFIG_ADMIN,
            ROLE_OPS_ADMIN, ROLE_GOVERNANCE_ADMIN, ROLE_DEVELOPER,
            ROLE_REPORT_VIEWER
    };

    // ====== Test Usernames ======
    public static final String USER_ADMIN = "admin-user";
    public static final String USER_TENANT_ADMIN = "tenant-admin-user";
    public static final String USER_SECTOR_ADMIN = "sector-admin-user";
    public static final String USER_OPS_ADMIN = "ops-admin-user";
    public static final String USER_AUDITOR = "auditor-user";
    public static final String USER_DEVELOPER = "dev-user";
    public static final String USER_CITIZEN = "citizen-user";
    public static final String USER_INTERNAL = "internal-user";

    // ====== Test Identifiers ======
    public static final String TEST_TENANT_ID = "550e8400-e29b-41d4-a716-446655440000";
    public static final String TEST_PERSON_ID = "660e8400-e29b-41d4-a716-446655440001";
    public static final String TEST_ENTITY_ID = "770e8400-e29b-41d4-a716-446655440002";
    public static final String TEST_REALM_NAME = "test-realm";
    public static final String TEST_MEMBER_CLASS = "COM";
}
