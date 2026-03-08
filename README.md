# RikReay IAM Enterprise Platform

Cambodia's national **Identity, Access Management & Interoperability** platform built on Keycloak + Spring Boot + X-Road with RBAC authorization.

Serves **all sectors** in Cambodia: Government (GOV), Commercial/Private (COM), NGOs (NGO), and Municipal Government (MUN).

---

## Architecture: 5 Domains, 12 Modules

```
┌─────────────────────────────────────────────────────────────────┐
│                     iam-gateway (:8081)                         │
│              Spring Cloud Gateway (Reactive)                    │
│              JWT validation · Rate limiting                     │
└──────┬──────┬──────┬──────┬──────┬──────┬──────┬──────┬────────┘
       │      │      │      │      │      │      │      │
  ┌────▼──┐┌──▼───┐┌─▼──┐┌─▼──┐┌──▼──┐┌──▼──┐┌──▼──┐┌──▼──┐
  │ Core  ││Tenant││Audit││XRd ││Admin││ Mon ││ Gov ││ Dev │
  │ :8082 ││:8083 ││:8084││:8085││:8086││:8087││:8088││:8089│
  └───────┘└──────┘└─────┘└────┘└─────┘└─────┘└─────┘└─────┘
                                              ┌──────┐┌──────┐
                                              │Notif ││Config│
                                              │:8090 ││:8888 │
                                              └──────┘└──────┘
```

### Domain 1: Identity & Access Core

| Module | Port | Description |
|--------|------|-------------|
| `iam-common` | — | Shared DTOs, RBAC constants, JWT converter, security auto-config, exceptions |
| `iam-gateway` | 8081 | Spring Cloud Gateway (reactive), JWT validation, rate limiting, routing |
| `iam-core-service` | 8082 | User profiles (Natural Person, Legal Entity, Representation), RBAC-secured APIs, X-Road endpoints |
| `iam-xroad-adapter` | 8085 | X-Road Security Server bridge, service routing, ACL management |

### Domain 2: Administration & Configuration

| Module | Port | Description |
|--------|------|-------------|
| `iam-tenant-service` | 8083 | Realm provisioning via Keycloak Admin API |
| `iam-admin-service` | 8086 | Unified admin APIs (`/api/v1/platform-admin/**`), dashboard, bulk operations |
| `iam-config-service` | 8888 | Spring Cloud Config Server, feature flags, centralized configuration |

### Domain 3: Monitoring & Operations

| Module | Port | Description |
|--------|------|-------------|
| `iam-monitoring-service` | 8087 | Health aggregation, auth analytics, X-Road exchange metrics, incidents, alerts |

### Domain 4: Identity Governance & Compliance

| Module | Port | Description |
|--------|------|-------------|
| `iam-audit-service` | 8084 | Kafka consumer, Elasticsearch indexer, audit query APIs |
| `iam-governance-service` | 8088 | Access reviews, certification campaigns, SoD policies, risk scoring, compliance reports |

### Domain 5: Developer & Integration Portal

| Module | Port | Description |
|--------|------|-------------|
| `iam-developer-portal` | 8089 | API docs, self-service app registration, webhooks, sandbox environments |

### Cross-Platform Service

| Module | Port | Description |
|--------|------|-------------|
| `iam-notification-service` | 8090 | Email/SMS/Telegram notifications, alert routing, scheduled reports |

---

## Technology Stack

| Layer | Technology | Version |
|-------|-----------|---------|
| Identity Provider | Keycloak (Quarkus) | 26.5.x |
| Backend | Spring Boot | 3.5.x |
| Cloud Release Train | Spring Cloud | 2025.0.x (Northfields) |
| Security | Spring Security 6.x | — |
| API Gateway | Spring Cloud Gateway 4.x (reactive) | — |
| Authorization | **RBAC only** (hasRole / hasAnyRole) | — |
| Protocol | OAuth 2.0 + OIDC, JWT RS256, PKCE (S256) | — |
| Java | OpenJDK | 21 (LTS) |
| Database | PostgreSQL | 16 |
| Cache | Redis | 7 |
| Events | Apache Kafka | Confluent 7.6 |
| Search | Elasticsearch | 8.12 |
| Resilience | Resilience4j | — |
| Migrations | Flyway | — |
| Secrets | HashiCorp Vault | 1.15 |
| Containers | Docker + Kubernetes | — |
| Data Exchange | X-Road 7.x (KH instance) | — |
| Monitoring | Prometheus + Grafana | — |

---

## RBAC Roles (13 Realm Roles)

| Role | Tier | Description |
|------|------|-------------|
| `iam-admin` | 0 | Full platform super administrator |
| `ops-admin` | 1 | Monitoring dashboards, incident management |
| `config-admin` | 1 | Platform configuration, feature flags |
| `governance-admin` | 1 | Access reviews, certifications, policies |
| `sector-admin` | 2 | Sector oversight for one member class (GOV/COM/NGO/MUN) |
| `tenant-admin` | 3 | Organization admin — manages their own realm |
| `service-manager` | — | X-Road service registrations |
| `auditor` | — | Read-only audit log access |
| `report-viewer` | — | Compliance and audit reports |
| `developer` | — | Self-service app registration, API docs, sandbox |
| `internal-user` | — | Internal staff user |
| `external-user` | — | External citizen/partner user |
| `api-access` | — | Basic API access (gateway pass-through) |

### Administration Tiers

| Tier | Role(s) | Scope |
|------|---------|-------|
| 0: Platform Super Admin | `iam-admin` | Entire platform, all tenants, infrastructure |
| 1: Platform Operations | `ops-admin`, `config-admin`, `governance-admin`, `auditor` | Platform-wide operations |
| 2: Sector Admin | `sector-admin` | All organizations within one member class |
| 3: Organization Admin | `tenant-admin` | Single organization (single Keycloak realm) |

---

## Inter-Service Dependencies

```
iam-common ← all services (shared library)
iam-config-service ← all services pull config (MUST START FIRST)
iam-gateway → routes to all 12 services

iam-core-service → Kafka (iam.audit.events, iam.platform.events)
iam-tenant-service → Keycloak Admin API
iam-audit-service ← Kafka (iam.audit.events, iam.xroad.events) → Elasticsearch
iam-xroad-adapter → X-Road Security Server, Redis (ACL cache)
iam-admin-service → Keycloak Admin API, Kafka (iam.audit.events, iam.notification.commands)
iam-monitoring-service → all /actuator endpoints, Prometheus, Redis, Kafka (iam.alert.triggers)
iam-governance-service → Keycloak Admin API, Kafka (iam.audit.events, iam.notification.commands)
iam-developer-portal → Keycloak Admin API, Kafka (iam.platform.events consumer)
iam-notification-service ← Kafka (iam.notification.commands, iam.alert.triggers) → SMTP/SMS/Telegram
```

### Kafka Topic Registry

| Topic | Producer(s) | Consumer(s) |
|-------|------------|-------------|
| `iam.audit.events` | All services | iam-audit-service |
| `iam.xroad.events` | core, xroad-adapter | iam-audit-service |
| `iam.notification.commands` | monitoring, governance, admin | iam-notification-service |
| `iam.platform.events` | core, tenant, admin, governance | iam-developer-portal |
| `iam.alert.triggers` | iam-monitoring-service | iam-notification-service |

---

## Quick Start

### Prerequisites

- Java 21 (OpenJDK)
- Maven 3.9+
- Docker & Docker Compose

### 1. Start Infrastructure

```bash
cd docker
docker compose up -d
```

This starts: PostgreSQL, Keycloak, Redis, Kafka, Elasticsearch, Prometheus, Grafana, Vault, Kibana, Mailpit.

### 2. Build All Modules

```bash
mvn clean package -DskipTests
```

### 3. Start Services (Order Matters)

```bash
# Step 1: Config Service FIRST (all services depend on it)
cd iam-config-service && mvn spring-boot:run &

# Step 2: Wait for config service health check
# http://localhost:8888/actuator/health

# Step 3: Start remaining services
cd iam-gateway && mvn spring-boot:run &
cd iam-core-service && mvn spring-boot:run &
cd iam-tenant-service && mvn spring-boot:run &
cd iam-audit-service && mvn spring-boot:run &
cd iam-xroad-adapter && mvn spring-boot:run &
cd iam-admin-service && mvn spring-boot:run &
cd iam-monitoring-service && mvn spring-boot:run &
cd iam-governance-service && mvn spring-boot:run &
cd iam-developer-portal && mvn spring-boot:run &
cd iam-notification-service && mvn spring-boot:run &
```

### Using Docker Compose Profiles

```bash
cd docker

# Config service only (must start first)
docker compose -f docker-compose.yml -f docker-compose.services.yml --profile config up -d

# Expansion services (after config is healthy)
docker compose -f docker-compose.yml -f docker-compose.services.yml --profile expansion up -d

# All services
docker compose -f docker-compose.yml -f docker-compose.services.yml --profile all up -d
```

### 4. Run Tests

```bash
mvn test
```

---

## API Documentation

Each service exposes OpenAPI/Swagger documentation:

| Service | Swagger UI | OpenAPI JSON |
|---------|-----------|--------------|
| Core Service | http://localhost:8082/swagger-ui.html | http://localhost:8082/v3/api-docs |
| Tenant Service | http://localhost:8083/swagger-ui.html | http://localhost:8083/v3/api-docs |
| Audit Service | http://localhost:8084/swagger-ui.html | http://localhost:8084/v3/api-docs |
| X-Road Adapter | http://localhost:8085/swagger-ui.html | http://localhost:8085/v3/api-docs |
| Admin Service | http://localhost:8086/swagger-ui.html | http://localhost:8086/v3/api-docs |
| Monitoring Service | http://localhost:8087/swagger-ui.html | http://localhost:8087/v3/api-docs |
| Governance Service | http://localhost:8088/swagger-ui.html | http://localhost:8088/v3/api-docs |
| Developer Portal | http://localhost:8089/swagger-ui.html | http://localhost:8089/v3/api-docs |
| Notification Service | http://localhost:8090/swagger-ui.html | http://localhost:8090/v3/api-docs |
| Config Service | http://localhost:8888/swagger-ui.html | http://localhost:8888/v3/api-docs |

Developer Portal aggregated docs (public): http://localhost:8089/api/v1/docs

---

## Test Users

All users are in the `iam-platform` Keycloak realm. Default password format: `Role@2026`.

| Username | Password | Realm Roles | Tier |
|----------|----------|-------------|------|
| `admin-user` | `Admin@2026` | iam-admin, api-access | 0 |
| `config.admin` | `ConfigAdmin@2026` | config-admin, internal-user, api-access | 1 |
| `ops.admin` | `OpsAdmin@2026` | ops-admin, internal-user, api-access | 1 |
| `gov.admin` | `GovAdmin@2026` | governance-admin, internal-user, api-access | 1 |
| `sector.admin` | `SectorAdmin@2026` | sector-admin, internal-user, api-access | 2 |
| `tax.officer` | `TaxOfficer@2026` | internal-user, api-access | — |
| `auditor.user` | `Auditor@2026` | auditor, report-viewer, internal-user, api-access | — |
| `dev.user` | `DevUser@2026` | developer, api-access | — |
| `report.viewer` | `ReportViewer@2026` | report-viewer, internal-user, api-access | — |
| `citizen.user` | `Citizen@2026` | external-user, api-access | — |
| `partner.user` | `Partner@2026` | external-user, api-access | — |

### Obtaining a Token

```bash
# Get access token for admin-user
curl -X POST http://localhost:8080/realms/iam-platform/protocol/openid-connect/token \
  -d "grant_type=password" \
  -d "client_id=iam-gateway" \
  -d "client_secret=gateway-secret-dev-only" \
  -d "username=admin-user" \
  -d "password=Admin@2026"
```

---

## Database Isolation

Each service owns its own PostgreSQL database — services NEVER query another service's database directly.

| Service | Database |
|---------|----------|
| Keycloak | `keycloak_db` |
| Core Service | `iam_core` |
| Tenant Service | `iam_tenant` |
| Audit Service | `iam_audit` |
| X-Road Adapter | `iam_xroad` |
| Admin Service | `iam_admin` |
| Monitoring Service | `iam_monitoring` |
| Governance Service | `iam_governance` |
| Developer Portal | `iam_developer` |
| Notification Service | `iam_notification` |
| Config Service | `iam_config` |

---

## X-Road Identity

- **Instance:** KH (Cambodia)
- **Member Classes:** GOV, COM, NGO, MUN
- **Protocol:** X-Road 7.x via CamDX

| Class | Example Members |
|-------|----------------|
| GOV | GDT, MOF, MEF, MoI, MoH, MoE, NBC |
| COM | ABA Bank, Wing Money, Smart Axiata |
| NGO | UNDP, World Bank, ADB |
| MUN | Phnom Penh, Siem Reap, Battambang |

---

## Deployment

| Environment | Location | Profile | Domain |
|-------------|----------|---------|--------|
| DEV | Local machine | `dev` | localhost |
| UAT | Google Cloud / Cambodia DC | `uat` | rikreay-uat.duckdns.org |
| PROD | Cambodia data center (mandatory) | `prod` | rikreay.duckdns.org |

Production deployment uses Kubernetes with Kustomize overlays:

```bash
kubectl apply -k k8s/overlays/prod/
```

---

## Project Structure

```
rikreay-iam-platform/
├── iam-common/                    # Shared library
├── iam-gateway/                   # API Gateway (:8081)
├── iam-core-service/              # Identity core (:8082)
├── iam-tenant-service/            # Tenant management (:8083)
├── iam-audit-service/             # Audit & compliance (:8084)
├── iam-xroad-adapter/             # X-Road bridge (:8085)
├── iam-admin-service/             # Administration (:8086)
├── iam-monitoring-service/        # Operations (:8087)
├── iam-governance-service/        # Governance (:8088)
├── iam-developer-portal/          # Developer portal (:8089)
├── iam-notification-service/      # Notifications (:8090)
├── iam-config-service/            # Config server (:8888)
├── docker/                        # Docker Compose files
├── k8s/                           # Kubernetes manifests
│   ├── base/                      # Base resources
│   └── overlays/{dev,uat,prod}/   # Environment patches
├── keycloak/realms/               # Keycloak realm export
├── docs/                          # Documentation
└── pom.xml                        # Parent POM
```

---

## License

Copyright 2026 RikReay IAM Platform Team. All rights reserved.
