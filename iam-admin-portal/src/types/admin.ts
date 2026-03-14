import { MemberClass, IncidentSeverity, IncidentStatus, AlertStatus, CampaignStatus, PolicyType, WorkflowStatus, ConsentMethod } from './enums';

export interface User {
  id: string;
  username: string;
  email: string;
  firstName?: string;
  lastName?: string;
  enabled: boolean;
  emailVerified: boolean;
  realmRoles: string[];
  clientRoles: Record<string, string[]>;
  createdTimestamp: number;
  lastLoginTimestamp?: number;
}

export interface Tenant {
  id: string;
  name: string;
  realmName: string;
  memberClass: MemberClass;
  description?: string;
  enabled: boolean;
  adminEmail: string;
  adminUsername: string;
  maxUsers: number;
  currentUsers: number;
  createdAt: string;
  updatedAt: string;
}

export interface CreateTenantRequest {
  name: string;
  memberClass: MemberClass;
  description?: string;
  adminEmail: string;
  adminUsername: string;
  adminFirstName: string;
  adminLastName: string;
  maxUsers: number;
}

export interface Incident {
  id: string;
  title: string;
  description: string;
  severity: IncidentSeverity;
  status: IncidentStatus;
  serviceName: string;
  assignedTo?: string;
  resolvedAt?: string;
  createdAt: string;
  updatedAt: string;
}

export interface Alert {
  id: string;
  title: string;
  message: string;
  severity: IncidentSeverity;
  status: AlertStatus;
  source: string;
  acknowledgedBy?: string;
  acknowledgedAt?: string;
  createdAt: string;
}

export interface ServiceHealth {
  serviceName: string;
  status: 'UP' | 'DOWN' | 'DEGRADED' | 'UNKNOWN';
  responseTime: number;
  lastChecked: string;
  details?: Record<string, unknown>;
}

export interface AuthAnalytics {
  totalLogins: number;
  failedLogins: number;
  uniqueUsers: number;
  averageSessionDuration: number;
  loginTrend: { date: string; count: number }[];
  failedLoginTrend: { date: string; count: number }[];
  topUsers: { username: string; loginCount: number }[];
}

export interface AuditEvent {
  id: string;
  eventType: string;
  username: string;
  tenantId?: string;
  ipAddress: string;
  userAgent?: string;
  resourceType: string;
  resourceId?: string;
  action: string;
  outcome: 'SUCCESS' | 'FAILURE';
  details?: Record<string, unknown>;
  timestamp: string;
}

export interface AuditStats {
  totalEvents: number;
  successCount: number;
  failureCount: number;
  eventsByType: { type: string; count: number }[];
  eventsByDay: { date: string; count: number }[];
}

export interface AccessReviewCampaign {
  id: string;
  name: string;
  description?: string;
  status: CampaignStatus;
  startDate: string;
  endDate: string;
  totalReviews: number;
  completedReviews: number;
  createdBy: string;
  createdAt: string;
}

export interface ConsentRecord {
  id: string;
  dataSubjectId: string;
  purpose: string;
  legalBasis: string;
  consentMethod: ConsentMethod;
  granted: boolean;
  grantedAt: string;
  expiresAt?: string;
  revokedAt?: string;
}

export interface AccessPolicy {
  id: string;
  name: string;
  description?: string;
  policyType: PolicyType;
  rules: Record<string, unknown>;
  enabled: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface ApprovalWorkflow {
  id: string;
  requestType: string;
  requesterId: string;
  requesterName: string;
  status: WorkflowStatus;
  approvers: { userId: string; name: string; decision?: 'APPROVED' | 'REJECTED'; decidedAt?: string }[];
  createdAt: string;
  updatedAt: string;
}

export interface FeatureFlag {
  id: string;
  key: string;
  name: string;
  description?: string;
  enabled: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface PlatformSettings {
  id: string;
  key: string;
  value: string;
  category: string;
  description?: string;
  updatedAt: string;
  updatedBy: string;
}

export interface DeveloperApp {
  id: string;
  name: string;
  description?: string;
  clientId: string;
  clientSecret?: string;
  redirectUris: string[];
  status: 'ACTIVE' | 'SUSPENDED' | 'PENDING';
  createdBy: string;
  createdAt: string;
  updatedAt: string;
}

export interface Webhook {
  id: string;
  appId: string;
  url: string;
  events: string[];
  secret?: string;
  active: boolean;
  lastDeliveryAt?: string;
  lastDeliveryStatus?: number;
  createdAt: string;
}

export interface NotificationLog {
  id: string;
  recipientId: string;
  channel: string;
  subject: string;
  status: 'SENT' | 'FAILED' | 'PENDING' | 'DELIVERED';
  sentAt: string;
  error?: string;
}

export interface NotificationTemplate {
  id: string;
  name: string;
  channel: string;
  subject: string;
  body: string;
  variables: string[];
  active: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface XRoadService {
  id: string;
  serviceCode: string;
  serviceName: string;
  memberClass: MemberClass;
  memberCode: string;
  subsystemCode: string;
  serviceVersion?: string;
  url: string;
  enabled: boolean;
  createdAt: string;
}

export interface XRoadAcl {
  id: string;
  serviceId: string;
  clientMemberClass: MemberClass;
  clientMemberCode: string;
  clientSubsystemCode?: string;
  allowed: boolean;
  createdAt: string;
}

// Dashboard stats
export interface PlatformStats {
  totalUsers: number;
  totalOrganizations: number;
  totalPersons: number;
  totalEntities: number;
  activeIncidents: number;
  healthyServices: number;
  totalServices: number;
  recentAuditEvents: number;
}

export interface SectorStats {
  memberClass: MemberClass;
  organizationCount: number;
  userCount: number;
  complianceRate: number;
  pendingApprovals: number;
}

export interface OrgStats {
  userCount: number;
  representativeCount: number;
  auditEventCount: number;
  complianceStatus: 'COMPLIANT' | 'NON_COMPLIANT' | 'PENDING';
  lastAuditDate?: string;
}
