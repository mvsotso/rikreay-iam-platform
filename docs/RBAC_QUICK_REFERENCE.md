# RBAC Quick Reference — RikReay IAM Platform

Complete endpoint-to-role mapping for all 12 services. Authorization is **RBAC only** — uses `hasRole()` and `hasAnyRole()` exclusively.

---

## 1. Role Definitions (13 Realm Roles)

| Role | Constant | Tier | Description |
|------|----------|------|-------------|
| `iam-admin` | `IamRoles.IAM_ADMIN` | 0 | Full platform super administrator |
| `ops-admin` | `IamRoles.OPS_ADMIN` | 1 | Monitoring, incidents, alert management |
| `config-admin` | `IamRoles.CONFIG_ADMIN` | 1 | Configuration and feature flag management |
| `governance-admin` | `IamRoles.GOVERNANCE_ADMIN` | 1 | Access reviews, certifications, policies |
| `sector-admin` | `IamRoles.SECTOR_ADMIN` | 2 | Sector oversight (scoped by memberClass attribute) |
| `tenant-admin` | `IamRoles.TENANT_ADMIN` | 3 | Organization admin (scoped to own Keycloak realm) |
| `service-manager` | `IamRoles.SERVICE_MANAGER` | — | X-Road service registrations |
| `auditor` | `IamRoles.AUDITOR` | — | Read-only audit log access |
| `report-viewer` | `IamRoles.REPORT_VIEWER` | — | Compliance and audit report viewing |
| `developer` | `IamRoles.DEVELOPER` | — | App registration, API docs, sandbox |
| `internal-user` | `IamRoles.INTERNAL_USER` | — | Internal staff user |
| `external-user` | `IamRoles.EXTERNAL_USER` | — | External citizen/partner user |
| `api-access` | `IamRoles.API_ACCESS` | — | Basic API gateway pass-through |

---

## 2. Client Roles (Per-Service)

| Client ID | Roles |
|-----------|-------|
| `iam-core-service` | `read`, `write`, `admin` |
| `iam-xroad-adapter` | `xroad-consumer`, `xroad-provider` |
| `iam-gateway` | `gateway-admin` |
| `iam-admin-service` | `admin-read`, `admin-write` |
| `iam-monitoring-service` | `monitor-read`, `monitor-write`, `alert-manage` |
| `iam-governance-service` | `governance-read`, `governance-write`, `certification-manage` |
| `iam-developer-portal` | `portal-read`, `app-manage`, `webhook-manage` |
| `iam-notification-service` | `notification-read`, `notification-write`, `template-manage` |
| `iam-config-service` | `config-read`, `config-write` |

---

## 3. Endpoint-to-Role Mapping (All Services)

### iam-gateway (:8081)

| Endpoint | Access |
|----------|--------|
| `/actuator/**` | permitAll |
| `/auth/**` | permitAll (Keycloak proxy) |
| `/xroad/**` | permitAll (X-Road Security Server) |
| `/api/v1/docs/**`, `/api/v1/sdks/**` | permitAll |
| `/v3/api-docs/**`, `/swagger-ui/**` | permitAll |
| `/fallback/**` | permitAll |
| All other routes | authenticated (valid JWT required) |

### iam-core-service (:8082)

| Endpoint | Method | Required Role(s) |
|----------|--------|-------------------|
| `/actuator/health`, `/actuator/info`, `/actuator/prometheus` | GET | permitAll |
| `/v3/api-docs/**`, `/swagger-ui/**` | GET | permitAll |
| `/xroad/**` | ALL | permitAll (X-Road authenticated) |
| `/api/v1/persons/me`, `/api/v1/users/me` | GET | authenticated |
| `/api/v1/persons/**` | ALL | `internal-user`, `tenant-admin`, `iam-admin` |
| `/api/v1/entities/**` | ALL | `internal-user`, `tenant-admin`, `iam-admin` |
| `/api/v1/representations/**` | ALL | `tenant-admin`, `iam-admin` |
| `/api/v1/users/**` | ALL | `internal-user`, `tenant-admin`, `iam-admin` |
| All other | ALL | `api-access` |

### iam-tenant-service (:8083)

| Endpoint | Method | Required Role(s) |
|----------|--------|-------------------|
| `/actuator/health`, `/actuator/info`, `/actuator/prometheus` | GET | permitAll |
| `/v3/api-docs/**`, `/swagger-ui/**` | GET | permitAll |
| `/api/v1/tenants` | POST | `iam-admin` |
| `/api/v1/tenants/*/suspend` | PUT | `iam-admin` |
| `/api/v1/tenants/*/activate` | PUT | `iam-admin` |
| `/api/v1/tenants/*` | DELETE | `iam-admin` |
| `/api/v1/tenants/**` | GET | `iam-admin`, `tenant-admin` |
| `/api/v1/tenants/*` | PUT | `iam-admin`, `tenant-admin` |
| All other | ALL | authenticated |

### iam-audit-service (:8084)

| Endpoint | Method | Required Role(s) |
|----------|--------|-------------------|
| `/actuator/health`, `/actuator/info`, `/actuator/prometheus` | GET | permitAll |
| `/v3/api-docs/**`, `/swagger-ui/**` | GET | permitAll |
| `/api/v1/audit/events/**` | GET | `auditor`, `iam-admin`, `report-viewer` |
| `/api/v1/audit/xroad/**` | GET | `auditor`, `iam-admin`, `service-manager` |
| `/api/v1/audit/stats/**` | GET | `auditor`, `iam-admin` |
| `/api/v1/audit/login-history/**` | GET | `auditor`, `iam-admin` |
| `/api/v1/audit/**` | ALL | `auditor`, `iam-admin`, `report-viewer` |
| All other | ALL | authenticated |

### iam-xroad-adapter (:8085)

| Endpoint | Method | Required Role(s) |
|----------|--------|-------------------|
| `/actuator/health`, `/actuator/info`, `/actuator/prometheus` | GET | permitAll |
| `/v3/api-docs/**`, `/swagger-ui/**` | GET | permitAll |
| `/xroad/**` | ALL | permitAll (X-Road authenticated) |
| `/api/v1/xroad/**` | ALL | `service-manager`, `iam-admin` |
| All other | ALL | authenticated |

### iam-admin-service (:8086)

| Endpoint | Method | Required Role(s) |
|----------|--------|-------------------|
| `/actuator/health`, `/actuator/info`, `/actuator/prometheus` | GET | permitAll |
| `/v3/api-docs/**`, `/swagger-ui/**` | GET | permitAll |
| `/api/v1/platform-admin/platform/**` | ALL | `iam-admin` |
| `/api/v1/platform-admin/sector-admins/**` | ALL | `iam-admin` |
| `/api/v1/platform-admin/users/bulk-import` | POST | `iam-admin` |
| `/api/v1/platform-admin/users/bulk-export` | GET | `iam-admin` |
| `/api/v1/platform-admin/users/bulk-disable` | POST | `iam-admin` |
| `/api/v1/platform-admin/users` | GET | `iam-admin`, `tenant-admin` |
| `/api/v1/platform-admin/settings/**` | ALL | `iam-admin`, `config-admin` |
| `/api/v1/platform-admin/sector/**` | ALL | `sector-admin` |
| `/api/v1/platform-admin/org/**` | ALL | `tenant-admin` |
| All other | ALL | authenticated |

### iam-monitoring-service (:8087)

| Endpoint | Method | Required Role(s) |
|----------|--------|-------------------|
| `/actuator/health`, `/actuator/info`, `/actuator/prometheus` | GET | permitAll |
| `/v3/api-docs/**`, `/swagger-ui/**` | GET | permitAll |
| `/api/v1/monitoring/auth-analytics/tenant/**` | GET | `ops-admin`, `iam-admin`, `tenant-admin` |
| `/api/v1/monitoring/xroad-metrics` | GET | `ops-admin`, `iam-admin`, `service-manager` |
| `/api/v1/monitoring/**` | ALL | `ops-admin`, `iam-admin` |
| All other | ALL | authenticated |

### iam-governance-service (:8088)

| Endpoint | Method | Required Role(s) |
|----------|--------|-------------------|
| `/actuator/health`, `/actuator/info`, `/actuator/prometheus` | GET | permitAll |
| `/v3/api-docs/**`, `/swagger-ui/**` | GET | permitAll |
| `/api/v1/governance/consents` | POST | authenticated |
| `/api/v1/governance/consents/me` | GET | authenticated |
| `/api/v1/governance/consents/*` | DELETE | authenticated |
| `/api/v1/governance/consents` | GET | `governance-admin`, `iam-admin` |
| `/api/v1/governance/campaigns/*/reviews` | POST | `tenant-admin`, `governance-admin` |
| `/api/v1/governance/campaigns/*/reviews` | GET | `governance-admin`, `iam-admin`, `tenant-admin` |
| `/api/v1/governance/reports/**` | ALL | `report-viewer`, `governance-admin`, `iam-admin` |
| `/api/v1/governance/**` | ALL | `governance-admin`, `iam-admin` |
| All other | ALL | authenticated |

### iam-developer-portal (:8089)

| Endpoint | Method | Required Role(s) |
|----------|--------|-------------------|
| `/actuator/health`, `/actuator/info`, `/actuator/prometheus` | GET | permitAll |
| `/v3/api-docs/**`, `/swagger-ui/**` | GET | permitAll |
| `/api/v1/docs/**` | GET | permitAll (public API docs) |
| `/api/v1/sdks` | GET | permitAll (SDK info) |
| `/api/v1/apps/**` | ALL | `developer`, `iam-admin` |
| `/api/v1/webhooks/**` | ALL | `developer`, `iam-admin` |
| `/api/v1/sandbox/**` | ALL | `developer`, `iam-admin` |
| All other | ALL | authenticated |

### iam-notification-service (:8090)

| Endpoint | Method | Required Role(s) |
|----------|--------|-------------------|
| `/actuator/health`, `/actuator/info`, `/actuator/prometheus` | GET | permitAll |
| `/v3/api-docs/**`, `/swagger-ui/**` | GET | permitAll |
| `/api/v1/notifications/**` | GET | `iam-admin`, `ops-admin` |
| `/api/v1/notifications/send` | POST | `iam-admin`, `ops-admin` |
| `/api/v1/notifications/channels/**` | ALL | `iam-admin` |
| `/api/v1/notifications/templates/**` | ALL | `iam-admin` |
| `/api/v1/notifications/schedules/**` | ALL | `iam-admin`, `ops-admin` |
| All other | ALL | authenticated |

### iam-config-service (:8888)

| Endpoint | Method | Required Role(s) |
|----------|--------|-------------------|
| `/actuator/health`, `/actuator/info`, `/actuator/prometheus` | GET | permitAll |
| `/v3/api-docs/**`, `/swagger-ui/**` | GET | permitAll |
| `/{application}/{profile}` | GET | permitAll (config bootstrap) |
| `/{application}/{profile}/{label}` | GET | permitAll (config bootstrap) |
| `/{application}-{profile}.yml` | GET | permitAll (config bootstrap) |
| `/{application}-{profile}.properties` | GET | permitAll (config bootstrap) |
| `/api/v1/config/**` | ALL | `config-admin`, `iam-admin` |
| All other | ALL | authenticated |

---

## 4. Role-to-Endpoint Access Matrix

Which endpoints each role can access (beyond public/permitAll endpoints):

### iam-admin (Tier 0 — Full Access)

| Service | Endpoints |
|---------|-----------|
| Core (:8082) | `/api/v1/persons/**`, `/api/v1/entities/**`, `/api/v1/representations/**`, `/api/v1/users/**` |
| Tenant (:8083) | `/api/v1/tenants/**` (full CRUD including create, suspend, delete) |
| Audit (:8084) | `/api/v1/audit/**` |
| X-Road (:8085) | `/api/v1/xroad/**` |
| Admin (:8086) | `/api/v1/platform-admin/platform/**`, `/api/v1/platform-admin/sector-admins/**`, `/api/v1/platform-admin/users/**`, `/api/v1/platform-admin/settings/**` |
| Monitoring (:8087) | `/api/v1/monitoring/**` |
| Governance (:8088) | `/api/v1/governance/**` |
| Developer (:8089) | `/api/v1/apps/**`, `/api/v1/webhooks/**`, `/api/v1/sandbox/**` |
| Notification (:8090) | `/api/v1/notifications/**` (all sub-paths) |
| Config (:8888) | `/api/v1/config/**` |

### ops-admin (Tier 1)

| Service | Endpoints |
|---------|-----------|
| Monitoring (:8087) | `/api/v1/monitoring/**` |
| Notification (:8090) | `/api/v1/notifications/**` (logs, send, schedules) |

### config-admin (Tier 1)

| Service | Endpoints |
|---------|-----------|
| Admin (:8086) | `/api/v1/platform-admin/settings/**` |
| Config (:8888) | `/api/v1/config/**` |

### governance-admin (Tier 1)

| Service | Endpoints |
|---------|-----------|
| Governance (:8088) | `/api/v1/governance/**` (campaigns, policies, reviews, consents admin, reports) |

### sector-admin (Tier 2)

| Service | Endpoints |
|---------|-----------|
| Admin (:8086) | `/api/v1/platform-admin/sector/**` |

### tenant-admin (Tier 3)

| Service | Endpoints |
|---------|-----------|
| Core (:8082) | `/api/v1/persons/**`, `/api/v1/entities/**`, `/api/v1/representations/**`, `/api/v1/users/**` |
| Tenant (:8083) | `/api/v1/tenants/**` (read, update — not create/delete) |
| Admin (:8086) | `/api/v1/platform-admin/org/**`, `/api/v1/platform-admin/users` (list) |
| Monitoring (:8087) | `/api/v1/monitoring/auth-analytics/tenant/**` |
| Governance (:8088) | `/api/v1/governance/campaigns/*/reviews` (submit and view) |

### service-manager

| Service | Endpoints |
|---------|-----------|
| X-Road (:8085) | `/api/v1/xroad/**` |
| Monitoring (:8087) | `/api/v1/monitoring/xroad-metrics` |
| Audit (:8084) | `/api/v1/audit/xroad/**` |

### auditor

| Service | Endpoints |
|---------|-----------|
| Audit (:8084) | `/api/v1/audit/**` |

### report-viewer

| Service | Endpoints |
|---------|-----------|
| Audit (:8084) | `/api/v1/audit/events/**` |
| Governance (:8088) | `/api/v1/governance/reports/**` |

### developer

| Service | Endpoints |
|---------|-----------|
| Developer (:8089) | `/api/v1/apps/**`, `/api/v1/webhooks/**`, `/api/v1/sandbox/**` |

### internal-user

| Service | Endpoints |
|---------|-----------|
| Core (:8082) | `/api/v1/persons/**`, `/api/v1/entities/**`, `/api/v1/users/**` |

### external-user

| Service | Endpoints |
|---------|-----------|
| (No specific endpoint access — uses api-access for gateway pass-through) |

### api-access

| Service | Endpoints |
|---------|-----------|
| Core (:8082) | All other endpoints (fallback) |
| Gateway (:8081) | Pass-through to authenticated routes |

---

## 5. JWT Token Structure

### Keycloak JWT Payload (Decoded)

```json
{
  "exp": 1741500000,
  "iat": 1741496400,
  "jti": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "iss": "http://localhost:8080/realms/iam-platform",
  "sub": "user-uuid-here",
  "typ": "Bearer",
  "azp": "iam-gateway",
  "preferred_username": "admin-user",
  "email": "admin@iam-platform.local",
  "realm_access": {
    "roles": [
      "iam-admin",
      "api-access"
    ]
  },
  "resource_access": {
    "iam-core-service": {
      "roles": ["read", "write", "admin"]
    },
    "iam-gateway": {
      "roles": ["gateway-admin"]
    }
  }
}
```

### How Roles Map to Spring Security Authorities

| JWT Location | Role | Spring Authority |
|-------------|------|------------------|
| `realm_access.roles["iam-admin"]` | Realm role | `ROLE_iam-admin` |
| `realm_access.roles["tenant-admin"]` | Realm role | `ROLE_tenant-admin` |
| `resource_access.iam-core-service.roles["read"]` | Client role | `ROLE_iam-core-service_read` |
| `resource_access.iam-gateway.roles["gateway-admin"]` | Client role | `ROLE_iam-gateway_gateway-admin` |

### Converter: KeycloakJwtAuthenticationConverter

Located in: `iam-common/src/main/java/com/iam/platform/common/security/KeycloakJwtAuthenticationConverter.java`

Extracts roles from both `realm_access.roles` and `resource_access.{client}.roles`, prefixes with `ROLE_` (realm) or `ROLE_{clientId}_` (client).

---

## 6. Spring Security Annotation Patterns

### Method-Level Security (Controllers)

```java
// Single role
@PreAuthorize("hasRole('iam-admin')")
public ResponseEntity<?> createTenant(...) { }

// Multiple roles (OR)
@PreAuthorize("hasAnyRole('iam-admin', 'tenant-admin')")
public ResponseEntity<?> listTenants(...) { }

// Client role
@PreAuthorize("hasRole('iam-core-service_admin')")
public ResponseEntity<?> adminOperation(...) { }
```

### HttpSecurity Configuration (SecurityConfig)

```java
// Single role
.requestMatchers("/api/v1/platform-admin/platform/**").hasRole("iam-admin")

// Multiple roles
.requestMatchers("/api/v1/tenants/**").hasAnyRole("iam-admin", "tenant-admin")

// Public endpoints
.requestMatchers("/actuator/health", "/actuator/info").permitAll()

// X-Road (authenticated by Security Server, not JWT)
.requestMatchers("/xroad/**").permitAll()

// Catch-all
.anyRequest().authenticated()
```

### Important Notes

1. **RBAC only** — Never use ABAC, attribute-based policies, or custom claims as authorities
2. `hasRole("iam-admin")` automatically checks for `ROLE_iam-admin` authority
3. X-Road endpoints use `permitAll()` but are authenticated by the X-Road Security Server via `XRoadRequestFilter`
4. Config service bootstrap endpoints are `permitAll()` because services need config before JWT is available
5. Developer Portal docs (`/api/v1/docs/**`, `/api/v1/sdks`) are `permitAll()` — public API documentation
6. All services expose `/actuator/health`, `/actuator/info`, `/actuator/prometheus` as `permitAll()`
