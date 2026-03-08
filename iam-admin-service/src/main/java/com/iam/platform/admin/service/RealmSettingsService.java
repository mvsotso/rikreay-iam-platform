package com.iam.platform.admin.service;

import com.iam.platform.admin.dto.RealmSettingsRequest;
import com.iam.platform.admin.dto.RealmSettingsResponse;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.representations.idm.RealmRepresentation;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class RealmSettingsService {

    private final Keycloak keycloak;

    @CircuitBreaker(name = "keycloak", fallbackMethod = "getSettingsFallback")
    public RealmSettingsResponse getRealmSettings(String realmName) {
        RealmRepresentation realm = keycloak.realm(realmName).toRepresentation();

        return new RealmSettingsResponse(
                realmName,
                realm.getPasswordPolicy() != null ? realm.getPasswordPolicy() : "",
                realm.getOtpPolicyType() != null,
                realm.getSsoSessionIdleTimeout() != null ? realm.getSsoSessionIdleTimeout() : 1800,
                realm.getSsoSessionMaxLifespan() != null ? realm.getSsoSessionMaxLifespan() : 36000,
                realm.getLoginTheme() != null ? realm.getLoginTheme() : "keycloak",
                List.of(),
                realm.isBruteForceProtected() != null && realm.isBruteForceProtected()
        );
    }

    @CircuitBreaker(name = "keycloak", fallbackMethod = "updateSettingsFallback")
    public RealmSettingsResponse updateRealmSettings(String realmName, RealmSettingsRequest request) {
        RealmRepresentation realm = keycloak.realm(realmName).toRepresentation();

        if (request.passwordPolicy() != null) {
            realm.setPasswordPolicy(request.passwordPolicy());
        }
        if (request.sessionIdleTimeout() != null) {
            realm.setSsoSessionIdleTimeout(request.sessionIdleTimeout());
        }
        if (request.sessionMaxLifespan() != null) {
            realm.setSsoSessionMaxLifespan(request.sessionMaxLifespan());
        }
        if (request.loginTheme() != null) {
            realm.setLoginTheme(request.loginTheme());
        }
        if (request.bruteForceProtection() != null) {
            realm.setBruteForceProtected(request.bruteForceProtection());
        }

        keycloak.realm(realmName).update(realm);
        log.info("Realm settings updated for: {}", realmName);

        return getRealmSettings(realmName);
    }

    @SuppressWarnings("unused")
    private RealmSettingsResponse getSettingsFallback(String realmName, Throwable t) {
        log.warn("Keycloak circuit breaker open for realm settings: {}", realmName);
        return new RealmSettingsResponse(realmName, "", false, 1800, 36000, "keycloak", List.of(), false);
    }

    @SuppressWarnings("unused")
    private RealmSettingsResponse updateSettingsFallback(String realmName, RealmSettingsRequest request, Throwable t) {
        log.error("Failed to update realm settings for: {}", realmName, t);
        throw new RuntimeException("Keycloak is unavailable, cannot update realm settings");
    }
}
