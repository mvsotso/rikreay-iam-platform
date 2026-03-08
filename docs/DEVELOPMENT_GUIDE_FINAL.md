# RikReay IAM Enterprise Platform — Claude Code Development Guide (Complete)

## Overview

This is the **complete, consolidated development guide** for building the RikReay IAM Enterprise Platform from scratch using **Claude Code Desktop**. It covers all 22 phases — from empty repository to full 12-module production platform.

**Project:** RikReay IAM Enterprise Platform — Cambodia's national Identity, Access Management & Interoperability platform
**GitHub:** https://github.com/mvsotso/rikreay-iam-platform.git
**Scope:** National platform for all sectors — GOV, COM, NGO, MUN (NOT tax-specific)
**Authorization:** RBAC only (NOT ABAC)

---

## Technology Stack

| Layer | Technology | Version |
|-------|-----------|---------|
| Identity Provider | Keycloak (Quarkus) | **26.5.x** (pin specific patch, e.g., 26.5.5) |
| Backend | Spring Boot | **3.5.x** (latest stable patch) |
| Security | Spring Security 6.x | — |
| API Gateway | Spring Cloud Gateway 4.x | — |
| Authorization | **RBAC only** | — |
| Protocol | OAuth 2.0 + OIDC, JWT RS256, PKCE | — |
| Database | PostgreSQL 16 | schema-per-service |
| Cache | Redis 7 | — |
| Events | Apache Kafka | dot-notation topics |
| Search | Elasticsearch 8 | — |
| Config | Spring Cloud Config Server | — |
| Cloud Release Train | Spring Cloud **2025.0.x** (Northfields) | — |
| Monitoring | Prometheus + Grafana | — |
| Resilience | Resilience4j | — |
| Migrations | Flyway | — |
| Secrets | HashiCorp Vault 1.15 | — |
| Containers | Docker + Kubernetes | — |
| CI/CD | GitHub Actions | — |
| Java | OpenJDK 21 (LTS) | — |
| Data Exchange | X-Road 7.x (KH instance) | — |

---

## Architecture Standards (Apply to ALL Phases)

These standards must be followed in every phase. Claude Code must reference these when generating code.

### Database Isolation — Schema Per Service

Each microservice owns its own PostgreSQL database. Services NEVER query another service's database directly — use REST APIs for cross-service data.

| Service | Database Name | Notes |
|---------|--------------|-------|
| Keycloak | keycloak_db | Managed by Keycloak |
| iam-core-service | iam_core | User profiles, core data |
| iam-tenant-service | iam_tenant | Tenant metadata |
| iam-audit-service | iam_audit | Also uses Elasticsearch |
| iam-xroad-adapter | iam_xroad | Service registry, ACL |
| iam-admin-service | iam_admin | Admin settings, bulk ops |
| iam-monitoring-service | iam_monitoring | Incidents, alerts |
| iam-governance-service | iam_governance | Campaigns, policies, workflows |
| iam-developer-portal | iam_developer | Apps, webhooks, sandboxes |
| iam-notification-service | iam_notification | Templates, channels, logs |
| iam-config-service | iam_config | Feature flags, change logs |

### Kafka Topic Registry

All services must use these exact topic names (dot-notation):

| Topic | Producer(s) | Consumer(s) | DTO |
|-------|------------|-------------|-----|
| `iam.audit.events` | All services | iam-audit-service | AuditEventDto |
| `iam.xroad.events` | iam-core-service, iam-xroad-adapter | iam-audit-service | AuditEventDto |
| `iam.notification.commands` | monitoring, governance, admin | iam-notification-service | NotificationCommandDto |
| `iam.platform.events` | core, tenant, admin, governance | iam-developer-portal (webhooks) | PlatformEventDto |
| `iam.alert.triggers` | iam-monitoring-service | iam-notification-service | AlertTriggerDto |

### Standard application.yml Template

Every service MUST follow this template:

```yaml
server:
  port: ${SERVICE_PORT}

spring:
  application:
    name: ${SERVICE_NAME}
  profiles:
    active: ${SPRING_PROFILES_ACTIVE:dev}
  config:
    import: optional:configserver:http://${CONFIG_SERVER_HOST:localhost}:8888
  datasource:
    url: jdbc:postgresql://${POSTGRES_HOST:localhost}:5432/${DB_NAME}
    username: ${POSTGRES_USER:iam_admin}
    password: ${POSTGRES_PASSWORD:iam_secret_2026}
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
  jpa:
    hibernate:
      ddl-auto: ${JPA_DDL_AUTO:validate}
    show-sql: false
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect
  flyway:
    enabled: true
    locations: classpath:db/migration
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: ${KEYCLOAK_URL:http://localhost:8080}/realms/${KEYCLOAK_REALM:iam-platform}
          jwk-set-uri: ${KEYCLOAK_URL:http://localhost:8080}/realms/${KEYCLOAK_REALM:iam-platform}/protocol/openid-connect/certs
  kafka:
    bootstrap-servers: ${KAFKA_SERVERS:localhost:9092}
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.springframework.kafka.support.serializer.JsonSerializer
      acks: all

management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  endpoint:
    health:
      show-details: when_authorized
  metrics:
    tags:
      application: ${spring.application.name}

app:
  cors:
    allowed-origins:
      - http://localhost:4200
      - http://localhost:3000
      - http://localhost:5173

logging:
  level:
    root: INFO
    com.iam.platform: DEBUG
```

### API Response Contracts

**Success response:**
```json
{
  "success": true,
  "data": { },
  "message": "Operation completed",
  "timestamp": "2026-03-08T10:00:00Z",
  "requestId": "uuid"
}
```

**Error response:**
```json
{
  "success": false,
  "data": null,
  "message": "User not found",
  "errorCode": "USER_NOT_FOUND",
  "timestamp": "2026-03-08T10:00:00Z",
  "path": "/api/v1/users/123"
}
```

**Paginated response — use Spring Data Pageable for all list endpoints:**
Standard query params: `?page=0&size=20&sort=createdAt,desc`

### Inter-Service Communication

- All inter-service REST calls use **WebClient** (not RestTemplate)
- Circuit breaker: Resilience4j with fallback for all outbound calls
- Timeout: 5s connect, 10s read
- Retry: 3 attempts with exponential backoff (GET only)
- Service URLs managed via Config Service (dev) / K8s DNS (prod)

### Resilience4j Default Configuration

All services using WebClient for inter-service calls must include this config:

```yaml
resilience4j:
  circuitbreaker:
    instances:
      default:
        sliding-window-size: 10
        failure-rate-threshold: 50
        wait-duration-in-open-state: 10s
        permitted-number-of-calls-in-half-open-state: 3
  retry:
    instances:
      default:
        max-attempts: 3
        wait-duration: 1s
        enable-exponential-backoff: true
        exponential-backoff-multiplier: 2
        retry-exceptions:
          - java.net.ConnectException
          - java.net.SocketTimeoutException
  timelimiter:
    instances:
      default:
        timeout-duration: 10s
```

### Soft Delete Pattern

All business entities (users, apps, tenants, campaigns) use soft delete:
- `deleted` (boolean, default false)
- `deletedAt` (Instant, nullable)
- JPA: `@SQLRestriction("deleted = false")` (NOT `@Where` — deprecated in Hibernate 6.3+)
- Hard delete only for temporary data (sandbox realms, notification logs)

### Database Migrations

- **Flyway** for all schema changes
- Files in `src/main/resources/db/migration/`
- Naming: `V1__create_initial_schema.sql`, `V2__add_indexes.sql`
- `ddl-auto: validate` in UAT and PROD, `update` in DEV profile ONLY

---

## Environment Strategy: DEV / UAT / PROD

### Three Environments

| Aspect | DEV | UAT | PROD |
|--------|-----|-----|------|
| **Spring Profile** | `dev` | `uat` | `prod` |
| **Purpose** | Local development & testing | Integration testing, stakeholder review | Live production |
| **Infrastructure** | Docker Compose (local) | Docker Compose or K8s (shared server) | Kubernetes cluster |
| **Database** | Local PostgreSQL, ddl-auto: update | Shared PostgreSQL, ddl-auto: validate | Managed PostgreSQL (HA), ddl-auto: validate |
| **Keycloak** | Dev mode, auto-import realm | Standalone, manual realm config | Clustered (HA), managed certs |
| **Secrets** | Hardcoded in yml (dev-only) | Vault or K8s Secrets | Vault with auto-rotation |
| **SSL/TLS** | None (HTTP) | Self-signed or Let's Encrypt | Production certificates |
| **Logging** | DEBUG, console output | INFO, JSON format | INFO, JSON → ELK stack |
| **Kafka** | Single broker, auto-create topics | 3-broker cluster, manual topics | 3+ broker cluster, topic ACLs |
| **Elasticsearch** | Single node, no security | Single node, basic security | Multi-node cluster, TLS + auth |
| **Redis** | Single instance, no password | Single instance, password | Sentinel or Cluster, TLS |
| **X-Road** | Mock/dev containers | Test Security Server | Production Security Server |
| **Monitoring** | Optional Prometheus+Grafana | Prometheus+Grafana+alerting | Full observability stack |
| **Rate Limiting** | Disabled or very high | Moderate limits | Production limits per role |

### Spring Profile Files (per service)

Each service MUST have these 4 YAML files:

```
src/main/resources/
├── application.yml           — Shared defaults (profile-independent)
├── application-dev.yml       — DEV overrides
├── application-uat.yml       — UAT overrides
└── application-prod.yml      — PROD overrides (env vars only, no secrets)
```

**application.yml** — Base config (always loaded):
```yaml
# See Standard application.yml Template section above
# Contains: port, datasource template, kafka, security, management, flyway
```

**application-dev.yml:**
```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: update     # Auto-schema for rapid development
    show-sql: true
  flyway:
    enabled: false          # Disable Flyway in dev (ddl-auto handles schema)
  datasource:
    url: jdbc:postgresql://localhost:5432/${DB_NAME}
    username: iam_admin
    password: iam_secret_2026
  kafka:
    bootstrap-servers: localhost:9092
  data:
    redis:
      host: localhost
      password: redis_secret_2026
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: http://localhost:8080/realms/iam-platform

keycloak:
  auth-server-url: http://localhost:8080
  admin-username: admin
  admin-password: admin

logging:
  level:
    com.iam.platform: DEBUG
    org.springframework.security: DEBUG

app:
  cors:
    allowed-origins:
      - http://localhost:4200
      - http://localhost:3000
      - http://localhost:5173
```

**application-uat.yml:**
```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: validate     # Schema managed by Flyway only
    show-sql: false
  flyway:
    enabled: true
  datasource:
    url: jdbc:postgresql://${POSTGRES_HOST}:5432/${DB_NAME}
    username: ${POSTGRES_USER}
    password: ${POSTGRES_PASSWORD}
  kafka:
    bootstrap-servers: ${KAFKA_SERVERS}
  data:
    redis:
      host: ${REDIS_HOST}
      password: ${REDIS_PASSWORD}
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: ${KEYCLOAK_URL}/realms/iam-platform

keycloak:
  auth-server-url: ${KEYCLOAK_URL}
  admin-username: ${KEYCLOAK_ADMIN_USER}
  admin-password: ${KEYCLOAK_ADMIN_PASSWORD}

logging:
  level:
    root: INFO
    com.iam.platform: INFO

app:
  cors:
    allowed-origins: ${CORS_ORIGINS}
```

**application-prod.yml:**
```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: false
  flyway:
    enabled: true
  datasource:
    url: jdbc:postgresql://${POSTGRES_HOST}:5432/${DB_NAME}?ssl=true&sslmode=require
    username: ${POSTGRES_USER}
    password: ${POSTGRES_PASSWORD}
    hikari:
      maximum-pool-size: 30
      minimum-idle: 10
  kafka:
    bootstrap-servers: ${KAFKA_SERVERS}
    properties:
      security.protocol: SASL_SSL
  data:
    redis:
      host: ${REDIS_HOST}
      password: ${REDIS_PASSWORD}
      ssl:
        enabled: true
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: ${KEYCLOAK_URL}/realms/iam-platform

keycloak:
  auth-server-url: ${KEYCLOAK_URL}
  admin-username: ${KEYCLOAK_ADMIN_USER}
  admin-password: ${KEYCLOAK_ADMIN_PASSWORD}

server:
  ssl:
    enabled: ${SSL_ENABLED:false}

logging:
  level:
    root: WARN
    com.iam.platform: INFO

app:
  cors:
    allowed-origins: ${CORS_ORIGINS}
```

### Docker Compose Files (per environment)

```
docker/
├── docker-compose.yml              — Infrastructure (PostgreSQL, Redis, Kafka, etc.)
├── docker-compose.xroad.yml        — X-Road overlay
├── docker-compose.dev.yml          — DEV services (all on localhost, dev profile)
├── docker-compose.uat.yml          — UAT services (shared server, uat profile)
└── docker-compose.prod.yml         — PROD reference (use K8s instead)
```

Usage:
```bash
# DEV (local development)
docker compose -f docker-compose.yml -f docker-compose.dev.yml up -d

# UAT (shared test server)
docker compose -f docker-compose.yml -f docker-compose.uat.yml up -d

# PROD (use Kubernetes — compose file for reference only)
kubectl apply -k k8s/overlays/prod/
```

### Kubernetes Kustomize Overlays

```
k8s/
├── base/                    — Base manifests (all services)
│   ├── kustomization.yaml
│   ├── namespace.yaml
│   └── {service}/           — deployment, service, hpa, configmap per service
├── overlays/
│   ├── dev/                 — Dev overrides (1 replica, no resource limits, debug logging)
│   │   ├── kustomization.yaml
│   │   └── patches/
│   ├── uat/                 — UAT overrides (2 replicas, moderate limits, info logging)
│   │   ├── kustomization.yaml
│   │   └── patches/
│   └── prod/                — Prod overrides (3+ replicas, strict limits, warn logging, TLS)
│       ├── kustomization.yaml
│       ├── patches/
│       └── ingress-tls.yaml
```

### Config Service Environment Management

The Config Service manages environment-specific configuration centrally:

```
config-repo/               — Git repo (or classpath in dev)
├── application.yml         — Shared across all services and environments
├── application-dev.yml     — DEV defaults
├── application-uat.yml     — UAT defaults
├── application-prod.yml    — PROD defaults
├── iam-core-service.yml    — Service-specific (all environments)
├── iam-core-service-dev.yml
├── iam-core-service-uat.yml
├── iam-core-service-prod.yml
└── ... (repeat for each service)
```

Feature flags are environment-scoped:
```json
{ "key": "enable-xroad-v2", "enabled": true, "environment": "dev" }
{ "key": "enable-xroad-v2", "enabled": false, "environment": "uat" }
{ "key": "enable-xroad-v2", "enabled": false, "environment": "prod" }
```

---

### Deployment Strategy — Local Dev + Google VM (UAT & PROD)

#### Infrastructure Overview

**Data Localization Requirement:** Cambodia's Draft LPDP (Article 22, 24) mandates local storage of personal data. The NBC TRM Guidelines require BFI data sovereignty within Cambodia. Since RikReay serves ALL sectors (GOV, COM, NGO, MUN including potential BFI tenants), production MUST be on Cambodia-based infrastructure.

| Environment | Location | Domain | SSL | Purpose |
|-------------|----------|--------|-----|---------|
| **DEV** | Developer's local PC | localhost | None | Development & testing |
| **UAT** | Google Cloud VM (Singapore) or Cambodia DC | rikreay-uat.duckdns.org | Let's Encrypt | Integration testing, stakeholder review |
| **PROD** | **Cambodia-based data center (Phnom Penh)** | rikreay.duckdns.org | Let's Encrypt | Production — data localization compliant |

**Cambodia Data Center Options for PROD:**
- Daun Penh Data Center (DPDC) — Tier 3, colocation + VPS + cloud, 99.999% uptime
- MekongNet Data Center — colocation + private cloud, international backbone
- Chaktomuk Data Center — Tier 3, Sabay Digital
- Kepstar Data Centre — 2 facilities in Phnom Penh
- On-premises servers at GDT or partner ministry

**Domain Strategy:**
- Phase 1 (now): Use DuckDNS free subdomains — rikreay-uat.duckdns.org and rikreay.duckdns.org
- Phase 2 (later): Register official domain, update DNS records, and replace TLS certs
- TLS provided by Let's Encrypt via Certbot with auto-renewal cron

#### DEV Environment (Local PC)

```bash
# Start infrastructure
cd docker && docker compose -f docker-compose.yml -f docker-compose.dev.yml up -d

# Start services (config first, then others)
cd iam-config-service && mvn spring-boot:run -Dspring-boot.run.profiles=dev &
cd iam-core-service && mvn spring-boot:run -Dspring-boot.run.profiles=dev &
# ... etc
```

- All ports exposed on localhost (8080-8090, 8888)
- No TLS, no reverse proxy
- `SPRING_PROFILES_ACTIVE=dev`
- Hardcoded dev-only secrets in application-dev.yml

#### UAT & PROD Deployment

Both UAT and PROD run with Docker Compose behind Nginx with Let's Encrypt TLS.

**UAT** can use Google Cloud VM (Singapore region) for convenience during testing, OR a Cambodia-based server for full compliance testing.

**PROD MUST use Cambodia-based infrastructure** (Daun Penh DC, MekongNet, Kepstar, or on-premises) to satisfy NBC TRM data sovereignty and upcoming LPDP Article 24 data localization requirements.

**Server Setup (one-time per environment):**

```bash
# Option A: Google Cloud VM (UAT only)
gcloud compute instances create rikreay-uat \
  --machine-type=e2-standard-4 \
  --image-family=ubuntu-2404-lts-amd64 \
  --image-project=ubuntu-os-cloud \
  --boot-disk-size=100GB \
  --tags=http-server,https-server

# Option B: Cambodia DC (PROD — required)
# Provision a dedicated server or VPS at DPDC/MekongNet/Kepstar
# Install Ubuntu 24.04 LTS, then:
sudo apt update && sudo apt install -y docker.io docker-compose-v2 nginx certbot python3-certbot-nginx
sudo usermod -aG docker $USER

# 3. Setup DuckDNS
mkdir -p /opt/duckdns
cat > /opt/duckdns/duck.sh << 'EOF'
echo url="https://www.duckdns.org/update?domains=rikreay-uat&token=YOUR_DUCKDNS_TOKEN&ip=" | curl -k -o /opt/duckdns/duck.log -K -
EOF
chmod +x /opt/duckdns/duck.sh
(crontab -l 2>/dev/null; echo "*/5 * * * * /opt/duckdns/duck.sh >/dev/null 2>&1") | crontab -

# 4. Configure Nginx reverse proxy (see below)

# 5. Get TLS certificate
sudo certbot --nginx -d rikreay-uat.duckdns.org --non-interactive --agree-tos -m your@email.com
```

**Nginx Configuration (UAT example — adjust domain for PROD):**

```nginx
# /etc/nginx/sites-available/rikreay-iam
server {
    listen 80;
    server_name rikreay-uat.duckdns.org;
    return 301 https://$host$request_uri;
}

server {
    listen 443 ssl;
    server_name rikreay-uat.duckdns.org;

    ssl_certificate /etc/letsencrypt/live/rikreay-uat.duckdns.org/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/rikreay-uat.duckdns.org/privkey.pem;

    # Keycloak
    location /auth/ {
        proxy_pass http://localhost:8080/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_buffer_size 128k;
        proxy_buffers 4 256k;
    }

    # API Gateway (all API traffic)
    location /api/ {
        proxy_pass http://localhost:8081/api/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    # X-Road endpoints (via gateway)
    location /xroad/ {
        proxy_pass http://localhost:8081/xroad/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    # Grafana (restricted)
    location /grafana/ {
        proxy_pass http://localhost:3000/;
        proxy_set_header Host $host;
    }

    # Kibana (restricted)
    location /kibana/ {
        proxy_pass http://localhost:5601/;
        proxy_set_header Host $host;
    }
}
```

**Deploy to Google VM:**

```bash
# On the VM
git clone https://github.com/mvsotso/rikreay-iam-platform.git
cd rikreay-iam-platform/docker

# Create .env.uat from template (fill in real values)
cp .env.uat.template .env.uat
nano .env.uat

# Start everything
docker compose -f docker-compose.yml -f docker-compose.uat.yml --env-file .env.uat up -d
```

**Keycloak Frontend URL Configuration (critical for UAT/PROD):**

```yaml
# In docker-compose.uat.yml keycloak service:
environment:
  KC_HOSTNAME: rikreay-uat.duckdns.org
  KC_HOSTNAME_PATH: /auth
  KC_PROXY_HEADERS: xforwarded
  KC_HTTP_ENABLED: true  # Nginx handles TLS termination
```

#### Migration to Official Domain

When ready to migrate from DuckDNS to official domain:
1. Register domain (e.g., rikreay.gov.kh or rikreay.com.kh)
2. Point DNS A record to Google VM IP
3. Update Nginx server_name
4. Run `certbot --nginx -d rikreay.gov.kh`
5. Update Keycloak KC_HOSTNAME
6. Update CORS origins in application-uat.yml / application-prod.yml
7. Update redirect URIs in Keycloak clients

---

## Target Architecture: 5 Domains, 12 Modules

```
iam-enterprise-platform/
├── iam-common/               — Shared DTOs, constants, JWT converter, security auto-config
├── iam-gateway/              — Spring Cloud Gateway (port 8081)
├── iam-core-service/         — Core APIs, user profiles (port 8082)
├── iam-tenant-service/       — Realm provisioning (port 8083)
├── iam-audit-service/        — Kafka → Elasticsearch indexer (port 8084)
├── iam-xroad-adapter/        — X-Road bridge service (port 8085)
├── iam-admin-service/        — Unified admin APIs (port 8086)
├── iam-monitoring-service/   — Health, analytics, incidents (port 8087)
├── iam-governance-service/   — Compliance, campaigns, risk (port 8088)
├── iam-developer-portal/     — API docs, app registration (port 8089)
├── iam-notification-service/ — Email/SMS/Telegram (port 8090)
├── iam-config-service/       — Config Server, feature flags (port 8888)
├── docker/                   — Docker Compose
├── keycloak/                 — Realm JSON
├── k8s/                      — Kubernetes manifests
├── docs/                     — Guides and references
└── CLAUDE.md                 — Claude Code project instructions
```

---

## Phase 1: Project Foundation

### Prompt 1.1 — Create Parent Project & Module Structure

```
Create a Maven multi-module Spring Boot project for the RikReay IAM Enterprise Platform.

Parent project:
- groupId: com.iam.platform
- artifactId: iam-enterprise-platform
- version: 1.0.0-SNAPSHOT
- Spring Boot 3.5.x parent (latest stable patch)
- Java 21
- packaging: pom

Modules (create ALL 12 as subdirectories with their own pom.xml):
1. iam-common — Shared library (models, DTOs, utilities, constants)
2. iam-gateway — Spring Cloud Gateway (API edge service)
3. iam-core-service — Core business API microservice
4. iam-tenant-service — Tenant/realm management service
5. iam-audit-service — Audit log Kafka consumer + Elasticsearch indexer
6. iam-xroad-adapter — X-Road Security Server bridge service
7. iam-admin-service — Unified admin APIs
8. iam-monitoring-service — Health aggregation, analytics, incidents
9. iam-governance-service — Access reviews, compliance, risk scoring
10. iam-developer-portal — API docs, app registration, webhooks
11. iam-notification-service — Email/SMS/Telegram notifications
12. iam-config-service — Spring Cloud Config Server, feature flags

Parent POM dependencyManagement:
- Spring Cloud 2025.0.x (Northfields — compatible with Spring Boot 3.5.x)
- spring-boot-starter-web, spring-boot-starter-security
- spring-boot-starter-oauth2-resource-server
- spring-boot-starter-data-jpa, postgresql
- spring-boot-starter-data-redis
- spring-kafka
- spring-boot-starter-actuator, micrometer-registry-prometheus
- spring-boot-starter-validation
- spring-cloud-starter-circuitbreaker-resilience4j
- spring-boot-starter-mail
- flyway-core, flyway-database-postgresql
- lombok, jackson-databind
- springdoc-openapi-starter-webmvc-ui (2.8.0) — for servlet services
- springdoc-openapi-starter-webflux-ui (2.8.0) — for iam-gateway ONLY
- spring-boot-starter-data-elasticsearch — for audit-service
- spring-boot-starter-thymeleaf — for notification-service email templates
- spring-boot-starter-test, spring-security-test
- keycloak-admin-client (26.5.5)

IMPORTANT — Module dependency rules:
- Every service module (not iam-common itself) must depend on iam-common:
  <dependency>
    <groupId>com.iam.platform</groupId>
    <artifactId>iam-common</artifactId>
    <version>${project.version}</version>
  </dependency>
- iam-gateway is REACTIVE — it must use:
  - spring-cloud-starter-gateway (includes WebFlux)
  - spring-boot-starter-oauth2-resource-server
  - spring-boot-starter-data-redis-reactive
  - springdoc-openapi-starter-webflux-ui (NOT webmvc-ui)
  - Do NOT add spring-boot-starter-web to iam-gateway (conflicts with WebFlux)
- All other services use spring-boot-starter-web (servlet)

Each module should declare ONLY the dependencies it needs.
Create a basic Application.java main class in each service module (not iam-common).
Create CLAUDE.md at root (the project instruction file for Claude Code).
Create .gitignore for Java/Maven projects.
Create .sdkmanrc with: java=21.0.4-tem

Do NOT create Docker files yet. Do NOT create application.yml yet (except for iam-config-service which needs bootstrap config).
Do NOT add ABAC or attribute-based dependencies.
```

### Prompt 1.2 — Create Shared Common Library

```
In the iam-common module, create the following shared classes.

1. Constants: com.iam.platform.common.constants.IamRoles
   ALL RBAC role constants (13 realm roles):
   - ROLE_IAM_ADMIN = "iam-admin"
   - ROLE_TENANT_ADMIN = "tenant-admin"
   - ROLE_SECTOR_ADMIN = "sector-admin"
   - ROLE_SERVICE_MANAGER = "service-manager"
   - ROLE_AUDITOR = "auditor"
   - ROLE_API_ACCESS = "api-access"
   - ROLE_INTERNAL_USER = "internal-user"
   - ROLE_EXTERNAL_USER = "external-user"
   - ROLE_CONFIG_ADMIN = "config-admin"
   - ROLE_OPS_ADMIN = "ops-admin"
   - ROLE_GOVERNANCE_ADMIN = "governance-admin"
   - ROLE_DEVELOPER = "developer"
   - ROLE_REPORT_VIEWER = "report-viewer"
   Client roles:
   - CLIENT_ROLE_READ = "read"
   - CLIENT_ROLE_WRITE = "write"
   - CLIENT_ROLE_ADMIN = "admin"
   - XROAD_CONSUMER = "xroad-consumer"
   - XROAD_PROVIDER = "xroad-provider"

2. Constants: com.iam.platform.common.constants.KafkaTopics
   - AUDIT_EVENTS = "iam.audit.events"
   - XROAD_EVENTS = "iam.xroad.events"
   - NOTIFICATION_COMMANDS = "iam.notification.commands"
   - PLATFORM_EVENTS = "iam.platform.events"
   - ALERT_TRIGGERS = "iam.alert.triggers"

3. Constants: com.iam.platform.common.constants.XRoadHeaders
   - CLIENT = "X-Road-Client"
   - ID = "X-Road-Id"
   - USER_ID = "X-Road-UserId"
   - REQUEST_HASH = "X-Road-Request-Hash"
   - SERVICE = "X-Road-Service"
   - REPRESENTED_PARTY = "X-Road-Represented-Party"

4. DTO: com.iam.platform.common.dto.ApiResponse<T>
   Fields: success (boolean), message (String), data (T), errorCode (String),
   errors (List<FieldError>), timestamp (Instant), requestId (String), path (String)
   Inner class: FieldError (field, message)
   Static factories: ok(data), ok(data, message), error(message), error(message, errorCode)

5. DTO: com.iam.platform.common.dto.UserProfileDto
   Fields: username, email, firstName, lastName, roles (List<String>),
   realmRoles (List<String>), clientRoles (Map<String, List<String>>),
   groups (List<String>), organization (String)

6. DTO: com.iam.platform.common.dto.XRoadContextDto
   Fields: clientInstance, clientMemberClass, clientMemberCode,
   clientSubsystem, fullClientId, messageId, userId, requestHash,
   serviceId, requestTimestamp (long)
   Methods: getMemberId(), isGovernmentRequest()

7. DTO: com.iam.platform.common.dto.AuditEventDto
   Fields: type (enum: AUTH_EVENT, API_ACCESS, XROAD_EXCHANGE, ADMIN_ACTION, CONFIG_CHANGE, GOVERNANCE_ACTION),
   timestamp (Instant), username, action, resource, sourceIp, success (boolean),
   metadata (Map<String, Object>), tenantId (String)

8. DTO: com.iam.platform.common.dto.NotificationCommandDto
   Fields: channelType (enum: EMAIL, SMS, TELEGRAM), recipient, subject, body,
   templateId, variables (Map<String, String>), priority (enum: LOW, NORMAL, HIGH, URGENT)

9. DTO: com.iam.platform.common.dto.PlatformEventDto
   Fields: eventType (enum: USER_CREATED, USER_UPDATED, USER_DELETED, LOGIN_SUCCESS,
   LOGIN_FAILED, ROLE_CHANGED, APP_REGISTERED, TENANT_CREATED, CONFIG_CHANGED),
   timestamp (Instant), payload (Map<String, Object>), tenantId, userId

10. DTO: com.iam.platform.common.dto.AlertTriggerDto
    Fields: alertRuleId (UUID), alertName, severity (enum: CRITICAL/HIGH/MEDIUM/LOW),
    condition (String), currentValue (String), threshold (String), serviceAffected (String),
    timestamp (Instant), metadata (Map<String, Object>)

11. Exception classes in com.iam.platform.common.exception:
    - IamPlatformException (base)
    - ResourceNotFoundException
    - AccessDeniedException
    - XRoadServiceException
    - TenantProvisioningException
    - ConfigurationException
    - IdentityVerificationException

12. Enums: com.iam.platform.common.enums
    - EntityType: GOVERNMENT_MINISTRY, GOVERNMENT_DEPARTMENT, STATE_ENTERPRISE, MUNICIPALITY,
      COMMUNE, PRIVATE_LLC, SINGLE_MEMBER_LLC, PUBLIC_LIMITED, BRANCH_OFFICE,
      REPRESENTATIVE_OFFICE, SOLE_PROPRIETOR, PARTNERSHIP, LOCAL_NGO, INTERNATIONAL_NGO,
      ASSOCIATION, FOREIGN_MISSION
    - MemberClass: GOV, COM, NGO, MUN
    - RepresentativeRole: LEGAL_REPRESENTATIVE, AUTHORIZED_SIGNATORY, TAX_REPRESENTATIVE,
      FINANCE_OFFICER, HR_MANAGER, IT_ADMINISTRATOR, COMPLIANCE_OFFICER, BRANCH_MANAGER,
      PROJECT_COORDINATOR, MUNICIPAL_OFFICER, GOVERNMENT_OFFICER, DELEGATED_USER, EXTERNAL_AUDITOR
    - DelegationScope: FULL_AUTHORITY, LIMITED, READ_ONLY, SPECIFIC_SERVICE
    - VerificationStatus: UNVERIFIED, PENDING, VERIFIED, REJECTED, EXPIRED, FAILED
    - IdentityVerificationLevel: LEVEL_0_UNVERIFIED, LEVEL_1_BASIC, LEVEL_2_DOCUMENT,
      LEVEL_3_EKYC, LEVEL_4_IN_PERSON
    - ExternalSystem: MOI_NATIONAL_ID, MOI_PERSONAL_ID_CODE, CAMDIGIKEY, MOC_BUSINESS_REGISTRATION,
      GDT_TIN, GDT_VAT, MLVT_REGISTRATION, NBC_LICENSE, CDC_INVESTMENT, XROAD_MEMBER,
      PASSPORT, FOREIGN_REGISTRATION
    - DocumentType: NATIONAL_ID_CARD, PASSPORT, BIRTH_CERTIFICATE, CERTIFICATE_OF_INCORPORATION,
      PATENT_TAX_CERTIFICATE, VAT_CERTIFICATE, NGO_REGISTRATION, GOVERNMENT_DECREE,
      BOARD_RESOLUTION, POWER_OF_ATTORNEY, APPOINTMENT_LETTER, DELEGATION_LETTER, MOU

13. Base entity: com.iam.platform.common.entity.BaseEntity
    Fields: id (UUID), createdAt (Instant), updatedAt (Instant),
    deleted (boolean, default false), deletedAt (Instant nullable)
    Use @MappedSuperclass, @PrePersist, @PreUpdate

Use Lombok @Data, @Builder, @NoArgsConstructor, @AllArgsConstructor.
Use Java records for immutable DTOs where appropriate.
```

**Commit:** `Phase 1: Maven multi-module project structure with 12 modules and shared common library`

---

## Phase 2: Docker Infrastructure

### Prompt 2.1 — Create Docker Compose for Development

```
Create the Docker development environment in the docker/ directory.

File: docker/docker-compose.yml

Services:

1. postgres (PostgreSQL 16-alpine)
   - Port: 5432
   - Create MULTIPLE databases on init (one per service):
     keycloak_db, iam_core, iam_tenant, iam_audit, iam_xroad, iam_admin,
     iam_monitoring, iam_governance, iam_developer, iam_notification, iam_config
   - Username: iam_admin, Password: iam_secret_2026
   - Health check with pg_isready
   - Create docker/init-scripts/create-multiple-databases.sh for multi-DB init

2. keycloak (quay.io/keycloak/keycloak:26.5.5)
   - Port: 8080
   - start-dev mode with --import-realm
   - PostgreSQL backend (keycloak_db)
   - Admin: admin / admin
   - Features: authorization,token-exchange,admin-fine-grained-authz
   - Mount keycloak/realms/ for realm auto-import
   - Depends on postgres (healthy)

3. redis (Redis 7-alpine)
   - Port: 6379, Password: redis_secret_2026
   - Max memory 256mb with allkeys-lru eviction

4. zookeeper (confluentinc/cp-zookeeper:7.6.0) — Port: 2181

5. kafka (confluentinc/cp-kafka:7.6.0)
   - Port: 9092 (external), 29092 (internal)
   - Auto-create topics enabled
   - Depends on zookeeper

6. elasticsearch (8.12.0)
   - Port: 9200, single node, security disabled (dev only), 512m heap

7. kibana (8.12.0) — Port: 5601, connected to elasticsearch

8. prometheus (v2.49.0)
   - Port: 9090
   - Mount docker/monitoring/prometheus.yml
   - Scrape configs for all services on ports 8080-8090, 8888

9. grafana (10.3.0) — Port: 3000, admin/admin

10. vault (hashicorp/vault:1.15)
    - Port: 8200, dev mode with root token: iam-dev-root-token

11. mailpit (axllent/mailpit:latest)
    - SMTP: 1025, Web UI: 8025

Also create:
- docker/docker-compose.xroad.yml (override file for X-Road):
  - xroad-central-server (niis/xroad-central-server:7.4.2) port 4000
  - xroad-ss-provider (niis/xroad-security-server:7.4.2) port 4001
  - xroad-ss-consumer (niis/xroad-security-server:7.4.2) port 4002

- docker/monitoring/prometheus.yml with scrape configs for all 12 services

All services on shared bridge network: iam-network.
Use named volumes for persistent data.
Health checks on all services.
Use Docker Compose profiles so services can be started selectively:
  - profile "core": postgres, keycloak, redis, kafka, elasticsearch
  - profile "full": all services
  - profile "xroad": adds X-Road containers

Also create environment-specific compose overrides:
- docker/docker-compose.dev.yml — DEV services (SPRING_PROFILES_ACTIVE=dev, localhost ports exposed)
- docker/docker-compose.uat.yml — UAT services (SPRING_PROFILES_ACTIVE=uat, env vars from .env.uat file)
- docker/docker-compose.prod.yml — PROD reference only (use K8s in production)
- docker/.env.dev — DEV environment variables (checked into git, dev-only secrets)
- docker/.env.uat.template — UAT env var template (NOT checked in, copy and fill)
- docker/.env.prod.template — PROD env var template (NOT checked in)

Content for .env.uat.template:
```
# RikReay IAM Platform — UAT Environment Variables
# Copy to .env.uat and fill in real values. Do NOT commit .env.uat to git.

SPRING_PROFILES_ACTIVE=uat
POSTGRES_HOST=postgres
POSTGRES_USER=iam_admin
POSTGRES_PASSWORD=CHANGE_ME_UAT_DB_PASSWORD
REDIS_HOST=redis
REDIS_PASSWORD=CHANGE_ME_UAT_REDIS_PASSWORD
KAFKA_SERVERS=kafka:29092
KEYCLOAK_URL=https://rikreay-uat.duckdns.org/auth
KEYCLOAK_ADMIN_USER=admin
KEYCLOAK_ADMIN_PASSWORD=CHANGE_ME_UAT_KC_PASSWORD
ELASTICSEARCH_URL=http://elasticsearch:9200
CORS_ORIGINS=https://rikreay-uat.duckdns.org
TELEGRAM_BOT_TOKEN=disabled
DUCKDNS_TOKEN=YOUR_DUCKDNS_TOKEN
```

Content for .env.prod.template is identical but with PROD values:
- KEYCLOAK_URL=https://rikreay.duckdns.org/auth
- CORS_ORIGINS=https://rikreay.duckdns.org
- Stronger passwords, Vault integration for secrets
```

**Commit:** `Phase 2: Docker Compose infrastructure with schema-per-service databases`

---

## Phase 3: Keycloak Realm Configuration

### Prompt 3.1 — Create Keycloak Realm JSON

```
Create the Keycloak realm configuration for auto-import.

File: keycloak/realms/iam-platform-realm.json

RBAC ONLY — NO ABAC, NO attribute-based policies, NO custom token mappers for department/clearance/org.

Realm settings:
- Realm name: iam-platform
- Display name: "RikReay IAM Enterprise Platform"
- SSL: external
- Registration: allowed
- Verify email: true, Login with email: true
- Brute force: enabled (5 failures, 10 min window, 30 min lockout)
- Access token lifespan: 600 seconds (10 min)
- SSO session idle: 1800 (30 min), max: 36000 (10 hours)
- Signature: RS256
- Password policy: length(8)+digits(1)+upperCase(1)+lowerCase(1)+specialChars(1)+notUsername+passwordHistory(3)
- SMTP (dev): host=mailpit, port=1025, from=noreply@iam-platform.local
- Internationalization: en + km, default: en

Realm Roles (13):
- iam-admin, tenant-admin, sector-admin, service-manager, auditor, api-access, internal-user, external-user
- config-admin, ops-admin, governance-admin, developer, report-viewer

Clients (11):
1. iam-gateway (confidential, service account enabled, secret: gateway-secret-dev-only)
   Client roles: gateway-admin
2. iam-core-service (confidential, service account enabled, secret: core-service-secret-dev-only)
   Client roles: read, write, admin
3. iam-xroad-adapter (confidential, service account enabled, secret: xroad-adapter-secret-dev-only)
   Client roles: xroad-consumer, xroad-provider
4. iam-tenant-service (confidential, service account enabled, secret: tenant-service-secret-dev-only)
5. iam-admin-service (confidential, service account enabled, secret: admin-service-secret-dev-only)
   Client roles: admin-read, admin-write
6. iam-monitoring-service (confidential, service account enabled, secret: monitoring-secret-dev-only)
   Client roles: monitor-read, monitor-write, alert-manage
7. iam-governance-service (confidential, service account enabled, secret: governance-secret-dev-only)
   Client roles: governance-read, governance-write, certification-manage
8. iam-developer-portal (confidential, service account enabled, secret: developer-portal-secret-dev-only)
   Client roles: portal-read, app-manage, webhook-manage
9. iam-notification-service (confidential, service account enabled, secret: notification-secret-dev-only)
   Client roles: notification-read, notification-write, template-manage
10. iam-config-service (confidential, service account enabled, secret: config-service-secret-dev-only)
    Client roles: config-read, config-write
11. iam-web-app (public client, PKCE S256)
    Redirect URIs: http://localhost:4200/*, http://localhost:3000/*, http://localhost:5173/*

Custom client scope: iam-roles-scope
  - realm roles mapper → claim "realm_access"
  - client roles mapper → claim "resource_access"
  - group membership mapper → claim "groups"

Groups:
- "Platform Administrators" → [iam-admin, api-access]
- "Config Administrators" → [config-admin, internal-user, api-access]
- "Operations Team" → [ops-admin, internal-user, api-access]
- "Governance Team" → [governance-admin, internal-user, api-access]
- "Internal Staff" → [internal-user, api-access]
  - Sub: "Tax Collection", "Tax Audit", "IT Department"
- "External Users" → [external-user, api-access]
  - Sub: "Citizens", "Partners"
- "Auditors" → [auditor, report-viewer, internal-user, api-access]
- "Sector Admins" → [sector-admin, internal-user, api-access]
- "Service Managers" → [service-manager, api-access]
- "Developers" → [developer, api-access]

Test Users (10):
1. admin-user / Admin@2026 → Platform Administrators
2. tax.officer / TaxOfficer@2026 → Internal Staff / Tax Collection
3. auditor.user / Auditor@2026 → Auditors
4. citizen.user / Citizen@2026 → External Users / Citizens
5. partner.user / Partner@2026 → External Users / Partners
6. config.admin / ConfigAdmin@2026 → Config Administrators
7. ops.admin / OpsAdmin@2026 → Operations Team
8. gov.admin / GovAdmin@2026 → Governance Team
9. dev.user / DevUser@2026 → Developers
10. report.viewer / ReportViewer@2026 → report-viewer + internal-user + api-access (assign roles directly, NOT via Auditors group — this user should NOT have auditor role)
11. sector.admin / SectorAdmin@2026 → Sector Admins (assign Keycloak user attribute: memberClass=GOV)

All users: email verified, enabled, not temporary
```

**Commit:** `Phase 3: Keycloak realm with 13 RBAC roles, 11 clients, 11 test users`

---

## Phase 4: Shared Security Configuration

### Prompt 4.1 — Create Keycloak JWT Converter and Security Auto-Config

```
In iam-common, create the shared security infrastructure.

1. File: com.iam.platform.common.security.KeycloakJwtAuthenticationConverter
   Implements Converter<Jwt, AbstractAuthenticationToken>
   
   Extraction logic (RBAC ONLY):
   a. Extract realm roles from jwt "realm_access" → "roles" (List<String>)
      Map each to: new SimpleGrantedAuthority("ROLE_" + roleName)
   b. Extract client roles from jwt "resource_access" → {clientId} → "roles"
      Map each to: new SimpleGrantedAuthority("ROLE_" + clientId + "_" + roleName)
   c. Use "preferred_username" as principal name (fallback to subject)
   
   IMPORTANT: Pure RBAC. Do NOT extract custom attributes as authorities.

2. File: com.iam.platform.common.security.IamSecurityAutoConfiguration
   @Configuration providing:
   - @Bean KeycloakJwtAuthenticationConverter
   - @Bean CorsConfigurationSource (reads app.cors.allowed-origins)
     Allows: GET, POST, PUT, DELETE, PATCH, OPTIONS
     Allows headers: Authorization, Content-Type, X-Road-Client, X-Road-Id, X-Road-UserId, X-Road-Request-Hash
   - @Bean GlobalExceptionHandler (@RestControllerAdvice)
     Handles: AccessDeniedException (403), ResourceNotFoundException (404),
     IamPlatformException (500), MethodArgumentNotValidException (400), generic Exception (500)
     Returns ApiResponse with proper HTTP status

3. Register via META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports

4. File: com.iam.platform.common.filter.XRoadRequestFilter
   OncePerRequestFilter that:
   - Only processes /xroad/** requests
   - Checks xroad.enabled property
   - Extracts and validates X-Road headers (Client and Id required)
   - Parses X-Road-Client format: INSTANCE/CLASS/MEMBER/SUBSYSTEM into XRoadContextDto
   - Stores in static ThreadLocal (ONLY in servlet-based services, NOT in reactive gateway)
   - Static methods: getContext(), isXRoadRequest()
   - Adds X-Road-Id to response for correlation
   - ALWAYS cleans ThreadLocal in finally block
   - Returns 400 for missing headers, 403 for unauthorized, 503 if disabled

Each service just needs a local SecurityFilterChain bean using the shared converter.
```

**Commit:** `Phase 4: Shared security auto-configuration with JWT converter and X-Road filter`


---

## Phase 5: Core Service Implementation

### Prompt 5.1 — Core Service Configuration and Security

```
Create iam-core-service (port 8082).

Package: com.iam.platform.core

application.yml following the standard template with:
- server.port: 8082
- spring.datasource.url: jdbc:postgresql://localhost:5432/iam_core
- spring.kafka topics: iam.audit.events, iam.xroad.events
- xroad member: { instance: KH, member-class: GOV, member-code: GDT, subsystem: TAX-SERVICES }
- Create application-dev.yml (ddl-auto: update, Flyway disabled, debug logging)
- Create application-uat.yml (ddl-auto: validate, Flyway enabled, info logging, env vars for secrets)
- Create application-prod.yml (ddl-auto: validate, Flyway enabled, SSL, env vars for all secrets)

SecurityConfig (@EnableMethodSecurity):
- /actuator/health, /actuator/info, /actuator/prometheus → permitAll
- /v3/api-docs/**, /swagger-ui/** → permitAll
- /xroad/** → permitAll (authenticated by X-Road Security Server, not JWT)
- /api/v1/persons/me, /api/v1/users/me → authenticated (any valid JWT)
- /api/v1/persons/** → hasAnyRole('internal-user', 'tenant-admin', 'iam-admin')
- /api/v1/entities/** → hasAnyRole('internal-user', 'tenant-admin', 'iam-admin')
- /api/v1/representations/** → hasAnyRole('tenant-admin', 'iam-admin')
- /api/v1/users/** → hasAnyRole('internal-user', 'tenant-admin', 'iam-admin')
- anyRequest → hasRole('api-access')
- Uses KeycloakJwtAuthenticationConverter from iam-common
- Register XRoadRequestFilter before security filter

RBAC only. All authorization uses hasRole() / hasAnyRole().

Create Flyway migration: V1__create_core_schema.sql

This migration MUST include all identity model tables:
- natural_persons (personalIdCode UNIQUE, nationalIdNumber, camDigiKeyId, firstNameKh, lastNameKh,
  firstNameEn, lastNameEn, dateOfBirth, gender, nationality, identityVerificationStatus,
  identityVerificationMethod, identityVerifiedAt, keycloakUserId, status, + BaseEntity fields)
- legal_entities (registrationNumber UNIQUE, taxIdentificationNumber UNIQUE, nameKh, nameEn,
  entityType, memberClass, xroadMemberCode, xroadSubsystem, sectorCode, incorporationDate,
  registeredAddress, province, realmName UNIQUE, parentEntityId FK self-ref, status, + BaseEntity fields)
- representations (naturalPersonId FK, legalEntityId FK, representativeRole, delegationScope,
  specificPermissions JSONB, title, validFrom, validUntil, authorizedByPersonId, authorizationDocument,
  authorizationDocumentType, verificationStatus, isPrimary, status, + BaseEntity fields)
- external_identity_links (ownerType, ownerId, externalSystem, externalIdentifier,
  verificationStatus, verifiedAt, verificationMethod, metadata JSONB, + BaseEntity fields)
- addresses (ownerType, ownerId, addressType, streetAddress, sangkat, khan, province,
  postalCode, country DEFAULT 'KH', isPrimary, + BaseEntity fields)
- contact_channels (ownerType, ownerId, channelType, value, isPrimary, isVerified,
  verifiedAt, notificationEnabled, + BaseEntity fields)
- identity_documents (ownerType, ownerId, documentType, documentNumber, issuedBy,
  issuedDate, expiryDate, fileStoragePath, fileHash, verificationStatus, + BaseEntity fields)

Entity Type enum values (16): GOVERNMENT_MINISTRY, GOVERNMENT_DEPARTMENT, STATE_ENTERPRISE,
MUNICIPALITY, COMMUNE, PRIVATE_LLC, SINGLE_MEMBER_LLC, PUBLIC_LIMITED, BRANCH_OFFICE,
REPRESENTATIVE_OFFICE, SOLE_PROPRIETOR, PARTNERSHIP, LOCAL_NGO, INTERNATIONAL_NGO,
ASSOCIATION, FOREIGN_MISSION

Representative Role enum values (13): LEGAL_REPRESENTATIVE, AUTHORIZED_SIGNATORY,
TAX_REPRESENTATIVE, FINANCE_OFFICER, HR_MANAGER, IT_ADMINISTRATOR, COMPLIANCE_OFFICER,
BRANCH_MANAGER, PROJECT_COORDINATOR, MUNICIPAL_OFFICER, GOVERNMENT_OFFICER,
DELEGATED_USER, EXTERNAL_AUDITOR

Member Class enum values (4): GOV, COM, NGO, MUN

Create indexes: naturalPersonId + legalEntityId on representations,
ownerType + ownerId on external_identity_links/addresses/contact_channels/identity_documents,
externalSystem + externalIdentifier UNIQUE on external_identity_links

Note: Create the Flyway migration file even though Flyway is disabled in dev profile.
The migration file is needed for UAT and PROD where ddl-auto is 'validate' and Flyway
manages schema. In dev, ddl-auto: update handles schema creation automatically.
Apply this same pattern to ALL service phases that create Flyway migrations.

Note: The 'optional:' prefix in spring.config.import means the service starts normally
even if Config Service is not running. During Phases 5-11, Config Service doesn't exist
yet — services use their local application-dev.yml directly. After Phase 12, services
pull additional config from Config Service when available.
```

### Prompt 5.2 — Core Service Controllers and Business Logic

```
Create REST controllers and services for iam-core-service.

JPA Entities (com.iam.platform.core.entity, all extend BaseEntity with @SQLRestriction):
1. NaturalPerson — human identity with personalIdCode, nationalIdNumber, camDigiKeyId,
   names (Khmer + Latin), dateOfBirth, gender, nationality, identityVerificationStatus,
   keycloakUserId, status (ACTIVE/SUSPENDED/DECEASED/BLOCKED)
2. LegalEntity — organization with registrationNumber, TIN, entityType (16 types),
   memberClass (GOV/COM/NGO/MUN), xroadMemberCode, realmName (→ Keycloak realm),
   parentEntityId (self-ref for branches), status (ACTIVE/SUSPENDED/DISSOLVED)
3. Representation — links NaturalPerson to LegalEntity with representativeRole (13 roles),
   delegationScope, validFrom/validUntil, authorizationDocument, verificationStatus, isPrimary
4. ExternalIdentityLink — maps platform identity to external systems (MoI, CamDigiKey, MoC,
   GDT, MLVT, NBC, etc.) with verificationStatus and metadata JSONB
5. Address — Cambodia administrative divisions (sangkat, khan, province), ownerType polymorphic
6. ContactChannel — EMAIL/PHONE/TELEGRAM/SMS with verification status
7. IdentityDocument — 13 document types with file storage path and SHA-256 hash

Controllers (all use ApiResponse<T> wrapper, OpenAPI annotations, @PreAuthorize RBAC):

1. NaturalPersonController (com.iam.platform.core.controller):
   - POST /api/v1/persons → hasAnyRole('iam-admin', 'tenant-admin') — register natural person
   - GET /api/v1/persons → hasAnyRole('internal-user', 'tenant-admin', 'iam-admin') — paginated list
   - GET /api/v1/persons/{id} → hasAnyRole('iam-admin', 'tenant-admin')
   - PUT /api/v1/persons/{id} → hasAnyRole('iam-admin', 'tenant-admin')
   - GET /api/v1/persons/me → authenticated — get own profile from JWT
   - PUT /api/v1/persons/me → authenticated — update own profile
   - POST /api/v1/persons/{id}/verify → hasRole('iam-admin') — trigger identity verification
   - GET /api/v1/persons/{id}/representations → authenticated — list entities this person represents

2. LegalEntityController:
   - POST /api/v1/entities → hasRole('iam-admin') — register legal entity (calls tenant-service to provision realm)
   - GET /api/v1/entities → hasAnyRole('internal-user', 'tenant-admin', 'iam-admin') — paginated list
   - GET /api/v1/entities/{id} → hasAnyRole('iam-admin', 'tenant-admin')
   - PUT /api/v1/entities/{id} → hasAnyRole('iam-admin', 'tenant-admin')
   - GET /api/v1/entities/{id}/representatives → hasAnyRole('tenant-admin', 'iam-admin') — list representatives

3. RepresentationController:
   - POST /api/v1/representations → hasAnyRole('tenant-admin', 'iam-admin') — create delegation
   - GET /api/v1/representations → hasAnyRole('tenant-admin', 'iam-admin') — list for an entity
   - PUT /api/v1/representations/{id} → hasAnyRole('tenant-admin', 'iam-admin') — update delegation
   - DELETE /api/v1/representations/{id} → hasAnyRole('tenant-admin', 'iam-admin') — revoke (soft delete)
   - POST /api/v1/representations/{id}/verify → hasRole('iam-admin') — verify authorization document

4. UserController (retained for backward compatibility):
   - GET /api/v1/users/me → authenticated (lightweight JWT-derived profile)
   - GET /api/v1/users → hasAnyRole('internal-user', 'tenant-admin', 'iam-admin')

5. XRoadServiceController:
   - GET /xroad/v1/taxpayer/{tin} → X-Road authenticated (no JWT, permitAll with XRoadRequestFilter)
   - GET /xroad/v1/declaration/{declarationId} → X-Road authenticated, GOV class only
   - GET /xroad/v1/taxpayer/{tin}/status → X-Road authenticated, all consumers
   - GET /xroad/v1/person/{personalIdCode}/verify → X-Road authenticated — verify natural person identity
   - GET /xroad/v1/entity/{registrationNumber}/verify → X-Road authenticated — verify legal entity
   - GET /xroad/v1/health → X-Road health check

Services:
1. NaturalPersonService — CRUD for NaturalPerson, identity verification workflow,
   link to Keycloak user, search by personalIdCode/nationalIdNumber/camDigiKeyId
2. LegalEntityService — CRUD for LegalEntity, search by registrationNumber/TIN.
   On create: calls iam-tenant-service REST API (POST /api/v1/tenants) to provision Keycloak realm.
   Do NOT use keycloak-admin-client directly — realm provisioning is tenant-service's responsibility.
   Uses WebClient with Resilience4j circuit breaker for the inter-service call.
3. RepresentationService — CRUD for Representation, validates delegation rules
   (must have at least one LEGAL_REPRESENTATIVE per entity), handles expiry
4. ExternalIdentityLinkService — CRUD, verification status management, integration
   with external systems (MoI, MoC, GDT) for identity verification
5. IdentityVerificationService — orchestrates verification workflow:
   verifyNaturalPerson(id, method), verifyLegalEntity(id, method),
   verifyRepresentation(id), publishes VerificationEvent to Kafka
6. AuditService — publishes AuditEventDto to Kafka topic "iam.audit.events"
   Methods: logApiAccess(), logXRoadAccess(), logAuthEvent(), logIdentityVerification()
   Audit failures NEVER block main request flow (try-catch, log error)

7. KafkaConfig — topic auto-creation beans for iam.audit.events and iam.xroad.events
   (3 partitions, 1 replica for dev)

All X-Road controller methods call AuditService to log access.
```

**Commit:** `Phase 5: Core service with user profiles, X-Road endpoints, and audit publishing`

---

## Phase 6: API Gateway

### Prompt 6.1 — Create API Gateway Service

```
Create iam-gateway (port 8081) as a Spring Cloud Gateway (reactive).

Dependencies: spring-cloud-starter-gateway, spring-boot-starter-oauth2-resource-server,
spring-boot-starter-data-redis-reactive, spring-cloud-starter-circuitbreaker-reactor-resilience4j,
springdoc-openapi-starter-webflux-ui (NOT webmvc-ui — gateway is reactive)

IMPORTANT: This is a REACTIVE application (WebFlux). Do NOT use servlet-based classes.
Do NOT use ThreadLocal. Do NOT import XRoadRequestFilter here.

application.yml (port 8081):

Create application-dev.yml, application-uat.yml, application-prod.yml following the standard pattern.
Note: Gateway has no database, so omit datasource/Flyway config. Include: Keycloak issuer-uri,
Redis config, rate limiter config, CORS origins. UAT/PROD use env vars for all URLs.

Gateway routes for ALL 12 services:
1. core-service: /api/v1/users/**, /api/v1/persons/**, /api/v1/entities/**, /api/v1/representations/**, /xroad/** → http://localhost:8082
2. tenant-service: /api/v1/tenants/** → http://localhost:8083
3. audit-service: /api/v1/audit/** → http://localhost:8084
4. xroad-adapter: /api/v1/xroad/** → http://localhost:8085
5. admin-service: /api/v1/platform-admin/** → http://localhost:8086
6. monitoring-service: /api/v1/monitoring/** → http://localhost:8087
7. governance-service: /api/v1/governance/** → http://localhost:8088
8. developer-portal: /api/v1/apps/**, /api/v1/docs/**, /api/v1/webhooks/**, /api/v1/sandbox/**, /api/v1/sdks/** → http://localhost:8089
9. notification-service: /api/v1/notifications/** → http://localhost:8090
10. config-service: /api/v1/config/** → http://localhost:8888
11. keycloak: /auth/** → http://localhost:8080

Each route: TokenRelay filter, CircuitBreaker with fallback, RemoveRequestHeader=Cookie

Global filters:
- RequestRateLimiter (Redis-backed, 100 req/sec default)
- AddRequestHeader: X-Request-Id (UUID)

Security (reactive SecurityWebFilterChain):
- JWT validation using Keycloak issuer-uri
- Permit: /actuator/**, /auth/**, /api/v1/docs/**
- All other: authenticated

NOTE: Admin service uses /api/v1/platform-admin/** (NOT /api/v1/admin/**) to avoid
collision with core-service's admin endpoints. This is intentional.

IMPORTANT: X-Road headers (X-Road-Client, X-Road-Id, X-Road-UserId, X-Road-Request-Hash)
must be preserved through the gateway for /xroad/** routes. Do NOT strip these headers.
```

**Commit:** `Phase 6: API Gateway with routes for all 12 services`

---

## Phase 7: Tenant Management Service

### Prompt 7.1 — Create Tenant Service

```
Create iam-tenant-service (port 8083).

Package: com.iam.platform.tenant

application.yml: port 8083, database: iam_tenant
Uses Keycloak Admin Client (keycloak-admin-client dependency)

Create application-dev.yml, application-uat.yml, application-prod.yml following the standard pattern
(see Architecture Standards section). DEV: ddl-auto update, Flyway disabled. UAT/PROD: ddl-auto validate, Flyway enabled.

JPA Entity: Tenant (@Entity, extends BaseEntity for soft delete)
- name (unique), realmName (unique), description
- memberClass (GOV/COM/NGO/MUN), memberCode, xroadSubsystem
- status (enum: ACTIVE, SUSPENDED, PROVISIONING, DECOMMISSIONED)

Flyway: V1__create_tenant_schema.sql

Service: TenantProvisioningService
- createTenant(CreateTenantRequest) → creates Keycloak realm + DB record
  Uses Keycloak Admin API to: create realm, set settings, create default RBAC roles,
  create default clients, configure email
- getTenant(realmName), listTenants(), suspendTenant(), deleteTenant() (soft delete)

Controller: TenantController
- POST /api/v1/tenants → hasRole('iam-admin')
- GET /api/v1/tenants → hasAnyRole('iam-admin', 'tenant-admin')
- GET /api/v1/tenants/{realmName} → hasAnyRole('iam-admin', 'tenant-admin')
- PUT /api/v1/tenants/{realmName}/suspend → hasRole('iam-admin')
- DELETE /api/v1/tenants/{realmName} → hasRole('iam-admin')

Publishes PlatformEventDto (TENANT_CREATED) to iam.platform.events Kafka topic.
Publishes AuditEventDto to iam.audit.events for all tenant lifecycle operations.
SecurityConfig: RBAC pattern using shared converter.
```

**Commit:** `Phase 7: Tenant management service with Keycloak realm provisioning`

---

## Phase 8: Audit Service

### Prompt 8.1 — Create Audit Consumer Service

```
Create iam-audit-service (port 8084).

Package: com.iam.platform.audit

Dependencies: spring-boot-starter-web, spring-boot-starter-data-jpa,
spring-boot-starter-data-elasticsearch, spring-kafka, postgresql, flyway-core, flyway-database-postgresql

application.yml: port 8084, database: iam_audit (for metadata only)
Elasticsearch config: localhost:9200
Kafka consumer group: iam-audit-consumer

Create application-dev.yml, application-uat.yml, application-prod.yml following the standard pattern.

Flyway: V1__create_audit_metadata_schema.sql (for audit metadata tables if any — primary storage is Elasticsearch)

Kafka consumers:
1. Listen to "iam.audit.events" topic
2. Listen to "iam.xroad.events" topic

Elasticsearch indexes (monthly rolling):
- iam-audit-{yyyy.MM}
- iam-xroad-{yyyy.MM}

Service: AuditIndexingService
- @KafkaListener for iam.audit.events → deserialize AuditEventDto → index to ES
- @KafkaListener for iam.xroad.events → index to ES

Controller: AuditQueryController
- GET /api/v1/audit/events → hasAnyRole('auditor', 'iam-admin', 'report-viewer')
  Paginated, filter by: type, username, dateFrom, dateTo, tenantId (optional), memberClass (optional)
  Note: tenantId filter enables org-admin scoped queries (called via iam-admin-service proxy)
  Note: memberClass filter enables sector-admin scoped queries
- GET /api/v1/audit/xroad → hasAnyRole('auditor', 'iam-admin', 'service-manager')
  Paginated X-Road events, filter by: tenantId, memberClass
- GET /api/v1/audit/stats → hasAnyRole('auditor', 'iam-admin')
  Aggregated statistics, optional tenantId/memberClass filter
- GET /api/v1/audit/events/export → hasAnyRole('auditor', 'iam-admin')
  Export as CSV/JSON, optional tenantId filter
- GET /api/v1/audit/login-history → hasAnyRole('auditor', 'iam-admin')
  Dedicated login/logout/failed-login history endpoint
  Filter by: username, tenantId, dateFrom, dateTo, status (SUCCESS/FAILED)

SecurityConfig: RBAC pattern.
```

**Commit:** `Phase 8: Audit service with Kafka consumer and Elasticsearch indexing`

---

## Phase 9: X-Road Adapter Service

### Prompt 9.1 — Create X-Road Adapter Service

```
Create iam-xroad-adapter (port 8085).

Package: com.iam.platform.xroad

application.yml: port 8085, database: iam_xroad
Redis for ACL caching (5 min TTL)

Entities:
- XRoadServiceRegistration (id, serviceCode, serviceVersion, targetService, targetPath, description,
  enabled, extends BaseEntity)
- XRoadAclEntry (id, consumerIdentifier, serviceRegistrationId, allowed, description, extends BaseEntity)

Flyway: V1__create_xroad_schema.sql

Create application-dev.yml, application-uat.yml, application-prod.yml following the standard pattern.

X-Road configuration:
  xroad:
    member: { instance: KH, member-class: GOV, member-code: GDT, subsystem: TAX-SERVICES }
    services:
      - code: getTaxpayerInfo, version: v1, target: iam-core-service, path: /xroad/v1/taxpayer/{tin}
      - code: getTaxDeclaration, version: v1, target: iam-core-service, path: /xroad/v1/declaration/{declarationId}
      - code: verifyTaxStatus, version: v1, target: iam-core-service, path: /xroad/v1/taxpayer/{tin}/status
    acl:
      - consumer: KH/GOV/MOF/BUDGET-SYSTEM → all services
      - consumer: KH/GOV/MEF/FISCAL-MGMT → getTaxpayerInfo, getTaxDeclaration
      - consumer: KH/COM/PARTNER001/TAX-PORTAL → verifyTaxStatus

Services:
1. XRoadRoutingService — routes incoming X-Road requests to internal services,
   validates ACL, adds headers, caches ACL in Redis
2. XRoadRegistryService — CRUD for service registrations and ACL entries

Controllers:
1. XRoadProxyController — catch-all for X-Road Security Server requests
   Extracts headers → validates ACL → proxies to target using WebClient → returns with correlation headers
2. XRoadAdminController (RBAC secured):
   - GET/POST /api/v1/xroad/services → hasAnyRole('service-manager', 'iam-admin')
   - GET/POST/DELETE /api/v1/xroad/acl → hasAnyRole('service-manager', 'iam-admin')
   - GET /api/v1/xroad/members → hasAnyRole('service-manager', 'iam-admin')

SecurityConfig: /xroad/** permitAll (SS auth), /api/v1/xroad/** RBAC secured.
Publishes audit events to iam.xroad.events.
```

**Commit:** `Phase 9: X-Road adapter with routing, ACL, and proxy`

---

## Phase 10: Testing & Documentation

### Prompt 10.1 — Create Integration Tests for Core Platform

```
Create integration tests for the 6 core modules.

For each service:
1. *ApplicationTests.java — context loads
2. *SecurityTests.java — RBAC endpoint tests:
   - Unauthenticated → 401
   - Wrong role → 403
   - Correct role → 200/201
   Use @WithMockUser and SecurityMockMvcRequestPostProcessors.jwt()

Specific tests:
- KeycloakJwtAuthenticationConverterTest: realm role extraction, client role extraction,
  empty claims, preferred_username as principal
- XRoadRequestFilterTest: valid headers, missing headers (400), ThreadLocal population and cleanup
- CoreSecurityConfigTest: health permitAll, admin requires iam-admin, xroad permitAll
- AuditQueryControllerTest: auditor role access, iam-admin access, unauthorized 403
- TenantControllerTest: CRUD with iam-admin, tenant-admin limited access
- XRoadAdminControllerTest: service-manager access

Create test utility: JwtTestUtils — builds mock JWTs with Keycloak role structure.
Use JUnit 5, MockMvc, @MockBean.
```

### Prompt 10.2 — Create OpenAPI Documentation and README

```
Add OpenAPI/Swagger documentation to all controllers.

Each service application.yml:
  springdoc:
    api-docs.path: /v3/api-docs
    swagger-ui.path: /swagger-ui.html

Annotate all controllers with:
- @Tag(name, description)
- @Operation(summary, description, security = @SecurityRequirement(name = "bearer-jwt"))
- @ApiResponse for 200, 400, 401, 403, 404, 500

Create shared OpenApiConfig in iam-common with SecurityScheme for bearer JWT.

Create comprehensive README.md at project root:
1. Architecture overview with 5 domains
2. Technology stack table
3. Quick Start guide (docker compose up, mvn build, start services)
4. Service port map (all 12 services + infrastructure)
5. Test user credentials (11 users)
6. RBAC role descriptions (13 roles)
7. X-Road service identifiers
8. API endpoint summary by service
9. Development workflow
```

**Commit:** `Phase 10: Integration tests, OpenAPI docs, and README`

---

## Phase 11: Production Preparation (Core)

### Prompt 11.1 — Dockerfiles and Kubernetes Manifests

```
Create production Dockerfiles for the 6 core services (gateway, core, tenant, audit, xroad-adapter, config-service).

Multi-stage Dockerfile pattern:
Stage 1: eclipse-temurin:21-jdk-alpine, Maven build
Stage 2: eclipse-temurin:21-jre-alpine
- addgroup/adduser for non-root execution
- HEALTHCHECK on /actuator/health
- ENTRYPOINT with container-aware JVM flags

Create docker/docker-compose.services.yml to build and run all services.

Create Kubernetes manifests in k8s/:
- Namespace: iam-platform
- Per service: Deployment (2 replicas), Service (ClusterIP), HPA (min 2, max 5, CPU 70%), ConfigMap
- Keycloak: StatefulSet (2 replicas for HA), headless Service, PVC
- Gateway: Deployment + Ingress with TLS
- kustomization.yaml for environment overlays (dev, uat, prod)
  See "Kubernetes Kustomize Overlays" section in Architecture Standards
  - dev: 1 replica, no resource limits, debug logging
  - uat: 2 replicas, moderate limits, info logging
  - prod: 3+ replicas, strict limits, TLS, network policies
```

**Commit:** `Phase 11: Production Dockerfiles and Kubernetes manifests`


---

## Phase 12: Config Service Implementation (BUILD FIRST — All Other New Services Depend on This)

### Prompt 12.1 — Config Service Foundation

```
Implement iam-config-service (port 8888) — Spring Cloud Config Server.
This MUST be built first because all other services pull config from it.

Package: com.iam.platform.config

Dependencies: spring-cloud-config-server, spring-boot-starter-web,
spring-boot-starter-security, spring-boot-starter-data-jpa, postgresql, flyway

ConfigServiceApplication.java — annotated with @EnableConfigServer

CRITICAL SECURITY SPLIT — two layers:
1. Native Spring Cloud Config endpoints (/{application}/{profile}) → permitAll
   These are for internal service-to-service config fetching.
   Services need config BEFORE they can validate JWT tokens (chicken-and-egg).
   Protect via network-level security (internal Docker network only) in production.
2. Management API (/api/v1/config/**) → hasAnyRole('config-admin', 'iam-admin')
   Human-facing admin endpoints protected by JWT RBAC.

application.yml:
- server.port: 8888
- spring.datasource.url: jdbc:postgresql://localhost:5432/iam_config
- spring.cloud.config.server.native.search-locations: classpath:/config-repo
- spring.profiles.active: native (filesystem-based for dev)

Create application-dev.yml, application-uat.yml, application-prod.yml following the standard pattern.
Note: Config Service is special — its base profile uses 'native' for config source type.
DEV: spring.profiles.active: native,dev. UAT/PROD: spring.profiles.active: native,uat (or prod).

Entities:
- FeatureFlag (id, key, value, description, enabled, environment, createdAt, updatedAt)
- ConfigChangeLog (id, version, application, profile, changesJson, author, timestamp)

Flyway: V1__create_config_schema.sql

Services:
1. FeatureFlagService — CRUD for feature flags with environment filtering
2. ConfigVersionService — history and rollback capability

Controllers:
1. FeatureFlagController:
   - GET/POST /api/v1/config/flags → hasAnyRole('config-admin', 'iam-admin')
   - PUT /api/v1/config/flags/{key} → toggle flag
2. ConfigManagementController:
   - GET /api/v1/config/{application}/{profile} → get config
   - PUT /api/v1/config/{application}/{profile} → update config
   - GET /api/v1/config/history → change log
   - POST /api/v1/config/rollback/{version} → rollback

Publish AuditEventDto (CONFIG_CHANGE) to iam.audit.events for all changes.
```

**Commit:** `Phase 12: Config service — Spring Cloud Config Server with feature flags`

---

## Phase 13: Notification Service Implementation (Cross-Cutting — Needed by Later Services)

### Prompt 13.1 — Notification Service

```
Implement iam-notification-service (port 8090).
Build this BEFORE admin/monitoring/governance because they all send notifications.

Package: com.iam.platform.notification

Dependencies: spring-boot-starter-web, spring-boot-starter-data-jpa, spring-boot-starter-mail,
spring-boot-starter-thymeleaf, spring-kafka, postgresql, flyway-core, flyway-database-postgresql

application.yml: port 8090, database: iam_notification
- spring.mail.host: mailpit (dev) / SMTP env var (prod)
- spring.kafka.consumer.group-id: notification-group
- telegram.bot.token: ${TELEGRAM_BOT_TOKEN:disabled}

Create application-dev.yml, application-uat.yml, application-prod.yml following the standard pattern.

Entities:
- NotificationChannel (id, type [EMAIL/SMS/TELEGRAM], configJson, enabled)
- NotificationTemplate (id, name, subject, bodyTemplate, channelType, variableNames)
- NotificationLog (id, templateId, channelType, recipient, status [SENT/FAILED/PENDING], sentAt, errorMessage)
  — This uses HARD delete (not soft delete) for log rotation
- ScheduledReport (id, name, cronExpression, templateId, recipientList, enabled)

Flyway: V1__create_notification_schema.sql

Services:
1. EmailService — JavaMailSender, supports HTML with Thymeleaf templates
2. SmsService — placeholder for SMS gateway integration
3. TelegramService — Telegram Bot API integration
4. NotificationDispatcher — routes to correct channel, logs all attempts
5. TemplateService — CRUD + renderTemplate(templateId, variables)
6. ScheduleService — CRUD + @Scheduled / Quartz for cron execution

Kafka consumers:
- NotificationKafkaConsumer: listens to "iam.notification.commands" and "iam.alert.triggers"
  Dispatches via NotificationDispatcher based on event type

Controllers (RBAC secured):
- NotificationController: GET list, POST send ad-hoc → hasAnyRole('iam-admin', 'ops-admin')
- ChannelController: CRUD channels → hasRole('iam-admin')
- TemplateController: CRUD templates → hasRole('iam-admin')
- ScheduleController: CRUD schedules → hasAnyRole('iam-admin', 'ops-admin')

SecurityConfig: RBAC pattern.
```

**Commit:** `Phase 13: Notification service with email, SMS, Telegram, and Kafka consumer`

---

## Phase 14: Admin Service Implementation

### Prompt 14.1 — Admin Service Foundation

```
Implement iam-admin-service (port 8086).

Package: com.iam.platform.admin

IMPORTANT: This service uses /api/v1/platform-admin/** (NOT /api/v1/admin/**)
to avoid route collision with iam-core-service's existing admin endpoints.

application.yml: port 8086, database: iam_admin
- spring.config.import: optional:configserver:http://localhost:8888
- keycloak.admin.* properties for Keycloak Admin API

Create application-dev.yml, application-uat.yml, application-prod.yml following the standard pattern.

Dependencies: keycloak-admin-client, spring-cloud-starter-circuitbreaker-resilience4j,
spring-boot-starter-webflux (for WebClient to call audit-service, monitoring-service, governance-service)

SecurityConfig (@EnableMethodSecurity): standard RBAC pattern.
- /actuator/** → permitAll
- /api/v1/platform-admin/platform/** → hasRole('iam-admin')
- /api/v1/platform-admin/sector/** → hasRole('sector-admin')
- /api/v1/platform-admin/org/** → hasRole('tenant-admin')
- /api/v1/platform-admin/users/** → hasAnyRole('iam-admin', 'tenant-admin')
- /api/v1/platform-admin/settings → hasAnyRole('iam-admin', 'config-admin')
- /api/v1/platform-admin/sector-admins → hasRole('iam-admin')

Entities:
- PlatformSettings (id, key, value, category, description, updatedBy, extends BaseEntity)
- UsageRecord (id, tenantId, legalEntityId FK, recordDate LocalDate, metricType enum
  [API_CALLS/LOGINS/FAILED_LOGINS/XROAD_TRANSACTIONS/ACTIVE_USERS/STORAGE_MB],
  count long, metadata JSONB, extends BaseEntity)
- SectorAdminAssignment (id, naturalPersonId FK, memberClass enum [GOV/COM/NGO/MUN],
  assignedByUserId, validFrom, validUntil, status [ACTIVE/REVOKED], extends BaseEntity)

Flyway: V1__create_admin_schema.sql (includes all 3 tables)

Services:
1. AdminDashboardService — platform-wide stats from Keycloak Admin API
   (total user count, total tenant count, total active sessions, orgs by sector) with circuit breaker
2. OrgDashboardService (NEW) — per-tenant dashboard stats:
   userCount, activeSessionCount, representativeCount, pendingVerificationsCount,
   apiCallsThisMonth, xroadTransactionsThisMonth, recentEventsCount
   Queries: Keycloak Admin API (scoped to tenant realm), iam-audit-service, UsageRecord table
3. SectorDashboardService (NEW) — per-sector aggregated stats:
   orgCount, totalUsers, activeOrgs, suspendedOrgs, aggregateApiCalls, aggregateXroadTransactions
   Filters by memberClass from SectorAdminAssignment
4. RealmSettingsService (NEW) — wraps Keycloak Admin API to let tenant-admin configure:
   passwordPolicy, requiredMfa, sessionIdleTimeout, sessionMaxLifespan,
   allowedRedirectUris, loginTheme, bruteForceProtection settings
   ONLY for the tenant-admin's own realm (validated against JWT tenantId claim)
5. BulkUserService — batch create/export/disable users via Keycloak Admin API
6. AdminSettingsService — CRUD for platform-wide settings in PostgreSQL
7. UsageTrackingService (NEW) — scheduled job (daily) that queries Elasticsearch audit events
   and Prometheus metrics, aggregates by tenant per day, stores UsageRecord rows
8. OrgAuditProxyService (NEW) — proxies audit queries to iam-audit-service with tenantId filter
9. OrgNotificationConfigService (NEW) — per-tenant notification preferences:
   which event types trigger notifications, which channels, which recipients

Controllers:

--- Tier 0/1: Platform Admin Endpoints ---
1. PlatformDashboardController:
   - GET /api/v1/platform-admin/platform/dashboard → hasRole('iam-admin')
     Returns: totalOrgs, totalUsers, orgsBySector, usersBySector, healthStatus, totalApiCalls
   - GET /api/v1/platform-admin/platform/usage → hasRole('iam-admin')
   - GET /api/v1/platform-admin/platform/usage/by-tenant → hasRole('iam-admin')
   - GET /api/v1/platform-admin/platform/usage/export → hasRole('iam-admin')

2. SectorAdminController:
   - CRUD /api/v1/platform-admin/sector-admins → hasRole('iam-admin')
     Manage sector admin assignments (assign a person as sector-admin for GOV/COM/NGO/MUN)

3. BulkUserController:
   - POST /api/v1/platform-admin/users/bulk-import → hasRole('iam-admin')
   - GET /api/v1/platform-admin/users/bulk-export → hasRole('iam-admin')
   - POST /api/v1/platform-admin/users/bulk-disable → hasRole('iam-admin')

4. AdminSettingsController:
   - GET /api/v1/platform-admin/settings → hasAnyRole('iam-admin', 'config-admin')
   - PUT /api/v1/platform-admin/settings → hasAnyRole('iam-admin', 'config-admin')

5. AdminUserController:
   - GET /api/v1/platform-admin/users → hasAnyRole('iam-admin', 'tenant-admin')
     Unified user list from Keycloak across realms (iam-admin) or within realm (tenant-admin)

--- Tier 2: Sector Admin Endpoints ---
6. SectorDashboardController:
   - GET /api/v1/platform-admin/sector/dashboard → hasRole('sector-admin')
     Auto-filters by memberClass from JWT/SectorAdminAssignment
   - GET /api/v1/platform-admin/sector/organizations → hasRole('sector-admin')
     Paginated list of LegalEntities in their sector
   - GET /api/v1/platform-admin/sector/audit → hasRole('sector-admin')
     Audit events filtered by memberClass
   - GET /api/v1/platform-admin/sector/compliance → hasRole('sector-admin')
     Aggregated compliance status across orgs in sector
   - GET /api/v1/platform-admin/sector/reports → hasRole('sector-admin')
   - GET /api/v1/platform-admin/sector/reports/export → hasRole('sector-admin')
   - POST /api/v1/platform-admin/sector/organizations/{id}/approve → hasRole('sector-admin')
   - POST /api/v1/platform-admin/sector/organizations/{id}/reject → hasRole('sector-admin')

--- Tier 3: Organization Admin Endpoints ---
7. OrgDashboardController:
   - GET /api/v1/platform-admin/org/dashboard → hasRole('tenant-admin')
     Returns: userCount, activeSessionCount, representativeCount, pendingVerifications,
     apiCallsThisMonth, xroadTransactions, recentEventsCount, mfaAdoptionRate

8. OrgSettingsController:
   - GET /api/v1/platform-admin/org/settings → hasRole('tenant-admin')
   - PUT /api/v1/platform-admin/org/settings → hasRole('tenant-admin')
     Manages: passwordPolicy, mfaRequired, sessionIdleTimeout, sessionMaxLifespan,
     loginTheme, allowedRedirectUris, bruteForceProtection

9. OrgAuditController:
   - GET /api/v1/platform-admin/org/audit → hasRole('tenant-admin')
     Params: type, dateFrom, dateTo, username — all filtered to their tenantId
   - GET /api/v1/platform-admin/org/audit/export → hasRole('tenant-admin')
   - GET /api/v1/platform-admin/org/login-history → hasRole('tenant-admin')
     Params: username, dateFrom, dateTo, status (SUCCESS/FAILED)

10. OrgUsageController:
    - GET /api/v1/platform-admin/org/usage → hasRole('tenant-admin')
      Returns: apiCallsByDay, loginsByDay, xroadTransactionsByDay, activeUsersThisMonth

11. OrgNotificationController:
    - GET /api/v1/platform-admin/org/notifications → hasRole('tenant-admin')
    - PUT /api/v1/platform-admin/org/notifications → hasRole('tenant-admin')
      Manages: event types to notify, channels, recipients for their org

12. OrgComplianceController:
    - GET /api/v1/platform-admin/org/compliance → hasRole('tenant-admin')
      Returns: pendingReviews, riskScoreAvg, policyViolations, consentStats, verificationStats

Publishes AuditEventDto (ADMIN_ACTION) to iam.audit.events.
Sends NotificationCommandDto to iam.notification.commands for bulk operations.
```

**Commit:** `Phase 14: Admin service with dashboard, bulk operations, and settings`

---

## Phase 15: Monitoring Service Implementation

### Prompt 15.1 — Monitoring Service — Health and Analytics

```
Implement iam-monitoring-service (port 8087).

Package: com.iam.platform.monitoring

application.yml: port 8087, database: iam_monitoring
- monitoring.services: list of all service URLs and ports
- monitoring.health-check-interval: 30
- Redis config for health cache (30s TTL)

Create application-dev.yml, application-uat.yml, application-prod.yml following the standard pattern.

Dependencies: spring-cloud-starter-circuitbreaker-resilience4j, spring-boot-starter-data-redis,
spring-boot-starter-data-elasticsearch (for auth analytics queries)

SecurityConfig (@EnableMethodSecurity): standard RBAC pattern.
- /actuator/** → permitAll
- /api/v1/monitoring/** → hasAnyRole('ops-admin', 'iam-admin')
- /api/v1/monitoring/auth-analytics/tenant/** → hasAnyRole('ops-admin', 'iam-admin', 'tenant-admin')

Publishes AuditEventDto to iam.audit.events for incident lifecycle changes.

Services:
1. HealthAggregationService — calls /actuator/health on all services
   Parallel WebClient calls with 5s timeout, caches in Redis
2. AuthAnalyticsService — queries Elasticsearch audit events for login patterns, MFA rates
3. XRoadMetricsService — queries Prometheus for X-Road throughput/latency

DTOs: ServiceHealthDto, AggregatedHealthResponse, AuthAnalyticsDto, XRoadMetricsDto

Controllers:
- GET /api/v1/monitoring/health → hasAnyRole('ops-admin', 'iam-admin')
- GET /api/v1/monitoring/services → hasAnyRole('ops-admin', 'iam-admin')
- GET /api/v1/monitoring/auth-analytics → hasAnyRole('ops-admin', 'iam-admin')
  Optional filter: ?tenantId=X for per-tenant auth analytics (login rates, MFA adoption)
- GET /api/v1/monitoring/auth-analytics/tenant/{tenantId} → hasAnyRole('ops-admin', 'iam-admin', 'tenant-admin')
  Tenant-scoped auth analytics — tenant-admin can only access their own tenantId
- GET /api/v1/monitoring/xroad-metrics → hasAnyRole('ops-admin', 'iam-admin', 'service-manager')
  Optional filter: ?tenantId=X, ?memberClass=GOV
```

### Prompt 15.2 — Monitoring Service — Incidents and Alerts

```
Add incident tracking and alerting to iam-monitoring-service.

Entities:
- Incident (id, title, severity [CRITICAL/HIGH/MEDIUM/LOW], status [OPEN/INVESTIGATING/RESOLVED/CLOSED],
  description, serviceAffected, assignedTo, createdAt, resolvedAt, extends BaseEntity)
- AlertRule (id, name, condition, threshold, channelType, enabled, extends BaseEntity)

Flyway: V1__create_monitoring_schema.sql (include both tables)

Services:
1. IncidentService — CRUD + resolve workflow
2. AlertService — CRUD + evaluateAlerts() scheduled job
   When threshold breached: publishes AlertTriggerDto to iam.alert.triggers (→ notification service)
   Also auto-creates Incident

Controllers:
- CRUD /api/v1/monitoring/incidents → hasAnyRole('ops-admin', 'iam-admin')
- CRUD /api/v1/monitoring/alerts → hasAnyRole('ops-admin', 'iam-admin')

Publishes audit events for incident and alert changes.
```

**Commit:** `Phase 15: Monitoring service with health aggregation, analytics, incidents, alerts`

---

## Phase 16: Governance Service Implementation

### Prompt 16.1 — Governance Service — Campaigns and Workflows

```
Implement iam-governance-service (port 8088).

Package: com.iam.platform.governance

application.yml: port 8088, database: iam_governance
Dependencies: keycloak-admin-client (for user provisioning/deprovisioning)

Create application-dev.yml, application-uat.yml, application-prod.yml following the standard pattern.

SecurityConfig (@EnableMethodSecurity): standard RBAC pattern.
- /actuator/** → permitAll
- /api/v1/governance/consents → authenticated (POST: give consent; GET /me: own consents)
- /api/v1/governance/consents/{id} → authenticated (DELETE: withdraw own consent)
- /api/v1/governance/** → hasAnyRole('governance-admin', 'iam-admin')
- /api/v1/governance/campaigns/{id}/reviews → hasAnyRole('tenant-admin', 'governance-admin')
- /api/v1/governance/reports/** → hasAnyRole('report-viewer', 'governance-admin', 'iam-admin')

Entities:
- CertificationCampaign (id, name, description, status [DRAFT/ACTIVE/COMPLETED/CANCELLED],
  startDate, endDate, scope [tenant/role filter], createdBy, extends BaseEntity)
- CertificationReview (id, campaignId, userId, reviewerId, decision [APPROVE/REVOKE/PENDING],
  comments, reviewedAt)
- LifecycleWorkflow (id, name, type [ONBOARDING/OFFBOARDING/ROLE_CHANGE],
  stepsJson, approvalChainJson, enabled, extends BaseEntity)
- WorkflowExecution (id, workflowId, targetUserId, currentStep,
  status [PENDING/IN_PROGRESS/COMPLETED/REJECTED], initiatedBy, extends BaseEntity)

Flyway: V1__create_governance_schema.sql

Services:
1. CampaignService — create campaign (generates reviews), list, submit review decisions
2. WorkflowService — create workflow, execute (creates execution), advance steps
   Integrates with Keycloak Admin API for user provisioning/deprovisioning

Controllers:
- POST/GET /api/v1/governance/campaigns → hasAnyRole('governance-admin', 'iam-admin')
- GET /api/v1/governance/campaigns/{id}/reviews → hasAnyRole('governance-admin', 'iam-admin', 'tenant-admin')
- POST /api/v1/governance/campaigns/{id}/reviews → hasAnyRole('tenant-admin', 'governance-admin')
- CRUD /api/v1/governance/workflows → hasAnyRole('governance-admin', 'iam-admin')
- POST /api/v1/governance/workflows/{id}/execute → hasAnyRole('governance-admin', 'iam-admin')

Publishes AuditEventDto (GOVERNANCE_ACTION) and NotificationCommandDto for campaign notifications.
```

### Prompt 16.2 — Governance Service — Policies, Risk, and Reports

```
Add policy enforcement, risk scoring, consent management, and compliance reporting to iam-governance-service.

Entities:
- SodPolicy (id, name, conflictingRolesJson [array of role pairs], severity, enabled, extends BaseEntity)
- RiskScore (id, userId, score, factorsJson, calculatedAt)
- ConsentRecord (id, dataSubjectType [NATURAL_PERSON/LEGAL_ENTITY_CONTACT], dataSubjectId,
  purpose, legalBasis [CONSENT/CONTRACT/LEGAL_OBLIGATION/VITAL_INTEREST/PUBLIC_INTEREST/LEGITIMATE_INTEREST],
  consentGiven, consentTimestamp, consentMethod [ELECTRONIC/WRITTEN/VERBAL],
  withdrawnAt, expiresAt, ipAddress, dataCategories JSONB, thirdPartySharing, crossBorderTransfer)
  — Required for LPDP compliance (Cambodia's upcoming data protection law)

Flyway: V2__add_governance_policies.sql (includes SodPolicy, RiskScore)
Flyway: V3__add_consent_records.sql (ConsentRecord table)

Services:
1. PolicyService — CRUD, evaluatePolicy(userId) → violations, checkConflicts(userId, role)
2. RiskScoringService — calculateRiskScore(userId)
   Factors: dormant days, role count, failed logins, last access recency
   batchCalculateAll() — scheduled nightly job
   getHighRiskUsers(threshold) — paginated
3. ConsentService — CRUD for consent records, giveConsent(), withdrawConsent(),
   getActiveConsents(dataSubjectId), checkConsent(dataSubjectId, purpose),
   exportConsents(dataSubjectId) for data subject access requests
4. ComplianceReportService — generate access/role/risk/consent reports, export PDF/Excel

Controllers:
- CRUD /api/v1/governance/policies → hasAnyRole('governance-admin', 'iam-admin')
- GET /api/v1/governance/policies/{id}/evaluate → check violations
- GET /api/v1/governance/risk-scores → hasAnyRole('governance-admin', 'iam-admin')
- GET /api/v1/governance/risk-scores/high-risk → filtered
- POST /api/v1/governance/consents → authenticated (data subject gives consent)
- GET /api/v1/governance/consents/me → authenticated (view own consents)
- DELETE /api/v1/governance/consents/{id} → authenticated (withdraw own consent)
- GET /api/v1/governance/consents → hasAnyRole('governance-admin', 'iam-admin') (admin view)
- GET /api/v1/governance/reports/** → hasAnyRole('report-viewer', 'governance-admin', 'iam-admin')
- GET /api/v1/governance/reports/export?format=pdf|excel
```

**Commit:** `Phase 16: Governance service with campaigns, policies, risk scoring, and compliance reports`

---

## Phase 17: Developer Portal Implementation

### Prompt 17.1 — Developer Portal — App Registration and API Docs

```
Implement iam-developer-portal (port 8089).

Package: com.iam.platform.developer

application.yml: port 8089, database: iam_developer
Dependencies: keycloak-admin-client, spring-cloud-starter-circuitbreaker-resilience4j

Create application-dev.yml, application-uat.yml, application-prod.yml following the standard pattern.

Entities:
- RegisteredApp (id, name, description, clientId, clientSecretEncrypted, redirectUrisJson,
  ownerId, status [ACTIVE/SUSPENDED], extends BaseEntity)
- WebhookConfig (id, appId, eventType, targetUrl, secretHash, enabled, extends BaseEntity)
- WebhookDeliveryLog (id, webhookId, eventType, httpStatus, responseTime, error, sentAt)
  — Hard delete for log rotation
- SandboxRealm (id, ownerId, realmName, expiresAt, status [ACTIVE/EXPIRED/DELETED])
  — Hard delete on expiry cleanup

Flyway: V1__create_developer_schema.sql

Services:
1. AppRegistrationService — creates Keycloak client + local record, regenerate credentials,
   manage redirect URIs (circuit breaker on Keycloak calls)
2. ApiDocService — fetches /v3/api-docs from all services, aggregates (with caching)

Controllers:
- GET /api/v1/docs/** → permitAll (public API documentation)
- GET /api/v1/sdks → permitAll
- CRUD /api/v1/apps → hasAnyRole('developer', 'iam-admin')
- POST /api/v1/apps/{id}/credentials → hasRole('developer')
- PUT /api/v1/apps/{id}/redirect-uris → hasRole('developer')

SecurityConfig: /api/v1/docs/** and /api/v1/sdks permitAll, rest RBAC.
```

### Prompt 17.2 — Developer Portal — Webhooks and Sandbox

```
Add webhooks and sandbox to iam-developer-portal.

Services:
1. WebhookService — CRUD for webhook configs
2. WebhookDispatcher — HTTP POST to registered URLs
   HMAC-SHA256 signature in X-Webhook-Signature header
   Retry: 3 attempts, exponential backoff (1s, 5s, 25s)
   Idempotency: X-Webhook-Delivery-Id header (UUID per delivery)
   Logs delivery status
3. WebhookEventConsumer — Kafka consumer on "iam.platform.events"
   Routes events to WebhookDispatcher based on registered event types
4. SandboxService — creates temporary Keycloak realm for testing
   Auto-expire after 7 days (scheduled cleanup), pre-populate with sample users/roles

Controllers:
- CRUD /api/v1/webhooks → hasAnyRole('developer', 'iam-admin')
- GET /api/v1/webhooks/{id}/deliveries → delivery log
- POST /api/v1/sandbox/realms → hasRole('developer')
- GET /api/v1/sandbox/realms → developer's sandboxes
- DELETE /api/v1/sandbox/realms/{id} → hasRole('developer')

Event types: USER_CREATED, USER_UPDATED, USER_DELETED, LOGIN_SUCCESS, LOGIN_FAILED,
ROLE_CHANGED, APP_REGISTERED, TENANT_CREATED, CONFIG_CHANGED
```

**Commit:** `Phase 17: Developer portal with app registration, webhooks, and sandbox`

---

## Phase 18: Integration Tests for Expansion Services

### Prompt 18.1 — Tests for All New Services

```
Create integration tests for the 6 new services (config, notification, admin, monitoring, governance, developer).

For EACH service, create:
1. *ApplicationTests.java — context loads
2. *SecurityTests.java — RBAC endpoint tests:
   - Unauthenticated → 401
   - Wrong role → 403
   - Correct role → 200
   - Use @WithMockUser with appropriate roles
3. *ControllerTests.java — @WebMvcTest with MockMvc
4. *ServiceTests.java — @SpringBootTest with @MockBean dependencies

Specific test focus:
- Config: feature flag CRUD, config version rollback, native endpoint permitAll
- Notification: template rendering, Kafka consumer deserialization
- Admin: dashboard stats aggregation, bulk import validation, /platform-admin/** path
- Monitoring: health aggregation timeout handling, alert evaluation
- Governance: campaign lifecycle (DRAFT→ACTIVE→COMPLETED), SoD policy conflict detection
- Developer: app registration → Keycloak client creation, webhook HMAC verification, sandbox expiry

Follow same test patterns as Phase 10. Use JwtTestUtils from iam-common.
```

**Commit:** `Phase 18: Integration tests for all expansion services`

---

## Phase 19: Docker and Kubernetes for Expansion Services

### Prompt 19.1 — Dockerfiles and Docker Compose Updates

```
Create production Dockerfiles for all 6 new services following the same multi-stage pattern
as Phase 11 (eclipse-temurin:21-jdk-alpine build → 21-jre-alpine runtime, non-root, healthcheck).

Update docker/docker-compose.services.yml to add:
- iam-config-service (8888, depends: postgres, vault) — starts FIRST
- iam-notification-service (8090, depends: postgres, kafka, mailpit)
- iam-admin-service (8086, depends: postgres, keycloak, kafka, config-service)
- iam-monitoring-service (8087, depends: postgres, redis, prometheus, elasticsearch, config-service)
- iam-governance-service (8088, depends: postgres, kafka, elasticsearch, config-service)
- iam-developer-portal (8089, depends: postgres, keycloak, kafka, config-service)

Add Docker Compose profiles:
- "config": config-service only (for bootstrap)
- "expansion": all 6 new services
- "all": everything

All compose override files (dev, uat, prod) must include the new services.
Each new service must have SPRING_PROFILES_ACTIVE set from the environment.

Create K8s manifests for new services in k8s/base/:
- Deployment (2 replicas), Service, HPA, ConfigMap per service
- Update ingress for new routes
- Config service deployment must have readinessProbe before other services start
- Update all 3 kustomize overlays (dev, uat, prod) to include new services
```

**Commit:** `Phase 19: Docker and Kubernetes for expansion services`

---

## Phase 20: Update Documentation

### Prompt 20.1 — Update All Documentation

```
Update project documentation for the complete 12-module architecture.

1. Update README.md:
   - Architecture overview with 5 domains
   - Complete module table (12 modules with ports and status)
   - Updated RBAC roles table (13 realm roles)
   - Updated quick start guide with startup order:
     a. docker compose up -d (infrastructure)
     b. Start config-service first
     c. Start remaining services
   - Inter-service dependency diagram
   - API documentation links for all 12 services
   - Test user credentials (11 users)
   - Technology stack table

2. Update CLAUDE.md with:
   - All 12 modules and their domains
   - All 13 RBAC roles
   - All client roles per service
   - Kafka topic registry
   - Database isolation list
   - Service-specific security table
   - Inter-service dependency map

3. Create/update docs/RBAC_QUICK_REFERENCE.md:
   - Complete endpoint-to-role mapping for all 12 services
   - Role-to-endpoint access matrix
   - JWT token structure examples
   - Spring Security annotation patterns
```

**Commit:** `Phase 20: Updated documentation for 12-module architecture`

---

## Phase 21: End-to-End Workflow Validation

### Prompt 21.1 — Cross-Service Integration Tests

```
Create end-to-end integration tests validating cross-service workflows.
Use @SpringBootTest with TestContainers for Kafka, PostgreSQL, Redis, Elasticsearch.

Workflows to test:

1. User Onboarding Flow:
   - Create tenant via tenant-service
   - Create user via admin-service bulk import
   - Verify audit event indexed in audit-service
   - Verify notification sent via notification-service Kafka topic

2. Access Certification Flow:
   - Create campaign via governance-service
   - Verify reviews generated
   - Submit review decisions
   - Verify audit trail in iam.audit.events

3. Developer App Registration Flow:
   - Register app via developer-portal
   - Verify Keycloak client created (mock)
   - Configure webhook
   - Publish PlatformEventDto → verify webhook delivery attempted

4. Monitoring Alert Flow:
   - Mock service health check failure
   - Verify alert rule evaluation triggers
   - Verify AlertTriggerDto published to iam.alert.triggers
   - Verify incident auto-created

5. Config Change Flow:
   - Toggle feature flag via config-service
   - Verify AuditEventDto (CONFIG_CHANGE) published
   - Verify config history entry created
   - Test rollback
```

**Commit:** `Phase 21: End-to-end cross-service workflow tests`

---

## Phase 22: Production Hardening

### Prompt 22.1 — Production Readiness

```
Apply production hardening across all services:

1. Structured Logging:
   - Add logstash-logback-encoder dependency to iam-common
   - Configure JSON log format in logback-spring.xml
   - Standard MDC fields: traceId, userId, tenantId, service
   - Create LoggingFilter that sets MDC from JWT and X-Road context

2. Rate Limiting per Role (gateway):
   - iam-admin: 1000 req/min
   - developer: 200 req/min
   - internal-user: 300 req/min
   - external-user: 60 req/min
   - api-access: 30 req/min
   Configure in gateway application.yml with Redis-backed rate limiter

3. Security Headers (all services):
   - X-Content-Type-Options: nosniff
   - X-Frame-Options: DENY
   - Strict-Transport-Security (prod profile)
   - Content-Security-Policy

4. Health Check Refinement:
   - Custom health indicators per service (DB, Kafka, ES, Keycloak, Redis)
   - Separate liveness (/actuator/health/liveness) and readiness (/actuator/health/readiness)

5. GitHub Actions CI/CD:
   - Build and test on PR
   - Docker image build on merge to main
   - Push to container registry
   - Deploy to staging automatically
```

**Commit:** `Phase 22: Production hardening — logging, rate limiting, security headers, CI/CD`

---

## Summary: Complete Phase Plan (22 Phases)

| Phase | Module/Task | Key Deliverables |
|-------|-----------|-----------------|
| **Foundation (1-4)** | | |
| 1 | Project Structure | Parent POM, 12 modules, iam-common DTOs |
| 2 | Infrastructure | Docker Compose (11 services), multi-DB init |
| 3 | Keycloak | Realm JSON, 13 roles, 11 clients, 11 test users |
| 4 | Security | JWT converter, auto-config, X-Road filter |
| **Core Platform (5-11)** | | |
| 5 | iam-core-service | User profiles, X-Road endpoints, audit publishing |
| 6 | iam-gateway | Reactive gateway, routes for all 12 services |
| 7 | iam-tenant-service | Realm provisioning, Keycloak Admin API |
| 8 | iam-audit-service | Kafka consumer, Elasticsearch indexing |
| 9 | iam-xroad-adapter | Routing, ACL, proxy |
| 10 | Testing & Docs | Integration tests, OpenAPI, README |
| 11 | Production (Core) | Dockerfiles, K8s manifests |
| **Expansion (12-22)** | | |
| 12 | iam-config-service | Config Server, feature flags (**build first**) |
| 13 | iam-notification-service | Email/SMS/Telegram, Kafka consumer |
| 14 | iam-admin-service | Dashboard, bulk ops, settings |
| 15 | iam-monitoring-service | Health, analytics, incidents, alerts |
| 16 | iam-governance-service | Campaigns, policies, risk, reports |
| 17 | iam-developer-portal | App registration, webhooks, sandbox |
| 18 | Tests (Expansion) | Integration tests for 6 new services |
| 19 | Docker+K8s (Expansion) | Dockerfiles, compose, K8s for new services |
| 20 | Documentation | Updated README, CLAUDE.md, RBAC reference |
| 21 | E2E Validation | Cross-service workflow integration tests |
| 22 | Production Hardening | Logging, rate limiting, security headers, CI/CD |

**Total: 22 phases, ~30 prompts for Claude Code Desktop**

---

## Usage Notes for Claude Code Desktop

### How to Use These Prompts

1. **Create fresh repo**: `mkdir iam-enterprise-platform && cd iam-enterprise-platform && git init`
2. **Copy CLAUDE.md** to the project root (Claude Code reads this automatically)
3. **Open Claude Code Desktop** and navigate to the project
4. **Copy and paste each prompt** in order (Phase 1 → Phase 22)
5. **After each prompt**, review and test:
   - `mvn clean compile` (check compilation)
   - `docker compose up -d` (test infrastructure)
   - `mvn spring-boot:run` (test each service)
6. **Commit after each phase**: `git add -A && git commit -m "Phase X: description"`
7. **Push periodically**: `git push origin main`

### Tips

- If a prompt is too large, Claude Code will handle it — these are sized for Opus
- Ask "fix the compilation errors" if Maven build fails
- Ask "run mvn clean test" to validate test cases
- Ask "what's missing?" after each phase for gap analysis
- Config service MUST be started before other expansion services
- The admin service uses `/api/v1/platform-admin/**` (not `/api/v1/admin/**`)
- All Kafka topics use dot-notation: `iam.audit.events` (not dashes)
- All services use Flyway migrations (not ddl-auto in production)
- All business entities extend BaseEntity (soft delete built-in)


---

# APPENDIX A: Test Prompts for Claude Code

After completing each build phase, paste the corresponding test prompt into Claude Code Desktop.

## Test Prompt — Phase 1: Shared Test Utilities

```
Create shared test utilities in iam-common that all service tests will use.

Package: com.iam.platform.common.test

1. JwtTestUtils.java:
   - static Jwt buildKeycloakJwt(String username, List<String> realmRoles, Map<String, List<String>> clientRoles)
     Creates a mock Keycloak JWT with correct claim structure:
     { "realm_access": { "roles": [...] }, "resource_access": { "client": { "roles": [...] } }, "preferred_username": "..." }
   - Convenience methods returning SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor:
     jwtAsAdmin(), jwtAsTenantAdmin(), jwtAsAuditor(), jwtAsInternalUser(),
     jwtAsExternalUser(), jwtAsConfigAdmin(), jwtAsOpsAdmin(), jwtAsGovernanceAdmin(),
     jwtAsDeveloper(), jwtAsReportViewer(), expiredJwt()

2. RbacTestCase.java (record):
   - String endpoint, String httpMethod, JwtRequestPostProcessor jwt (null=unauthenticated), int expectedStatus, String description

3. TestDataFactory.java:
   - Builders for UserProfileDto, AuditEventDto, XRoadContextDto, NotificationCommandDto, PlatformEventDto

4. AbstractIntegrationTest.java (@SpringBootTest base):
   - @Testcontainers with PostgreSQL, Kafka, Redis
   - @DynamicPropertySource to inject container URLs

Use Spring Security's SecurityMockMvcRequestPostProcessors.jwt() for JWT building.
```

## Test Prompt — Phase 4: Security Components

```
Create tests for iam-common security components.

1. KeycloakJwtAuthenticationConverterTest (7 tests):
   - testRealmRolesExtracted, testClientRolesExtracted, testBothRealmAndClientRoles
   - testEmptyRoles, testMissingRealmAccess, testPreferredUsername, testFallbackToSubject

2. XRoadRequestFilterTest (7 tests):
   - testValidHeaders, testMissingClientHeader, testMissingIdHeader, testMalformedClientHeader
   - testThreadLocalCleanup, testNonXRoadPath, testXRoadDisabled

3. ApiResponseTest (4 tests): testOk, testOkWithMessage, testError, testErrorWithCode

4. GlobalExceptionHandlerTest (5 tests):
   - testResourceNotFound→404, testAccessDenied→403, testValidationError→400
   - testIamPlatformException→500, testGenericException→500(no stack trace)

Use JUnit 5, Mockito, AssertJ. Import JwtTestUtils.
```

## Test Prompt — Phase 5: Core Service

```
Create tests for iam-core-service: unit, integration, and RBAC security.

1. UserControllerTest (@WebMvcTest):
   - testGetCurrentUser_authenticated→200, testGetCurrentUser_unauthenticated→401
   - testListUsers_asAdmin→200, testListUsers_asExternalUser→403
   - testDeleteUser_asAdmin→200, testDeleteUser_asTenantAdmin→403

2. AuditServiceTest (unit):
   - testLogApiAccess_publishesToKafka, testKafkaFailure_doesNotThrow

3. CoreServiceRbacTest — @ParameterizedTest RBAC matrix:
   Every endpoint × every role. Use JwtTestUtils convenience methods.
   Generate test cases from RBAC reference (Appendix B).

4. CoreServiceIntegrationTest (Testcontainers):
   - testFlywayMigrationsRun, testKafkaAuditEventPublished

5. CoreArchitectureTest (ArchUnit):
   - Controllers in .controller, Services in .service, no circular deps
   - All entities extend BaseEntity, all controllers have @Tag
```

## Test Prompt — Phase 6: Gateway

```
Create tests for iam-gateway (REACTIVE — use WebTestClient, NOT MockMvc).

1. GatewayRouteTest: verify each of the 12 service routes
2. GatewaySecurityTest: health→200, no token→401, valid token→routes, expired→401
3. RateLimitTest: exceed threshold→429

Do NOT import servlet-based classes. Do NOT use ThreadLocal.
```

## Test Prompt — Phases 7-9: Tenant, Audit, X-Road

```
For each service create: *RbacTest (RBAC matrix), *ServiceTest (unit), *IntegrationTest (Testcontainers), *ArchitectureTest (ArchUnit).

iam-tenant-service: RBAC (POST tenants→iam-admin only, GET→iam-admin+tenant-admin), Keycloak realm creation mock, soft delete test, Flyway migration test.

iam-audit-service: RBAC (audit events→auditor+iam-admin+report-viewer, xroad→+service-manager), Kafka→ES indexing test, monthly index naming test.

iam-xroad-adapter: RBAC (/xroad/**→permitAll, /api/v1/xroad/**→service-manager+iam-admin), ACL validation test, Redis cache test.
```

## Test Prompt — Phase 12-17: Expansion Services

```
For EACH expansion service, create: *RbacTest, *ServiceTest, *IntegrationTest, *ArchitectureTest.

iam-config-service: RBAC (native endpoints→permitAll, /api/v1/config/**→config-admin+iam-admin). Feature flag CRUD, environment filtering, config rollback. Flyway migration.

iam-notification-service: RBAC (/api/v1/notifications/**→iam-admin+ops-admin). Dispatcher routing, template variable rendering, Kafka consumer for iam.notification.commands. Email to Mailpit.

iam-admin-service: RBAC (/api/v1/platform-admin/dashboard→iam-admin, users→+tenant-admin, settings→+config-admin). Bulk import validation, circuit breaker fallback, Keycloak Admin API mock.

iam-monitoring-service: RBAC (/api/v1/monitoring/**→ops-admin+iam-admin, xroad-metrics→+service-manager). Health timeout→UNKNOWN, alert threshold evaluation, incident status transitions (OPEN→INVESTIGATING→RESOLVED→CLOSED).

iam-governance-service: RBAC (/api/v1/governance/**→governance-admin+iam-admin, reports→+report-viewer, reviews POST→+tenant-admin). Campaign lifecycle (DRAFT→ACTIVE→COMPLETED), SoD conflict detection, risk score calculation.

iam-developer-portal: RBAC (docs→permitAll, apps→developer+iam-admin, sandbox→developer). Webhook HMAC-SHA256 verification, sandbox 7-day expiry, redirect URI validation.
```

## Test Prompt — Phase 21: End-to-End

```
Create 5 E2E tests with Testcontainers (full stack) + REST Assured + Awaitility:

1. UserOnboardingE2E: Create tenant → bulk import user → verify in core-service → audit event in ES → notification on Kafka
2. SsoLoginFlowE2E: Get token → call protected API → wrong role→403 → expired→401 → audit logged
3. AccessCertificationE2E: Create campaign → activate → submit reviews → verify role changes → audit trail
4. DeveloperAppE2E: Register app → get client credentials token → configure webhook → trigger event → verify delivery
5. MonitoringAlertE2E: Create alert rule → mock health failure → alert triggers → notification published → incident created
```

## Test Prompt — Phase 22: Production Hardening

```
Create production hardening verification tests:

1. SecurityHeadersTest: verify all services return X-Content-Type-Options, X-Frame-Options, CSP headers
2. ActuatorSecurityTest: /actuator/env, /actuator/beans return 401 (not publicly accessible)
3. ErrorResponseSecurityTest: trigger 400, 404, 500 → verify NO stack traces in response body
4. DependencyScanTest: run OWASP Dependency Check → verify zero critical/high vulnerabilities
5. ContainerSecurityTest: verify Docker containers run as non-root user
6. TlsConfigTest: verify PostgreSQL, Redis, Kafka connections use TLS in UAT/PROD profiles
```

## ArchUnit Rules (apply to ALL modules)

```
Create ArchitectureTest for every module using ArchUnit:
- Controllers only in .controller, Services in .service, Entities in .entity, Repos in .repository
- Controllers → services → repositories (no skipping layers)
- No circular package dependencies
- All controllers: @RestController + @Tag
- All entities extend BaseEntity
- No ThreadLocal in iam-gateway
- Controllers end with "Controller", Services end with "Service"
```

---

# APPENDIX B: RBAC Quick Reference

## 13 Realm Roles

| # | Role | Domain | Description |
|---|------|--------|-------------|
| 1 | iam-admin | Core | Full platform administrator — access to everything (Tier 0-1) |
| 2 | tenant-admin | Admin | Organization admin — manages their own org (Tier 3) |
| 3 | sector-admin | Admin | Sector oversight for one member class GOV/COM/NGO/MUN (Tier 2) |
| 4 | service-manager | Core | Manages X-Road service registrations and ACLs |
| 5 | auditor | Governance | Read-only access to audit logs and events |
| 6 | api-access | Core | Basic API access (gateway pass-through) |
| 7 | internal-user | Core | Internal government/organization staff |
| 8 | external-user | Core | External citizen or partner user |
| 9 | config-admin | Admin | Manage platform configuration and feature flags |
| 10 | ops-admin | Operations | Access monitoring dashboards and incident management |
| 11 | governance-admin | Governance | Manage access reviews, certifications, policies |
| 12 | developer | Developer | Self-service app registration, API docs, sandbox |
| 13 | report-viewer | Governance | View compliance and audit reports (read-only) |

## Endpoint → Role Matrix (All Services)

### iam-core-service (:8082)
| Endpoint | Method | Roles |
|----------|--------|-------|
| /api/v1/persons/me | GET/PUT | authenticated |
| /api/v1/persons | GET | internal-user, tenant-admin, iam-admin |
| /api/v1/persons | POST | tenant-admin, iam-admin |
| /api/v1/persons/{id} | GET/PUT | tenant-admin, iam-admin |
| /api/v1/persons/{id}/verify | POST | iam-admin |
| /api/v1/persons/{id}/representations | GET | authenticated |
| /api/v1/entities | GET | internal-user, tenant-admin, iam-admin |
| /api/v1/entities | POST | iam-admin |
| /api/v1/entities/{id} | GET/PUT | tenant-admin, iam-admin |
| /api/v1/entities/{id}/representatives | GET | tenant-admin, iam-admin |
| /api/v1/representations | GET/POST | tenant-admin, iam-admin |
| /api/v1/representations/{id} | PUT/DELETE | tenant-admin, iam-admin |
| /api/v1/representations/{id}/verify | POST | iam-admin |
| /api/v1/users/me | GET | authenticated (backward compatibility) |
| /api/v1/users | GET | internal-user, tenant-admin, iam-admin |
| /xroad/** | * | permitAll (SS auth) |

### iam-tenant-service (:8083)
| Endpoint | Method | Roles |
|----------|--------|-------|
| /api/v1/tenants | GET | iam-admin, tenant-admin |
| /api/v1/tenants | POST | iam-admin |
| /api/v1/tenants/{id} | PUT/DELETE | iam-admin |
| /api/v1/tenants/{id}/users | GET/POST | tenant-admin, iam-admin |

### iam-audit-service (:8084)
| Endpoint | Method | Roles |
|----------|--------|-------|
| /api/v1/audit/events | GET | auditor, iam-admin, report-viewer |
| /api/v1/audit/xroad | GET | auditor, iam-admin, service-manager |
| /api/v1/audit/stats | GET | auditor, iam-admin |
| /api/v1/audit/events/export | GET | auditor, iam-admin |

### iam-xroad-adapter (:8085)
| Endpoint | Method | Roles |
|----------|--------|-------|
| /xroad/** | * | permitAll (SS auth) |
| /api/v1/xroad/services | GET/POST | service-manager, iam-admin |
| /api/v1/xroad/acl | GET/POST/DELETE | service-manager, iam-admin |

### iam-admin-service (:8086)
| Endpoint | Method | Roles |
|----------|--------|-------|
| /api/v1/platform-admin/platform/dashboard | GET | iam-admin |
| /api/v1/platform-admin/platform/usage/** | GET | iam-admin |
| /api/v1/platform-admin/sector-admins | GET/POST/PUT/DELETE | iam-admin |
| /api/v1/platform-admin/users | GET | iam-admin, tenant-admin |
| /api/v1/platform-admin/users/bulk-* | POST/GET | iam-admin |
| /api/v1/platform-admin/settings | GET/PUT | iam-admin, config-admin |
| /api/v1/platform-admin/sector/dashboard | GET | sector-admin |
| /api/v1/platform-admin/sector/organizations | GET | sector-admin |
| /api/v1/platform-admin/sector/organizations/{id}/approve | POST | sector-admin |
| /api/v1/platform-admin/sector/audit | GET | sector-admin |
| /api/v1/platform-admin/sector/compliance | GET | sector-admin |
| /api/v1/platform-admin/sector/reports/** | GET | sector-admin |
| /api/v1/platform-admin/org/dashboard | GET | tenant-admin |
| /api/v1/platform-admin/org/settings | GET/PUT | tenant-admin |
| /api/v1/platform-admin/org/audit | GET | tenant-admin |
| /api/v1/platform-admin/org/audit/export | GET | tenant-admin |
| /api/v1/platform-admin/org/login-history | GET | tenant-admin |
| /api/v1/platform-admin/org/usage | GET | tenant-admin |
| /api/v1/platform-admin/org/notifications | GET/PUT | tenant-admin |
| /api/v1/platform-admin/org/compliance | GET | tenant-admin |

### iam-monitoring-service (:8087)
| Endpoint | Method | Roles |
|----------|--------|-------|
| /api/v1/monitoring/** | * | ops-admin, iam-admin |
| /api/v1/monitoring/xroad-metrics | GET | ops-admin, iam-admin, service-manager |

### iam-governance-service (:8088)
| Endpoint | Method | Roles |
|----------|--------|-------|
| /api/v1/governance/** | * | governance-admin, iam-admin |
| /api/v1/governance/campaigns/{id}/reviews | POST | tenant-admin, governance-admin |
| /api/v1/governance/consents | POST | authenticated (data subject gives consent) |
| /api/v1/governance/consents/me | GET | authenticated (own consents) |
| /api/v1/governance/consents/{id} | DELETE | authenticated (withdraw own consent) |
| /api/v1/governance/consents | GET | governance-admin, iam-admin (admin view) |
| /api/v1/governance/reports/** | GET | report-viewer, governance-admin, iam-admin |

### iam-developer-portal (:8089)
| Endpoint | Method | Roles |
|----------|--------|-------|
| /api/v1/docs/**, /api/v1/sdks | GET | permitAll |
| /api/v1/apps/** | * | developer, iam-admin |
| /api/v1/webhooks/** | * | developer, iam-admin |
| /api/v1/sandbox/** | * | developer |

### iam-notification-service (:8090)
| Endpoint | Method | Roles |
|----------|--------|-------|
| /api/v1/notifications/** | * | iam-admin, ops-admin |
| /api/v1/notifications/templates/** (write) | POST/PUT | iam-admin |

### iam-config-service (:8888)
| Endpoint | Method | Roles |
|----------|--------|-------|
| /{application}/{profile} | GET | permitAll (internal) |
| /api/v1/config/** | * | config-admin, iam-admin |

## Test Users (10)

| Username | Password | Roles |
|----------|----------|-------|
| admin-user | Admin@2026 | iam-admin, api-access |
| tax.officer | TaxOfficer@2026 | internal-user, api-access |
| auditor.user | Auditor@2026 | auditor, report-viewer, internal-user, api-access |
| citizen.user | Citizen@2026 | external-user, api-access |
| partner.user | Partner@2026 | external-user, api-access |
| config.admin | ConfigAdmin@2026 | config-admin, internal-user, api-access |
| ops.admin | OpsAdmin@2026 | ops-admin, internal-user, api-access |
| gov.admin | GovAdmin@2026 | governance-admin, internal-user, api-access |
| dev.user | DevUser@2026 | developer, api-access |
| report.viewer | ReportViewer@2026 | report-viewer, internal-user, api-access |
| sector.admin | SectorAdmin@2026 | sector-admin, internal-user, api-access (memberClass=GOV) |

## Spring Security Annotation Patterns

```java
@PreAuthorize("hasRole('iam-admin')")                         // Single role
@PreAuthorize("hasAnyRole('iam-admin', 'tenant-admin')")      // Multiple roles (OR)
@PreAuthorize("hasRole('iam-core-service_admin')")            // Client role
```

---

# APPENDIX C: Integration Strategy (5 Layers)

External systems integrate via **standard protocols** — we don't need to know their technology.

| Layer | Protocol | Coverage | Use Case |
|-------|----------|----------|----------|
| 1 | **OIDC** (OpenID Connect) | ~80% | Modern web/mobile apps (any language) |
| 2 | **SAML 2.0** | ~15% | Enterprise & legacy government systems |
| 3 | **LDAP/AD Federation** | Sync | Corporate directory integration |
| 4 | **X-Road** | G2G | Government-to-government data exchange |
| 5 | **REST API** (Client Credentials) | M2M | Machine-to-machine backend access |

### Onboarding Flow
Organization contacts us → Assessment (which protocol?) → Tenant provisioned → Client registered in Developer Portal → LDAP connected (if applicable) → Testing in Sandbox → Go live

### Integration Decision Matrix
| Their System | Protocol | Effort |
|-------------|----------|--------|
| Modern web app (React, Angular) | OIDC + PKCE | Low |
| Mobile app (iOS, Android, Flutter) | OIDC + PKCE | Low |
| Enterprise with ADFS/Azure AD | SAML 2.0 or OIDC | Medium |
| Legacy system (PHP, old Java) | OIDC (simplest) | Medium |
| Active Directory organization | LDAP Federation | Medium |
| Government data exchange | X-Road | High |
| Machine-to-machine | Client Credentials | Low |
| Cannot modify at all | OAuth2 reverse proxy | Medium |

---

# APPENDIX D: Security Hardening Checklist

## 7 Critical Findings (fix before PROD)

| # | Finding | Fix | When |
|---|---------|-----|------|
| 1 | Keycloak must be 26.5.x | Pin version in Docker + pom.xml (e.g., 26.5.5) | During build |
| 2 | No FIDO2/WebAuthn Passkeys | Enable in Keycloak realm config | Before UAT |
| 3 | No mTLS between services | Add Istio service mesh | Before PROD |
| 4 | Elasticsearch security disabled | Enable X-Pack security | Before UAT |
| 5 | Redis no TLS | Enable TLS + ACLs | Before UAT |
| 6 | Kafka no auth/encryption | Enable SASL_SSL (SCRAM-SHA-512) | Before UAT |
| 7 | No WAF | Add ModSecurity + OWASP CRS | Before PROD |

## 6 Security Layers

| Layer | Components |
|-------|-----------|
| Network | WAF, DDoS protection, TLS termination, IP allowlist |
| Perimeter | JWT validation, rate limiting (per-role), security headers, CORS |
| Authentication | FIDO2 Passkeys, TOTP MFA, brute force protection, DPoP tokens |
| Authorization | RBAC with @PreAuthorize, realm-per-tenant, X-Road ACL |
| Data | PostgreSQL encryption, Redis TLS, Kafka SASL_SSL, Vault secrets |
| Audit | Hash-chained logs, Elasticsearch, real-time alerts, incident tracking |

## Enhanced Technology Stack Additions

| Addition | Purpose |
|----------|---------|
| FIDO2/WebAuthn Passkeys | Phishing-resistant authentication |
| DPoP (RFC 9449) | Sender-constrained tokens |
| Istio service mesh | Automatic mTLS between services |
| ModSecurity/WAF | OWASP Top 10 request inspection |
| OpenTelemetry + Jaeger | Distributed tracing |
| Trivy | Container vulnerability scanning |
| GitLeaks | Prevent credentials in git |
| K8s Network Policies | Pod-to-pod isolation |

---

# APPENDIX E: Testing Strategy Summary

## Test Pyramid

| Type | Coverage | Tools | When |
|------|----------|-------|------|
| Unit Tests | 50% (80% line coverage) | JUnit 5, Mockito, AssertJ | Every commit |
| Integration Tests | 20% | Testcontainers, @SpringBootTest | Every PR |
| Security/RBAC Tests | 15% (100% endpoint coverage) | Spring Security Test | Every PR |
| Contract Tests | 10% | Spring Cloud Contract | Every PR |
| E2E Tests | 5% | REST Assured, Testcontainers | Nightly |
| Performance Tests | Baseline | k6 | Pre-release |
| Manual Tests | 10 procedures | QA team | UAT cycle |
| Compliance Tests | 25-item checklist | Auditor | Quarterly |

## Quality Gates

| Gate | When | Must Pass |
|------|------|-----------|
| Developer | Before PR | Unit tests, 80% coverage |
| CI | On PR | All automated tests, dependency scan |
| UAT Entry | Before UAT | CI green + contract tests |
| UAT Exit | Before PROD | All manual tests + performance + pen test |
| PROD Release | Before go-live | UAT exit + change approval + rollback plan |

## Performance Baselines

| Metric | Target | Critical |
|--------|--------|----------|
| Token issuance | < 200ms p95 | < 500ms p99 |
| API response (gateway) | < 300ms p95 | < 1000ms p99 |
| Gateway throughput | > 10,000 req/sec | > 5,000 req/sec |
| Concurrent SSO sessions | > 100,000 | > 50,000 |
| X-Road latency | < 500ms p95 | < 1000ms p99 |

## CI/CD Pipeline

```yaml
jobs:
  unit-tests:     mvn test jacoco:report (fail if < 70%)
  integration:    mvn verify -Pintegration-tests
  security-scan:  OWASP Dependency Check + GitLeaks
  architecture:   mvn test -Dtest="*ArchitectureTest"
  container-scan: Trivy (on Docker build)
```

---

# APPENDIX F: Manual Test Procedures

## MT-001: SSO Login Flow
1. Open app URL → Redirected to Keycloak login
2. Enter admin-user / Admin@2026 → Redirected back with session
3. Check dev tools for JWT in Authorization header
4. Navigate to admin page → Access granted
5. Logout → Session cleared
6. Access admin page directly → Redirected to login
7. Repeat in Chrome, Firefox, Safari, Edge

## MT-002: Multi-Tenant Isolation
1. Create Tenant "Ministry-A" → Realm provisioned
2. Create user-a in Ministry-A
3. Create Tenant "Bank-B" → Second realm
4. Create user-b in Bank-B
5. Login as user-a → Only sees Ministry-A users
6. user-a attempts Bank-B API → 403 Forbidden

## MT-003: MFA Enrollment
1. Login → Navigate to Account → Security → Signing In
2. Add TOTP (scan QR) → Registered
3. Logout + login → Prompted for TOTP after password
4. Enter correct code → Success
5. Wrong code 3 times → Account locked

## MT-004: Password Reset
1. Click "Forgot Password" → Enter email
2. Check Mailpit → Reset email received
3. Click link → Reset form → Set new password
4. Login with new password → Success
5. Old password → Fails

## MT-005: X-Road Exchange
1. Valid GOV headers → Data returned
2. COM headers to GOV-only service → 403
3. Missing required headers → 400
4. Check ES for audit events → Recorded

## MT-006: Developer Self-Service
1. Login as dev.user → Register app → Client ID generated
2. Use client credentials → Token received
3. Call API → 200 OK
4. Configure webhook → Create user → Webhook delivery logged

## MT-007: Monitoring & Alerting
1. Dashboard shows all services healthy
2. Stop a service → Shows DOWN
3. Alert fires → Notification sent → Incident auto-created
4. Restart service → Shows UP → Resolve incident

## MT-008: Governance Campaign
1. Create campaign "Q1 Review" → DRAFT
2. Activate → Reviews generated for users in scope
3. Tenant-admin submits review decisions
4. Revoked roles removed from Keycloak
5. Complete campaign → Compliance report available

## MT-009: Error Security
1. Invalid JSON → 400 (no stack trace)
2. Expired JWT → 401 (no token details leaked)
3. Wrong role → 403 (no endpoint info leaked)
4. SQL injection in query param → Safely handled
5. XSS payload → Sanitized
6. /actuator/env → 401 (not public)

## MT-010: Audit Trail
1. Perform operations → Events generated
2. Query /api/v1/audit/events → All visible
3. Filter by username, date, type → Correct results
4. Export CSV → Downloads correctly
