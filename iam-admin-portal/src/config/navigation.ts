import {
  LayoutDashboard,
  Users,
  Building2,
  Shield,
  Activity,
  FileSearch,
  Scale,
  Code2,
  Settings,
  Bell,
  Globe,
  UserCircle,
  Network,
  Fingerprint,
  FileKey,
  AlertTriangle,
  ClipboardCheck,
  Flag,
  Building,
  BarChart3,
  type LucideIcon,
} from "lucide-react";
import { Roles, type Role } from "@/lib/constants";

export interface NavItem {
  title: string;
  href: string;
  icon: LucideIcon;
  roles?: Role[];
  children?: NavItem[];
}

export interface NavSection {
  title: string;
  items: NavItem[];
}

export const navigation: NavSection[] = [
  {
    title: "Overview",
    items: [
      {
        title: "Dashboard",
        href: "/dashboard",
        icon: LayoutDashboard,
      },
    ],
  },
  {
    title: "Identity",
    items: [
      {
        title: "Natural Persons",
        href: "/identity/persons",
        icon: Fingerprint,
        roles: [Roles.INTERNAL_USER, Roles.TENANT_ADMIN, Roles.IAM_ADMIN],
      },
      {
        title: "Legal Entities",
        href: "/identity/entities",
        icon: Building2,
        roles: [Roles.INTERNAL_USER, Roles.TENANT_ADMIN, Roles.IAM_ADMIN],
      },
      {
        title: "Representations",
        href: "/identity/representations",
        icon: FileKey,
        roles: [Roles.TENANT_ADMIN, Roles.IAM_ADMIN],
      },
    ],
  },
  {
    title: "User Management",
    items: [
      {
        title: "Users",
        href: "/users",
        icon: Users,
        roles: [Roles.IAM_ADMIN, Roles.TENANT_ADMIN],
      },
      {
        title: "Tenants",
        href: "/tenants",
        icon: Building,
        roles: [Roles.IAM_ADMIN],
      },
    ],
  },
  {
    title: "Organization",
    items: [
      {
        title: "Org Dashboard",
        href: "/org/dashboard",
        icon: BarChart3,
        roles: [Roles.TENANT_ADMIN],
      },
      {
        title: "Org Settings",
        href: "/org/settings",
        icon: Settings,
        roles: [Roles.TENANT_ADMIN],
      },
      {
        title: "Org Compliance",
        href: "/org/compliance",
        icon: ClipboardCheck,
        roles: [Roles.TENANT_ADMIN],
      },
      {
        title: "Org Audit",
        href: "/org/audit",
        icon: FileSearch,
        roles: [Roles.TENANT_ADMIN],
      },
    ],
  },
  {
    title: "Sector",
    items: [
      {
        title: "Sector Dashboard",
        href: "/sector/dashboard",
        icon: BarChart3,
        roles: [Roles.SECTOR_ADMIN],
      },
      {
        title: "Organizations",
        href: "/sector/organizations",
        icon: Building2,
        roles: [Roles.SECTOR_ADMIN],
      },
      {
        title: "Sector Reports",
        href: "/sector/reports",
        icon: ClipboardCheck,
        roles: [Roles.SECTOR_ADMIN],
      },
    ],
  },
  {
    title: "Operations",
    items: [
      {
        title: "Service Health",
        href: "/monitoring",
        icon: Activity,
        roles: [Roles.OPS_ADMIN, Roles.IAM_ADMIN],
      },
      {
        title: "Incidents",
        href: "/monitoring/incidents",
        icon: AlertTriangle,
        roles: [Roles.OPS_ADMIN, Roles.IAM_ADMIN],
      },
      {
        title: "Alerts",
        href: "/monitoring/alerts",
        icon: Bell,
        roles: [Roles.OPS_ADMIN, Roles.IAM_ADMIN],
      },
      {
        title: "Auth Analytics",
        href: "/monitoring/analytics",
        icon: BarChart3,
        roles: [Roles.OPS_ADMIN, Roles.IAM_ADMIN],
      },
    ],
  },
  {
    title: "Audit & Compliance",
    items: [
      {
        title: "Audit Events",
        href: "/audit",
        icon: FileSearch,
        roles: [Roles.AUDITOR, Roles.IAM_ADMIN, Roles.REPORT_VIEWER],
      },
      {
        title: "Login History",
        href: "/audit/login-history",
        icon: Shield,
        roles: [Roles.AUDITOR, Roles.IAM_ADMIN],
      },
    ],
  },
  {
    title: "Governance",
    items: [
      {
        title: "Campaigns",
        href: "/governance/campaigns",
        icon: ClipboardCheck,
        roles: [Roles.GOVERNANCE_ADMIN],
      },
      {
        title: "Consents",
        href: "/governance/consents",
        icon: FileKey,
        roles: [Roles.GOVERNANCE_ADMIN],
      },
      {
        title: "Policies",
        href: "/governance/policies",
        icon: Scale,
        roles: [Roles.GOVERNANCE_ADMIN],
      },
      {
        title: "Workflows",
        href: "/governance/workflows",
        icon: Network,
        roles: [Roles.GOVERNANCE_ADMIN],
      },
      {
        title: "Reports",
        href: "/governance/reports",
        icon: BarChart3,
        roles: [Roles.GOVERNANCE_ADMIN, Roles.REPORT_VIEWER],
      },
    ],
  },
  {
    title: "Developer",
    items: [
      {
        title: "Applications",
        href: "/developer/apps",
        icon: Code2,
        roles: [Roles.DEVELOPER],
      },
      {
        title: "Webhooks",
        href: "/developer/webhooks",
        icon: Globe,
        roles: [Roles.DEVELOPER],
      },
      {
        title: "API Docs",
        href: "/developer/docs",
        icon: FileSearch,
      },
    ],
  },
  {
    title: "Configuration",
    items: [
      {
        title: "Feature Flags",
        href: "/config/flags",
        icon: Flag,
        roles: [Roles.CONFIG_ADMIN, Roles.IAM_ADMIN],
      },
      {
        title: "Settings",
        href: "/config/settings",
        icon: Settings,
        roles: [Roles.CONFIG_ADMIN, Roles.IAM_ADMIN],
      },
    ],
  },
  {
    title: "Notifications",
    items: [
      {
        title: "Notification Logs",
        href: "/notifications",
        icon: Bell,
        roles: [Roles.IAM_ADMIN, Roles.OPS_ADMIN],
      },
      {
        title: "Templates",
        href: "/notifications/templates",
        icon: FileSearch,
        roles: [Roles.IAM_ADMIN, Roles.OPS_ADMIN],
      },
    ],
  },
  {
    title: "X-Road",
    items: [
      {
        title: "Services",
        href: "/xroad/services",
        icon: Globe,
        roles: [Roles.SERVICE_MANAGER, Roles.IAM_ADMIN],
      },
      {
        title: "ACL",
        href: "/xroad/acl",
        icon: Shield,
        roles: [Roles.SERVICE_MANAGER, Roles.IAM_ADMIN],
      },
    ],
  },
];

export function getVisibleNavigation(userRoles: string[]): NavSection[] {
  return navigation
    .map((section) => ({
      ...section,
      items: section.items.filter(
        (item) => !item.roles || item.roles.some((role) => userRoles.includes(role))
      ),
    }))
    .filter((section) => section.items.length > 0);
}
