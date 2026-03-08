package com.iam.platform.common.constants;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class IamRoles {

    // Realm Roles (13)
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

    // Client Roles
    public static final String CLIENT_ROLE_READ = "read";
    public static final String CLIENT_ROLE_WRITE = "write";
    public static final String CLIENT_ROLE_ADMIN = "admin";
    public static final String XROAD_CONSUMER = "xroad-consumer";
    public static final String XROAD_PROVIDER = "xroad-provider";
}
