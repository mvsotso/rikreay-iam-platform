# RikReay IAM Platform — Handoff Document for New Chat

## What This Is

This handoff document provides everything needed to continue building the **RikReay IAM Enterprise Platform** in a new Claude chat or Claude Code Desktop session. All design decisions are finalized, verified (151/151 checks passed), and ready for Phase 1 build.

## Project Summary

**RikReay** is Cambodia's national Identity, Access Management & Interoperability platform serving all sectors (GOV, COM, NGO, MUN). Built on Keycloak 26.5.x + Spring Boot 3.5.x + Spring Cloud 2025.0.x + X-Road with **RBAC-only** authorization.

**GitHub:** https://github.com/mvsotso/rikreay-iam-platform.git
**Author:** Sot So, Chief of Data Management Bureau, General Department of Taxation (GDT), Cambodia

## Three Build Documents (All Finalized)

Upload these 3 files to the new chat or place them in the project root:

### 1. CLAUDE.md (453 lines)
**Purpose:** Claude Code project instructions — the AI reads this first before generating any code.
**Contains:** Architecture overview, 13 RBAC roles, identity model (3 identity types, 9 entities, 16 entity types), 4-tier administration model, security matrix, deployment strategy, 28 DO NOT rules.
**Where to place:** Project root (`/CLAUDE.md`)

### 2. DEVELOPMENT_GUIDE_FINAL.md (2,849 lines)
**Purpose:** Complete 22-phase build guide with copy-paste prompts for Claude Code Desktop.
**Contains:** Phase-by-phase instructions from empty repo to production-ready platform. Each phase has exact prompts, entity definitions, controller endpoints, service logic, Flyway migrations, SecurityConfig, and commit messages.
**Where to place:** Project root (`/docs/DEVELOPMENT_GUIDE_FINAL.md`)

### 3. RikReay_Technical_Document.docx (1,821 lines, 16 sections)
**Purpose:** Professional technical architecture document for stakeholders, regulators, and auditors.
**Contains:** Platform architecture, technology stack, security architecture, identity model, administration tiers, deployment, integration strategy, dependency lifecycle management, Cambodia regulatory compliance, development standards, testing strategy, 22 build phases, service port map.
**Where to place:** Project root (`/docs/RikReay_Technical_Document.docx`)

## Key Architecture Facts (Quick Reference)

### Technology Stack
| Tech | Version | Notes |
|------|---------|-------|
| Spring Boot | 3.5.x | Latest stable |
| Spring Cloud | 2025.0.x (Northfields) | Compatible with Boot 3.5.x |
| Keycloak | 26.5.5 | Pin exact patch, never use `latest` |
| Java | 21 LTS | Use records, sealed classes |
| PostgreSQL | 16 | 11 databases, schema-per-service |
| Redis | 7 | Cache, rate limiter, ACL cache |
| Kafka | 7.6 (Confluent) | 5 topics, dot-notation |
| Elasticsearch | 8.12 | Audit log indexing |
| X-Road | 7.x | KH instance, CamDX alignment |
| Vault | 1.15 | Secrets (PROD only) |

### 12 Modules with Ports
| Module | Port | Database |
|--------|------|----------|
| iam-common | — | — |
| iam-gateway | 8081 | — |
| iam-core-service | 8082 | iam_core |
| iam-tenant-service | 8083 | iam_tenant |
| iam-audit-service | 8084 | iam_audit |
| iam-xroad-adapter | 8085 | iam_xroad |
| iam-admin-service | 8086 | iam_admin |
| iam-monitoring-service | 8087 | iam_monitoring |
| iam-governance-service | 8088 | iam_governance |
| iam-developer-portal | 8089 | iam_developer |
| iam-notification-service | 8090 | iam_notification |
| iam-config-service | 8888 | iam_config |

### 13 RBAC Realm Roles
iam-admin, tenant-admin, sector-admin, service-manager, auditor, api-access, internal-user, external-user, config-admin, ops-admin, governance-admin, developer, report-viewer

### 5 Kafka Topics
iam.audit.events, iam.xroad.events, iam.notification.commands, iam.platform.events, iam.alert.triggers

### 11 Test Users
admin-user, tax.officer, auditor.user, citizen.user, partner.user, config.admin, ops.admin, gov.admin, dev.user, report.viewer, sector.admin

### Identity Model (3 Types)
- **Natural Person** (រូបវន្តបុគ្គល) → Keycloak User, verified via MoI eID / CamDigiKey
- **Legal Entity** (នីតិបុគ្គល) → Keycloak Realm (tenant), registered via MoC, TIN from GDT
- **Representative** (អ្នកតំណាង) → Delegation relationship linking person to entity with scoped authority

### 4 Administration Tiers
- Tier 0: Platform Super Admin (iam-admin) — everything
- Tier 1: Platform Operations (ops-admin, config-admin, auditor) — day-to-day ops
- Tier 2: Sector Admin (sector-admin) — oversight for one member class (GOV/COM/NGO/MUN)
- Tier 3: Organization Admin (tenant-admin) — manages their own org with self-service dashboard

### Deployment
| Env | Location | Domain |
|-----|----------|--------|
| DEV | Local PC | localhost |
| UAT | Google Cloud VM | rikreay-uat.duckdns.org |
| PROD | Google Cloud VM (migrate to Cambodia DC when LPDP enacted) | rikreay.duckdns.org |

## Critical DO NOT Rules (Top 10)

1. **RBAC only** — No ABAC, no attribute-based policies
2. **Do NOT** use `keycloak-admin-client` in iam-core-service — realm provisioning is tenant-service's job
3. **Do NOT** add `spring-boot-starter-web` to iam-gateway — it's reactive (WebFlux)
4. **Do NOT** use `RestTemplate` — use WebClient + Resilience4j
5. **Do NOT** use `@Where` — deprecated in Hibernate 6.3+, use `@SQLRestriction`
6. **Do NOT** use `bearer-only` client type — removed in Keycloak 20+
7. **Do NOT** use `ddl-auto` in production — Flyway only
8. **Do NOT** query another service's database — use REST APIs
9. **Do NOT** use `/api/v1/admin/**` in admin-service — use `/api/v1/platform-admin/**`
10. **Do NOT** use ThreadLocal in iam-gateway — it's reactive

## Build Order (22 Phases)

```
FOUNDATION (Phases 1-4):
  Phase 1:  Maven parent POM + 12 modules + iam-common DTOs/constants/enums
  Phase 2:  Docker Compose (PostgreSQL 11 DBs, Keycloak, Redis, Kafka, ES)
  Phase 3:  Keycloak realm JSON (13 roles, 11 clients, 11 test users)
  Phase 4:  Shared security (JWT converter, X-Road filter, GlobalExceptionHandler)

CORE SERVICES (Phases 5-9):
  Phase 5:  iam-core-service (identity model: 7 entities, 5 controllers, 7 services)
  Phase 6:  iam-gateway (reactive, routes for all 12 services)
  Phase 7:  iam-tenant-service (Keycloak realm provisioning)
  Phase 8:  iam-audit-service (Kafka consumer, ES indexing, tenant-scoped queries)
  Phase 9:  iam-xroad-adapter (X-Road bridge, ACL, service registry)

TESTING & PROD PREP (Phases 10-11):
  Phase 10: Integration tests for core services + OpenAPI docs + README
  Phase 11: Dockerfiles + K8s manifests for 6 core services

EXPANSION SERVICES (Phases 12-17):
  Phase 12: iam-config-service (Spring Cloud Config — BUILDS FIRST)
  Phase 13: iam-notification-service (email/SMS/Telegram)
  Phase 14: iam-admin-service (3-tier dashboards: platform/sector/org)
  Phase 15: iam-monitoring-service (health, incidents, alerts)
  Phase 16: iam-governance-service (campaigns, policies, risk, consent)
  Phase 17: iam-developer-portal (app registration, webhooks, sandbox)

FINALIZATION (Phases 18-22):
  Phase 18: Integration tests for expansion services
  Phase 19: Dockerfiles + K8s for expansion services
  Phase 20: Documentation update (README, CLAUDE.md, RBAC reference)
  Phase 21: E2E validation (5 cross-service workflow tests)
  Phase 22: Production hardening (logging, rate limiting, security headers, CI/CD, dependency scanning)
```

## How to Start Building

### Option A: Claude Code Desktop
1. Clone the repo: `git clone https://github.com/mvsotso/rikreay-iam-platform.git`
2. Place `CLAUDE.md` in project root
3. Place `DEVELOPMENT_GUIDE_FINAL.md` in `/docs/`
4. Open the project in Claude Code Desktop
5. Copy Phase 1.1 prompt from the Development Guide
6. Paste into Claude Code and let it generate
7. Review, test, commit: `Phase 1: Maven multi-module project structure`
8. Continue with Phase 1.2, Phase 2, etc.

### Option B: New Claude Chat
1. Upload all 3 documents to the new chat
2. Say: "I want to build the RikReay IAM Platform. Start with Phase 1 from the Development Guide. CLAUDE.md has all the architecture rules."
3. Claude will read CLAUDE.md first, then follow the Development Guide phase by phase

## What Was Decided in This Session

All of these decisions are locked into the 3 documents:

1. **RBAC only (not ABAC)** — firm decision, enforced everywhere
2. **Three-tier identity model** — Natural Person, Legal Entity, Representative with 16 entity types, 13 representative roles, 5 verification levels, CamDigiKey OAuth 2.0 integration
3. **4-tier administration** — Platform Super Admin, Platform Ops, Sector Admin (new role), Organization Admin with self-service dashboard
4. **13 RBAC roles** (was 12, added sector-admin)
5. **11 test users** (was 10, added sector.admin with memberClass=GOV)
6. **Cambodia regulatory compliance** — LPDP, E-Commerce Law, Sub-Decree 252, NBC TRM Guidelines analyzed; ConsentRecord entity added for LPDP readiness
7. **Data localization** — Google Cloud VM for UAT/PROD initially; plan to migrate to Cambodia-based DC when LPDP enacted
8. **Dependency lifecycle** — Keycloak quarterly upgrades, Spring Boot 4.x migration plan for Q2 2026, X-Road 8 evaluation for Q3 2026
9. **Core-service does NOT provision Keycloak realms** — calls tenant-service REST API instead
10. **Every service phase has SecurityConfig, Flyway, and profile YAMLs** — verified across all phases

## Verification Status

**151/151 automated checks passed** covering:
- 13 RBAC roles × 3 docs
- 11 databases × 2 docs
- 5 Kafka topics × 2 docs
- 12 service ports × 2 docs
- 8 version strings × 2 docs
- 9 identity entities × 2 docs
- 16 entity types × 2 docs
- SecurityConfig in every service phase
- Flyway migration in every service phase
- Profile YAMLs in every service phase
- Audit event publishing in every relevant service
- Zero stale references (no "12 roles" or "10 users")
- Zero anti-pattern positive references
- All 22 phase headers and commit messages
- All 11 test users
- All gateway routes
- All admin tier endpoints
- Architecture rules (core-service prohibition, gateway reactivity, config-first)
