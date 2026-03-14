// Realm Roles (13) — mirrors com.iam.platform.common.constants.IamRoles
export const Roles = {
  IAM_ADMIN: "iam-admin",
  TENANT_ADMIN: "tenant-admin",
  SECTOR_ADMIN: "sector-admin",
  SERVICE_MANAGER: "service-manager",
  AUDITOR: "auditor",
  API_ACCESS: "api-access",
  INTERNAL_USER: "internal-user",
  EXTERNAL_USER: "external-user",
  CONFIG_ADMIN: "config-admin",
  OPS_ADMIN: "ops-admin",
  GOVERNANCE_ADMIN: "governance-admin",
  DEVELOPER: "developer",
  REPORT_VIEWER: "report-viewer",
} as const;

export type Role = (typeof Roles)[keyof typeof Roles];

// Member Classes
export const MemberClasses = {
  GOV: "GOV",
  COM: "COM",
  NGO: "NGO",
  MUN: "MUN",
} as const;

export type MemberClass = (typeof MemberClasses)[keyof typeof MemberClasses];
