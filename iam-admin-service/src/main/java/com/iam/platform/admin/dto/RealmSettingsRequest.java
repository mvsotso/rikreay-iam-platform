package com.iam.platform.admin.dto;

import java.util.List;

public record RealmSettingsRequest(
        String passwordPolicy,
        Boolean mfaRequired,
        Integer sessionIdleTimeout,
        Integer sessionMaxLifespan,
        String loginTheme,
        List<String> allowedRedirectUris,
        Boolean bruteForceProtection
) {}
