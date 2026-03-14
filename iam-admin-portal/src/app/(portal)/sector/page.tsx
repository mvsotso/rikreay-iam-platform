'use client';

import Link from 'next/link';
import { PageHeader } from '@/components/layout/page-header';
import { StatCard } from '@/components/charts/stat-card';
import { MemberClassBadge } from '@/components/shared/member-class-badge';
import { useSectorStats } from '@/services/admin';
import {
  Building2,
  Users,
  CheckCircle,
  ClipboardCheck,
  ArrowRight,
} from 'lucide-react';

export default function SectorOverviewPage() {
  const { data: sectorStats, isLoading } = useSectorStats();

  const totalOrgs = sectorStats?.reduce((sum, s) => sum + s.organizationCount, 0) ?? 0;
  const totalUsers = sectorStats?.reduce((sum, s) => sum + s.userCount, 0) ?? 0;
  const avgCompliance = sectorStats?.length
    ? Math.round(sectorStats.reduce((sum, s) => sum + s.complianceRate, 0) / sectorStats.length)
    : 0;
  const totalPending = sectorStats?.reduce((sum, s) => sum + s.pendingApprovals, 0) ?? 0;

  return (
    <div className="space-y-6">
      <PageHeader
        title="Sector Administration"
        description="Manage and oversee organizations within your sector"
      />

      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
        <StatCard
          title="Organizations"
          value={isLoading ? '...' : totalOrgs}
          icon={Building2}
        />
        <StatCard
          title="Total Users"
          value={isLoading ? '...' : totalUsers}
          icon={Users}
        />
        <StatCard
          title="Compliance Rate"
          value={isLoading ? '...' : `${avgCompliance}%`}
          icon={CheckCircle}
        />
        <StatCard
          title="Pending Approvals"
          value={isLoading ? '...' : totalPending}
          icon={ClipboardCheck}
        />
      </div>

      {/* Sector Breakdown */}
      {sectorStats && sectorStats.length > 0 && (
        <div className="rounded-xl border bg-card p-6 shadow-sm">
          <h3 className="text-lg font-semibold mb-4">Sector Breakdown</h3>
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
            {sectorStats.map((stat) => (
              <div
                key={stat.memberClass}
                className="rounded-lg border p-4 space-y-3"
              >
                <div className="flex items-center justify-between">
                  <MemberClassBadge memberClass={stat.memberClass} />
                  <span className="text-xs text-muted-foreground">
                    {stat.complianceRate}% compliant
                  </span>
                </div>
                <div className="space-y-1">
                  <div className="flex items-center justify-between text-sm">
                    <span className="text-muted-foreground">Organizations</span>
                    <span className="font-medium">{stat.organizationCount}</span>
                  </div>
                  <div className="flex items-center justify-between text-sm">
                    <span className="text-muted-foreground">Users</span>
                    <span className="font-medium">{stat.userCount}</span>
                  </div>
                  <div className="flex items-center justify-between text-sm">
                    <span className="text-muted-foreground">Pending</span>
                    <span className="font-medium">{stat.pendingApprovals}</span>
                  </div>
                </div>
              </div>
            ))}
          </div>
        </div>
      )}

      {/* Quick Links */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
        <Link
          href="/sector/organizations"
          className="group flex items-start gap-4 rounded-xl border bg-card p-6 shadow-sm hover:bg-accent transition-colors"
        >
          <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-lg bg-primary/10">
            <Building2 className="h-5 w-5 text-primary" />
          </div>
          <div className="flex-1 min-w-0">
            <h3 className="font-semibold flex items-center gap-2">
              Organizations
              <ArrowRight className="h-4 w-4 opacity-0 -translate-x-1 transition-all group-hover:opacity-100 group-hover:translate-x-0" />
            </h3>
            <p className="mt-1 text-sm text-muted-foreground">
              View and manage all organizations in your sector
            </p>
          </div>
        </Link>
        <Link
          href="/governance"
          className="group flex items-start gap-4 rounded-xl border bg-card p-6 shadow-sm hover:bg-accent transition-colors"
        >
          <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-lg bg-primary/10">
            <ClipboardCheck className="h-5 w-5 text-primary" />
          </div>
          <div className="flex-1 min-w-0">
            <h3 className="font-semibold flex items-center gap-2">
              Compliance
              <ArrowRight className="h-4 w-4 opacity-0 -translate-x-1 transition-all group-hover:opacity-100 group-hover:translate-x-0" />
            </h3>
            <p className="mt-1 text-sm text-muted-foreground">
              Review compliance status and governance reports
            </p>
          </div>
        </Link>
      </div>
    </div>
  );
}
