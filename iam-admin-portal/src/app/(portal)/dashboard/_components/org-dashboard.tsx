'use client';

import { PageHeader } from '@/components/layout/page-header';
import { StatCard } from '@/components/charts/stat-card';
import { useOrgStats } from '@/services/admin';
import Link from 'next/link';
import { Users, UserCheck, Shield, CheckCircle, Calendar } from 'lucide-react';
import { formatDate } from '@/lib/utils';

export function OrgDashboard() {
  const { data: stats } = useOrgStats();

  return (
    <div className="space-y-6">
      <PageHeader title="Organization Dashboard" description="Your organization overview" />

      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
        <StatCard title="Users" value={stats?.userCount ?? 0} icon={Users} />
        <StatCard title="Representatives" value={stats?.representativeCount ?? 0} icon={UserCheck} />
        <StatCard title="Audit Events" value={stats?.auditEventCount ?? 0} icon={Shield} />
        <StatCard title="Compliance" value={stats?.complianceStatus ?? 'N/A'} icon={CheckCircle} className={stats?.complianceStatus === 'COMPLIANT' ? 'border-green-500/50' : stats?.complianceStatus === 'NON_COMPLIANT' ? 'border-destructive/50' : ''} />
      </div>

      <div className="rounded-lg border bg-card p-6">
        <h3 className="text-lg font-semibold mb-4">Quick Actions</h3>
        <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
          <Link href="/identity/persons" className="rounded-md border p-4 hover:bg-accent transition-colors">
            <UserCheck className="h-8 w-8 text-primary mb-2" />
            <h4 className="font-medium">Manage Persons</h4>
            <p className="text-sm text-muted-foreground">View and manage natural persons</p>
          </Link>
          <Link href="/identity/entities" className="rounded-md border p-4 hover:bg-accent transition-colors">
            <Users className="h-8 w-8 text-primary mb-2" />
            <h4 className="font-medium">Manage Entities</h4>
            <p className="text-sm text-muted-foreground">View and manage legal entities</p>
          </Link>
          <Link href="/audit" className="rounded-md border p-4 hover:bg-accent transition-colors">
            <Shield className="h-8 w-8 text-primary mb-2" />
            <h4 className="font-medium">View Audit Logs</h4>
            <p className="text-sm text-muted-foreground">Review recent activity</p>
          </Link>
        </div>
      </div>

      {stats?.lastAuditDate && (
        <div className="rounded-lg border bg-card p-6">
          <div className="flex items-center gap-2 text-sm text-muted-foreground">
            <Calendar className="h-4 w-4" />
            <span>Last audit: {formatDate(stats.lastAuditDate)}</span>
          </div>
        </div>
      )}
    </div>
  );
}
