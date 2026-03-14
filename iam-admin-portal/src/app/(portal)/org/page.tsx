'use client';

import Link from 'next/link';
import { PageHeader } from '@/components/layout/page-header';
import { StatCard } from '@/components/charts/stat-card';
import { useOrgStats } from '@/services/admin';
import {
  Users,
  Shield,
  FileSearch,
  Bell,
  CheckCircle,
  UserCheck,
  Settings,
  ArrowRight,
} from 'lucide-react';

const quickLinks = [
  {
    title: 'Manage Users',
    description: 'View and manage organization users and representatives',
    href: '/users',
    icon: Users,
  },
  {
    title: 'Security Settings',
    description: 'Password policy, MFA, and session configuration',
    href: '/org/security',
    icon: Shield,
  },
  {
    title: 'Audit Logs',
    description: 'Review recent activity and compliance events',
    href: '/audit',
    icon: FileSearch,
  },
  {
    title: 'Notifications',
    description: 'Configure notification preferences and channels',
    href: '/notifications',
    icon: Bell,
  },
];

export default function OrgSelfServicePage() {
  const { data: stats, isLoading } = useOrgStats();

  return (
    <div className="space-y-6">
      <PageHeader
        title="Organization Self-Service"
        description="Manage your organization settings, users, and compliance"
      />

      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
        <StatCard
          title="Users"
          value={isLoading ? '...' : (stats?.userCount ?? 0)}
          icon={Users}
        />
        <StatCard
          title="Representatives"
          value={isLoading ? '...' : (stats?.representativeCount ?? 0)}
          icon={UserCheck}
        />
        <StatCard
          title="Audit Events"
          value={isLoading ? '...' : (stats?.auditEventCount ?? 0)}
          icon={FileSearch}
        />
        <StatCard
          title="Compliance"
          value={isLoading ? '...' : (stats?.complianceStatus ?? 'N/A')}
          icon={CheckCircle}
          className={
            stats?.complianceStatus === 'COMPLIANT'
              ? 'border-green-500/50'
              : stats?.complianceStatus === 'NON_COMPLIANT'
                ? 'border-destructive/50'
                : ''
          }
        />
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
        {quickLinks.map((link) => {
          const Icon = link.icon;
          return (
            <Link
              key={link.href}
              href={link.href}
              className="group flex items-start gap-4 rounded-xl border bg-card p-6 shadow-sm hover:bg-accent transition-colors"
            >
              <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-lg bg-primary/10">
                <Icon className="h-5 w-5 text-primary" />
              </div>
              <div className="flex-1 min-w-0">
                <h3 className="font-semibold flex items-center gap-2">
                  {link.title}
                  <ArrowRight className="h-4 w-4 opacity-0 -translate-x-1 transition-all group-hover:opacity-100 group-hover:translate-x-0" />
                </h3>
                <p className="mt-1 text-sm text-muted-foreground">
                  {link.description}
                </p>
              </div>
            </Link>
          );
        })}
      </div>
    </div>
  );
}
