package com.iam.platform.admin.dto;

import java.util.List;

public record RealmSettingsResponse(
        String realmName,
        String passwordPolicy,
        boolean mfaRequired,
        int sessionIdleTimeout,
        int sessionMaxLifespan,
        String loginTheme,
        List<String> allowedRedirectUris,
        boolean bruteForceProtection
) {}
