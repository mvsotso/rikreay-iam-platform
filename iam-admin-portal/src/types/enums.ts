export type MemberClass = "GOV" | "COM" | "NGO" | "MUN";

export type EntityType =
  | "GOVERNMENT_MINISTRY"
  | "GOVERNMENT_DEPARTMENT"
  | "STATE_ENTERPRISE"
  | "MUNICIPALITY"
  | "COMMUNE"
  | "PRIVATE_LLC"
  | "SINGLE_MEMBER_LLC"
  | "PUBLIC_LIMITED"
  | "BRANCH_OFFICE"
  | "REPRESENTATIVE_OFFICE"
  | "SOLE_PROPRIETOR"
  | "PARTNERSHIP"
  | "LOCAL_NGO"
  | "INTERNATIONAL_NGO"
  | "ASSOCIATION"
  | "FOREIGN_MISSION";

export type RepresentativeRole =
  | "LEGAL_REPRESENTATIVE"
  | "AUTHORIZED_SIGNATORY"
  | "TAX_REPRESENTATIVE"
  | "FINANCE_OFFICER"
  | "IT_ADMINISTRATOR"
  | "COMPLIANCE_OFFICER"
  | "GOVERNMENT_OFFICER"
  | "DELEGATED_USER"
  | "EXTERNAL_AUDITOR";

export type DelegationScope = "FULL" | "LIMITED" | "READ_ONLY" | "SPECIFIC";

export type VerificationStatus =
  | "UNVERIFIED"
  | "BASIC"
  | "DOCUMENT_VERIFIED"
  | "EKYC_VERIFIED"
  | "IN_PERSON_VERIFIED";

export type ChannelType = "EMAIL" | "PHONE" | "TELEGRAM" | "SMS";

export type IncidentSeverity = "CRITICAL" | "HIGH" | "MEDIUM" | "LOW";
export type IncidentStatus = "OPEN" | "INVESTIGATING" | "MITIGATED" | "RESOLVED" | "CLOSED";

export type AlertStatus = "ACTIVE" | "ACKNOWLEDGED" | "RESOLVED";

export type CampaignStatus = "DRAFT" | "ACTIVE" | "COMPLETED" | "CANCELLED";

export type ConsentMethod = "ELECTRONIC" | "WRITTEN" | "VERBAL";

export type PolicyType = "ACCESS" | "SEGREGATION_OF_DUTIES" | "RECERTIFICATION" | "RISK_BASED";

export type WorkflowStatus = "PENDING" | "APPROVED" | "REJECTED" | "EXPIRED";
