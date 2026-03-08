# CLAUDE.md — Project Instructions for Claude Code

## Project Overview

This is the **RikReay IAM Enterprise Platform** — Cambodia's national Identity, Access Management & Interoperability platform built on **Keycloak 26.5.x** + **Spring Boot 3.5.x** + **Spring Cloud 2025.0.x** + X-Road with RBAC authorization.

**Scope:** Serves ALL sectors in Cambodia — Government (GOV), Commercial/Private (COM), NGOs (NGO), and Municipal Government (MUN). This is NOT a tax-specific platform.

**GitHub:** https://github.com/mvsotso/rikreay-iam-platform.git

## Architecture: 5 Domains, 12 Modules

### Domain 1: Identity & Access Core
- `iam-common` — Shared DTOs, RBAC constants, JWT converter, security auto-config, exceptions
- `iam-gateway` — Spring Cloud Gateway (reactive), JWT validation, rate limiting (:8081)
- `iam-core-service` — User profiles, RBAC-secured APIs, X-Road endpoints (:8082)
- `iam-xroad-adapter` — X-Road Security Server bridge, routing, ACL (:8085)

### Domain 2: Administration & Configuration
- `iam-tenant-service` — Realm provisioning via Keycloak Admin API (:8083)
- `iam-admin-service` — Unified admin APIs at `/api/v1/platform-admin/**` (:8086)
- `iam-config-service` — Spring Cloud Config Server, feature flags (:8888)

### Domain 3: Monitoring & Operations
- `iam-monitoring-service` — Health aggregation, auth analytics, incidents, alerts (:8087)

### Domain 4: Identity Governance & Compliance
- `iam-audit-service` — Kafka consumer → Elasticsearch indexer, audit queries (:8084)
- `iam-governance-service` — Access reviews, certifications, policies, risk scoring (:8088)

### Domain 5: Developer & Integration Portal
- `iam-developer-portal` — API docs, self-service app registration, webhooks, sandbox (:8089)

### Cross-Platform Service
- `iam-notification-service` — Email/SMS/Telegram notifications, alert routing (:8090)

## Key Decisions — DO NOT CHANGE

- **Authorization: RBAC only** — No ABAC, no attribute-based policies, no custom claims as authorities
- **All authorization uses `hasRole()` and `hasAnyRole()`** with Keycloak realm roles and client roles
- **Realm-per-tenant** multi-tenancy in Keycloak
- **Spring Boot 3.5.x** (latest supported) with **Spring Cloud 2025.0.x** (Northfields)
- **Keycloak 26.5.x** — pin to specific patch (e.g., 26.5.5). Do NOT use unpinned tags.
- **Java 21** (LTS) — use records, sealed classes, pattern matching where appropriate
- **X-Road KH instance** with 4 member classes: GOV, COM, NGO, MUN
- **JWT RS256** with PKCE (S256) for public clients
- **X-Road endpoints** (`/xroad/**`) are permitAll — authenticated by Security Server via XRoadRequestFilter
- **Kafka 7.6** (Confluent Platform) for event streaming (dot-notation topics), **Elasticsearch 8.12** for audit indexing
- **Database-per-service** — each microservice owns its PostgreSQL schema
- **Flyway** for database migrations (NOT ddl-auto in production)
- **Soft delete** for all business entities via BaseEntity (`@SQLRestriction("deleted = false")` — `@Where` is deprecated in Hibernate 6.3+)
- **WebClient** for inter-service REST calls with Resilience4j circuit breakers
- **Config Service bootstraps first** — all other services pull config from it
- **Admin service uses `/api/v1/platform-admin/**`** — NOT `/api/v1/admin/**` (avoids core-service collision)
- **Not tax-specific** — National platform for all sectors in Cambodia
- **Three-tier identity model** — Natural Person (រូបវន្តបុគ្គល), Legal Entity (នីតិបុគ្គល), Representative (អ្នកតំណាង) relationship
- **CamDigiKey integration** — Keycloak IdP broker for Cambodia's national digital identity (OAuth 2.0 / OIDC)
- **CamDX / X-Road alignment** — Interoperates with Cambodia Data Exchange (X-Road KH instance)

## Identity Model

### Three Identity Types

| Type | Khmer | Authoritative Source | Keycloak Mapping |
|------|-------|---------------------|------------------|
| Natural Person | រូបវន្តបុគ្គល | MoI (eID, CamDigiKey) | Keycloak User |
| Legal Entity | នីតិបុគ្គល | MoC (registration), GDT (TIN) | Keycloak Realm (tenant) |
| Representative | អ្នកតំណាង | Delegation document | Role assignments scoped to tenant realm |

### Core Identity Entities (iam-core-service)

- `NaturalPerson` — Human identity with personalIdCode (MoI permanent ID), nationalIdNumber, camDigiKeyId, names (Khmer + Latin), verification status (UNVERIFIED → VERIFIED)
- `LegalEntity` — Organization with registrationNumber (MoC), TIN (GDT), entityType (16 types: GOVERNMENT_MINISTRY, PRIVATE_LLC, LOCAL_NGO, MUNICIPALITY, etc.), memberClass (GOV/COM/NGO/MUN), realmName (Keycloak tenant)
- `Representation` — Links NaturalPerson to LegalEntity with: representativeRole (13 roles), delegationScope (FULL/LIMITED/READ_ONLY/SPECIFIC), validFrom/validUntil, authorizationDocument, verificationStatus
- `ExternalIdentityLink` — Maps platform identity to 12 external systems (MoI, CamDigiKey, MoC, GDT TIN, GDT VAT, MLVT, NBC, CDC, X-Road, Passport, etc.)
- `Address` — Cambodia administrative divisions (sangkat, khan, province)
- `ContactChannel` — Multi-channel (EMAIL, PHONE, TELEGRAM, SMS) with verification
- `IdentityDocument` — 13 document types (National ID, Certificate of Incorporation, Power of Attorney, etc.) with file storage and SHA-256 hash

### Additional Identity Entities

- `ConsentRecord` (iam-governance-service) — LPDP-ready consent tracking per data subject per purpose, 6 legal bases
- `VerificationEvent` (iam-audit-service) — Immutable audit trail for all identity verification actions (Elasticsearch)

### Entity Type Classification

| entityType | memberClass | Examples |
|------------|-------------|----------|
| GOVERNMENT_MINISTRY, GOVERNMENT_DEPARTMENT, STATE_ENTERPRISE | GOV | GDT, MOF, MEF, MoI |
| MUNICIPALITY, COMMUNE | MUN | Phnom Penh, Siem Reap, Battambang |
| PRIVATE_LLC, SINGLE_MEMBER_LLC, PUBLIC_LIMITED, BRANCH_OFFICE, REPRESENTATIVE_OFFICE, SOLE_PROPRIETOR, PARTNERSHIP | COM | ABA Bank, Wing Money, Smart Axiata |
| LOCAL_NGO, INTERNATIONAL_NGO, ASSOCIATION, FOREIGN_MISSION | NGO | UNDP, World Bank, ADB |

### Representative Roles

| Role | Description |
|------|-------------|
| LEGAL_REPRESENTATIVE | Director, CEO — full legal authority |
| AUTHORIZED_SIGNATORY | Authorized to sign on behalf of entity |
| TAX_REPRESENTATIVE | Authorized for GDT tax filings |
| FINANCE_OFFICER | Financial data and reports access |
| IT_ADMINISTRATOR | Platform and system administration |
| COMPLIANCE_OFFICER | Regulatory compliance (BFI tenants) |
| GOVERNMENT_OFFICER | Civil servant for ministry operations |
| DELEGATED_USER | General delegated user with specific permissions |
| EXTERNAL_AUDITOR | Temporary time-bounded audit access |

### Identity Verification Levels

| Level | Name | Method | Access |
|-------|------|--------|--------|
| 0 | Unverified | Self-registration | Read-only public info |
| 1 | Basic | Email + phone verified | Basic portal access |
| 2 | Document | National ID uploaded + reviewed | Full citizen services |
| 3 | eKYC | CamDigiKey or biometric | Government services, represent entities |
| 4 | In-Person | Physical verification | High-value transactions, legal representative |

### Cambodia Identity Ecosystem Integration

| System | Method | Purpose |
|--------|--------|---------|
| MoI National ID / eID | X-Road / CamDX | Verify natural person identity |
| CamDigiKey | OAuth 2.0 (Keycloak IdP broker) | Citizen digital authentication |
| MoC Business Registration | X-Road / CamDX | Verify legal entity registration |
| GDT (Tax) | Internal / X-Road | TIN, VAT, patent verification |
| MLVT (Labour) | X-Road / CamDX | Labour registration verification |
| NBC (Banking) | X-Road | Banking license verification |
| CamDX | X-Road protocol | All G2G data exchange |

## Database Isolation

| Service | Database | Notes |
|---------|----------|-------|
| Keycloak | keycloak_db | Managed by Keycloak |
| iam-core-service | iam_core | User profiles |
| iam-tenant-service | iam_tenant | Tenant metadata |
| iam-audit-service | iam_audit | Also uses Elasticsearch |
| iam-xroad-adapter | iam_xroad | Service registry, ACL |
| iam-admin-service | iam_admin | Settings, bulk ops |
| iam-monitoring-service | iam_monitoring | Incidents, alerts |
| iam-governance-service | iam_governance | Campaigns, policies |
| iam-developer-portal | iam_developer | Apps, webhooks |
| iam-notification-service | iam_notification | Templates, channels |
| iam-config-service | iam_config | Feature flags |

**Rule:** Services NEVER query another service's database. Use REST APIs.

## Kafka Topic Registry

| Topic | Producer(s) | Consumer(s) |
|-------|------------|-------------|
| `iam.audit.events` | All services | iam-audit-service |
| `iam.xroad.events` | core, xroad-adapter | iam-audit-service |
| `iam.notification.commands` | monitoring, governance, admin | iam-notification-service |
| `iam.platform.events` | core, tenant, admin, governance | iam-developer-portal |
| `iam.alert.triggers` | iam-monitoring-service | iam-notification-service |

## RBAC Roles (13 Realm Roles)

| Role | Domain | Description |
|------|--------|-------------|
| iam-admin | Core | Full platform administrator (Tier 0-1) |
| tenant-admin | Admin | Organization admin — manages their own org (Tier 3) |
| sector-admin | Admin | Sector oversight for one member class GOV/COM/NGO/MUN (Tier 2) |
| service-manager | Core | Manages X-Road service registrations |
| auditor | Governance | Read-only access to audit logs |
| api-access | Core | Basic API access (gateway pass-through) |
| internal-user | Core | Internal staff user |
| external-user | Core | External citizen/partner user |
| config-admin | Admin | Manage platform configuration and feature flags |
| ops-admin | Operations | Access monitoring dashboards and incident management |
| governance-admin | Governance | Manage access reviews, certifications, policies |
| developer | Developer | Self-service app registration, API docs, sandbox |
| report-viewer | Governance | View compliance and audit reports |

## Administration Tiers

| Tier | Role(s) | Scope | Who |
|------|---------|-------|-----|
| 0: Platform Super Admin | iam-admin | Entire platform, all tenants, infrastructure | Platform team (2-3 people) |
| 1: Platform Operations | ops-admin, config-admin, auditor, governance-admin | Platform-wide operations, NOT infrastructure | Ops team (5-10 people) |
| 2: Sector Admin | sector-admin | All organizations within one member class | Sector regulator (MPTC, NBC, MoC, MoI) |
| 3: Organization Admin | tenant-admin | Single organization (single Keycloak realm) | Org IT admin or legal representative |

**Sector Admin scope:** sector-admin role combined with memberClass assignment (stored as user attribute in Keycloak). API endpoints filter by memberClass. This is RBAC, not ABAC.

**Org Admin self-service:** tenant-admin can manage their org dashboard, users, representatives, audit logs, security settings (password policy, MFA, session timeout), notification preferences, compliance status, and reports — all scoped to their own realm.

## Client Roles (per-service)

| Client ID | Roles |
|-----------|-------|
| `iam-core-service` | read, write, admin |
| `iam-xroad-adapter` | xroad-consumer, xroad-provider |
| `iam-gateway` | gateway-admin |
| `iam-admin-service` | admin-read, admin-write |
| `iam-monitoring-service` | monitor-read, monitor-write, alert-manage |
| `iam-governance-service` | governance-read, governance-write, certification-manage |
| `iam-developer-portal` | portal-read, app-manage, webhook-manage |
| `iam-notification-service` | notification-read, notification-write, template-manage |
| `iam-config-service` | config-read, config-write |

## Code Conventions

- **Package root:** `com.iam.platform`
- **Subpackages:** `com.iam.platform.{core|tenant|audit|xroad|gateway|admin|monitoring|governance|developer|notification|config}`
- **Shared code:** `com.iam.platform.common`
- Use **Lombok** (`@Data`, `@Builder`, `@Slf4j`, `@RequiredArgsConstructor`)
- Use **Java records** for immutable DTOs
- All REST responses wrapped in `ApiResponse<T>`
- All controllers annotated with **OpenAPI/Swagger** annotations
- All services use **constructor injection** (`@RequiredArgsConstructor`)
- Exception handling via `GlobalExceptionHandler` in iam-common
- All business entities extend `BaseEntity` (soft delete: deleted + deletedAt)
- **Flyway** migrations in `src/main/resources/db/migration/`
- **WebClient** (not RestTemplate) for inter-service calls
- **Resilience4j** circuit breakers on all outbound REST calls
- Pagination via Spring Data `Pageable` with standard query params

## Security Patterns

- Keycloak JWT → `KeycloakJwtAuthenticationConverter` → Spring Security authorities
- Realm role `"iam-admin"` → `ROLE_iam-admin` authority
- Client role `"read"` on `"iam-core-service"` → `ROLE_iam-core-service_read` authority
- Keycloak clients: `confidential` access type with `Service Account Enabled` (NOT `bearer-only` — removed in Keycloak 20+)
- X-Road endpoints (`/xroad/**`) are permitAll — authenticated by X-Road Security Server via `XRoadRequestFilter`
- X-Road context stored in `ThreadLocal<XRoadContextDto>` (servlet services only — NOT in reactive gateway)
- Config Service native endpoints (`/{app}/{profile}`) are permitAll (bootstrap before JWT available)

### Service-Specific Security

| Service | Public Endpoints | Protected Endpoints |
|---------|-----------------|-------------------|
| iam-gateway (:8081) | /actuator/health | All routes require valid JWT |
| iam-core-service (:8082) | /actuator/health, /xroad/** | /api/v1/persons/** → internal-user, tenant-admin, iam-admin; /api/v1/entities/** → internal-user, tenant-admin, iam-admin; /api/v1/representations/** → tenant-admin, iam-admin; /api/v1/users/** → internal-user, tenant-admin |
| iam-tenant-service (:8083) | /actuator/health | /api/v1/tenants/** → iam-admin, tenant-admin |
| iam-audit-service (:8084) | /actuator/health | /api/v1/audit/** → auditor, iam-admin, report-viewer |
| iam-xroad-adapter (:8085) | /actuator/health, /xroad/** | /api/v1/xroad/** → service-manager, iam-admin |
| iam-admin-service (:8086) | /actuator/health | /api/v1/platform-admin/org/** → tenant-admin; /api/v1/platform-admin/sector/** → sector-admin; /api/v1/platform-admin/platform/** → iam-admin; /api/v1/platform-admin/users/** → iam-admin, tenant-admin; /api/v1/platform-admin/settings → iam-admin, config-admin |
| iam-monitoring-service (:8087) | /actuator/health | /api/v1/monitoring/** → ops-admin, iam-admin |
| iam-governance-service (:8088) | /actuator/health | /api/v1/governance/** → governance-admin; reports → report-viewer |
| iam-developer-portal (:8089) | /actuator/health, /api/v1/docs/**, /api/v1/sdks | /api/v1/apps/** → developer; /api/v1/webhooks/** → developer |
| iam-notification-service (:8090) | /actuator/health | /api/v1/notifications/** → iam-admin, ops-admin |
| iam-config-service (:8888) | /actuator/health, /{app}/{profile} | /api/v1/config/** → config-admin, iam-admin |

## Service Ports

| Service | Port | Status |
|---------|------|--------|
| Keycloak | 8080 | Docker |
| API Gateway | 8081 | Module |
| Core Service | 8082 | Module |
| Tenant Service | 8083 | Module |
| Audit Service | 8084 | Module |
| X-Road Adapter | 8085 | Module |
| Admin Service | 8086 | Module |
| Monitoring Service | 8087 | Module |
| Governance Service | 8088 | Module |
| Developer Portal | 8089 | Module |
| Notification Service | 8090 | Module |
| Config Service | 8888 | Module |
| PostgreSQL | 5432 | Docker |
| Redis | 6379 | Docker |
| Kafka | 9092 | Docker |
| Elasticsearch | 9200 | Docker |
| Prometheus | 9090 | Docker |
| Grafana | 3000 | Docker |
| Vault 1.15 | 8200 | Docker |
| Kibana | 5601 | Docker |
| Mailpit | 8025 | Docker |

## X-Road Identity

- **Instance:** KH (Cambodia)
- **Member Classes:** GOV, COM, NGO, MUN
- **Example GOV Members:** GDT, MOF, MEF, MoI, MoH, MoE, NBC
- **Example COM Members:** ABA Bank, Wing Money, Smart Axiata
- **Example NGO Members:** UNDP, World Bank, ADB
- **Example MUN Members:** Phnom Penh, Siem Reap, Battambang

## Inter-Service Dependencies

```
iam-common ← all services
iam-config-service ← all services pull config (MUST START FIRST)
iam-gateway → routes to all services
iam-core-service → Kafka (iam.audit.events)
iam-tenant-service → Keycloak Admin API
iam-audit-service ← Kafka (iam.audit.events, iam.xroad.events) → Elasticsearch
iam-xroad-adapter → X-Road Security Server, PostgreSQL (iam_xroad), Redis (ACL cache)
iam-admin-service → Keycloak Admin API, Kafka (iam.audit.events, iam.notification.commands)
iam-monitoring-service → all /actuator endpoints, Prometheus, Redis, Kafka (iam.alert.triggers)
iam-governance-service → Keycloak Admin API, Kafka (iam.audit.events, iam.notification.commands, iam.platform.events)
iam-developer-portal → Keycloak Admin API, Kafka (iam.platform.events consumer)
iam-notification-service ← Kafka (iam.notification.commands, iam.alert.triggers) → SMTP, SMS, Telegram
```

## Environment Profiles: DEV / UAT / PROD

Every service MUST have 4 YAML files:
- `application.yml` — shared defaults
- `application-dev.yml` — local development (ddl-auto: update, Flyway disabled, hardcoded secrets)
- `application-uat.yml` — integration testing (ddl-auto: validate, Flyway enabled, env vars for secrets)
- `application-prod.yml` — production (ddl-auto: validate, SSL enabled, env vars, strict pool sizes)

| Config | DEV | UAT | PROD |
|--------|-----|-----|------|
| ddl-auto | update | validate | validate |
| Flyway | disabled | enabled | enabled |
| Secrets | Hardcoded (dev-only) | .env.uat (not in git) | Vault 1.15 + auto-rotation |
| SSL | off | optional | required |
| Logging | DEBUG | INFO | WARN + INFO (app) |
| Replicas (K8s) | 1 | 2 | 3+ |

Docker Compose files:
- `docker-compose.yml` + `docker-compose.dev.yml` (local)
- `docker-compose.yml` + `docker-compose.uat.yml` (shared server)
- K8s for production: `kubectl apply -k k8s/overlays/prod/`

K8s overlays: `k8s/overlays/{dev,uat,prod}/`

## API Versioning

- **URL path versioning:** `/api/v1/`, `/api/v2/`
- v1 endpoints maintained indefinitely with deprecation notices
- Breaking changes require new version path
- Non-breaking additions allowed in current version

## Startup Order

1. Infrastructure: `docker compose up -d`
2. Config Service: `iam-config-service` (must be ready before others)
3. Core services: gateway, core-service, tenant-service, audit-service, xroad-adapter
4. Expansion: admin, monitoring, governance, developer-portal, notification

## Build & Run

```bash
# Infrastructure
cd docker && docker compose up -d

# Build all modules
mvn clean package -DskipTests

# Start config service FIRST
cd iam-config-service && mvn spring-boot:run &

# Then start other services
cd iam-core-service && mvn spring-boot:run &
# ... etc

# Run tests
mvn test
```

## Git Commit Convention

```
Phase X: Short description

Examples:
Phase 1: Maven multi-module project structure
Phase 12: Config service with Spring Cloud Config Server
Phase 15: Monitoring service with health aggregation
```

## Deployment Strategy

### Data Localization & Sovereignty

**Critical:** Cambodia's Draft Law on Personal Data Protection (LPDP, July 2025) includes Article 22 (prohibits cross-border transfer without authorization) and Article 24 (mandates local storage of personal data collected in Cambodia). The NBC Technology Risk Management Guidelines (2019) already require BFIs to store critical data within Cambodia. Since RikReay serves ALL sectors (GOV, COM, NGO, MUN), the platform MUST be deployed on Cambodia-based infrastructure for production.

**Deployment Rule:** Production data (identity, audit, governance) must reside on infrastructure physically located in Cambodia. Use Cambodia-based data centers (Daun Penh Data Center, MekongNet, Chaktomuk, or Kepstar) or on-premises servers. Google Cloud (Singapore region) is acceptable only for DEV/UAT or non-personal-data workloads.

### Environments

| Environment | Location | Domain | Purpose |
|-------------|----------|--------|---------|
| DEV | Local PC (developer machine) | localhost | Development & testing |
| UAT | Google Cloud VM (Singapore region) or Cambodia DC | rikreay-uat.duckdns.org | Integration testing, stakeholder review |
| PROD | Cambodia-based data center (Phnom Penh) | rikreay.duckdns.org | Production (migrate to official domain later) |

**Domain Strategy:**
- Phase 1: Use DuckDNS free subdomains (rikreay-uat.duckdns.org, rikreay.duckdns.org)
- Phase 2: Register official domain and update DNS + TLS certs
- TLS: Let's Encrypt via Certbot (auto-renewal) on Google VM

### DEV (Local PC)
- Run with `docker compose -f docker-compose.yml -f docker-compose.dev.yml up -d`
- Services run locally via `mvn spring-boot:run` or Docker
- All ports exposed on localhost
- No TLS, `SPRING_PROFILES_ACTIVE=dev`
- Hardcoded dev-only secrets in application-dev.yml

### UAT (Google Cloud VM or Cambodia DC)
- Google Cloud VM (Singapore region) for non-sensitive testing, OR Cambodia DC for full compliance testing
- `docker compose -f docker-compose.yml -f docker-compose.uat.yml up -d`
- Nginx reverse proxy with Let's Encrypt TLS on rikreay-uat.duckdns.org
- `SPRING_PROFILES_ACTIVE=uat`
- Secrets via `.env.uat` file (NOT checked into git)
- DuckDNS cron job for dynamic DNS updates

### PROD (Cambodia-Based Data Center — MANDATORY)
- Cambodia-based hosting: Daun Penh Data Center (DPDC), MekongNet, Chaktomuk, Kepstar, or on-premises
- Required for NBC BFI compliance and upcoming LPDP data localization (Articles 22, 24)
- `docker compose -f docker-compose.yml -f docker-compose.prod.yml up -d` (or K8s when ready)
- Nginx reverse proxy with Let's Encrypt TLS on rikreay.duckdns.org
- `SPRING_PROFILES_ACTIVE=prod`
- Secrets via Vault or `.env.prod` (NOT checked into git)
- DuckDNS cron job, migrate to official domain + Cloud DNS later

### Nginx Reverse Proxy (UAT & PROD)
- All services behind Nginx on the Google VM
- Keycloak: `https://{domain}/auth/**`
- Gateway: `https://{domain}/api/**`
- Swagger: `https://{domain}/swagger/**`
- Kibana: `https://{domain}/kibana/**` (restricted access)
- Grafana: `https://{domain}/grafana/**` (restricted access)

### DuckDNS Setup (on Google VM)
```bash
# /opt/duckdns/duck.sh
echo url="https://www.duckdns.org/update?domains=rikreay&token=YOUR_TOKEN&ip=" | curl -k -o /opt/duckdns/duck.log -K -
# Cron: */5 * * * * /opt/duckdns/duck.sh >/dev/null 2>&1
```

## DO NOT

- Do NOT implement ABAC or attribute-based authorization
- Do NOT add custom token mappers for department/clearance/org_id claims
- Do NOT use `hasAuthority()` for custom attribute prefixes
- Do NOT use Spring Security's deprecated `WebSecurityConfigurerAdapter`
- Do NOT store secrets in application.yml for production profiles
- Do NOT make this tax-specific — it serves ALL sectors in Cambodia
- Do NOT change the X-Road instance identifier from KH
- Do NOT change RBAC to ABAC
- Do NOT use RestTemplate — use WebClient with Resilience4j
- Do NOT use ThreadLocal in iam-gateway (it's reactive)
- Do NOT use ddl-auto in production — use Flyway migrations
- Do NOT query another service's database directly — use REST APIs
- Do NOT use `/api/v1/admin/**` in iam-admin-service — use `/api/v1/platform-admin/**`
- Do NOT use dash-notation for Kafka topics — use dot-notation (iam.audit.events)
- Do NOT hardcode secrets in application-uat.yml or application-prod.yml — use env vars or Vault
- Do NOT enable ddl-auto: update in UAT or PROD — use Flyway migrations only
- Do NOT skip creating application-uat.yml — all 3 profiles (dev, uat, prod) are required
- Do NOT use Keycloak versions before 26.0 — critical CVEs in earlier versions (CVE-2024-3656)
- Do NOT use unpinned Keycloak image tags — always use specific patch version (e.g., 26.5.5)
- Do NOT use `bearer-only` client type — removed in Keycloak 20+, use confidential with service account
- Do NOT use Spring Boot 3.3.x or 3.4.x — both are EOL, use 3.5.x with Spring Cloud 2025.0.x
- Do NOT use `@Where` annotation — deprecated in Hibernate 6.3+, use `@SQLRestriction` instead
- Do NOT add `spring-boot-starter-web` to iam-gateway — it's reactive (WebFlux), this causes conflicts
- Do NOT use `springdoc-openapi-starter-webmvc-ui` in iam-gateway — use `springdoc-openapi-starter-webflux-ui`

## Documentation References

| Document | Path | Description |
|----------|------|-------------|
| README | `README.md` | Architecture overview, quick start, test users |
| CLAUDE.md | `CLAUDE.md` | Claude Code instructions, key decisions, conventions |
| RBAC Quick Reference | `docs/RBAC_QUICK_REFERENCE.md` | Complete endpoint-to-role mapping for all services |
| Development Guide | `docs/DEVELOPMENT_GUIDE_FINAL.md` | 22-phase build guide |
| Architecture Handoff | `docs/HANDOFF.md` | Architecture decisions and handoff notes |
| Technical Document | `docs/TECHNICAL_DOCUMENT.md` | Detailed technical specification |
