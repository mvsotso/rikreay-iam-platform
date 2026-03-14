'use client';

import { PageHeader } from '@/components/layout/page-header';
import { StatCard } from '@/components/charts/stat-card';
import { MemberClassBadge } from '@/components/shared/member-class-badge';
import { useSectorStats } from '@/services/admin';
import { Building2, Users, CheckCircle, Clock } from 'lucide-react';
import { BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer } from 'recharts';

export function SectorDashboard() {
  const { data: sectors } = useSectorStats();

  const totalOrgs = sectors?.reduce((sum, s) => sum + s.organizationCount, 0) ?? 0;
  const totalUsers = sectors?.reduce((sum, s) => sum + s.userCount, 0) ?? 0;
  const avgCompliance = sectors?.length ? Math.round(sectors.reduce((sum, s) => sum + s.complianceRate, 0) / sectors.length) : 0;
  const totalPending = sectors?.reduce((sum, s) => sum + s.pendingApprovals, 0) ?? 0;

  return (
    <div className="space-y-6">
      <PageHeader title="Sector Overview" description="Sector administration and compliance" />

      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
        <StatCard title="Total Organizations" value={totalOrgs} icon={Building2} />
        <StatCard title="Total Users" value={totalUsers} icon={Users} />
        <StatCard title="Avg Compliance" value={`${avgCompliance}%`} icon={CheckCircle} />
        <StatCard title="Pending Approvals" value={totalPending} icon={Clock} />
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Sector Breakdown */}
        <div className="rounded-lg border bg-card p-6">
          <h3 className="text-lg font-semibold mb-4">Sector Breakdown</h3>
          <div className="space-y-4">
            {sectors?.map((sector) => (
              <div key={sector.memberClass} className="flex items-center justify-between border-b pb-3 last:border-0">
                <div className="flex items-center gap-3">
                  <MemberClassBadge memberClass={sector.memberClass} />
                  <div>
                    <p className="text-sm font-medium">{sector.organizationCount} organizations</p>
                    <p className="text-xs text-muted-foreground">{sector.userCount} users</p>
                  </div>
                </div>
                <div className="text-right">
                  <p className="text-sm font-medium">{sector.complianceRate}%</p>
                  <p className="text-xs text-muted-foreground">{sector.pendingApprovals} pending</p>
                </div>
              </div>
            ))}
          </div>
        </div>

        {/* Compliance Chart */}
        <div className="rounded-lg border bg-card p-6">
          <h3 className="text-lg font-semibold mb-4">Compliance by Sector</h3>
          <div className="h-64">
            <ResponsiveContainer width="100%" height="100%">
              <BarChart data={sectors ?? []} layout="vertical">
                <CartesianGrid strokeDasharray="3 3" className="stroke-muted" />
                <XAxis type="number" domain={[0, 100]} tick={{ fill: 'hsl(var(--muted-foreground))' }} />
                <YAxis dataKey="memberClass" type="category" width={50} tick={{ fill: 'hsl(var(--muted-foreground))' }} />
                <Tooltip contentStyle={{ backgroundColor: 'hsl(var(--card))', border: '1px solid hsl(var(--border))' }} />
                <Bar dataKey="complianceRate" fill="hsl(var(--primary))" name="Compliance %" radius={[0, 4, 4, 0]} />
              </BarChart>
            </ResponsiveContainer>
          </div>
        </div>
      </div>
    </div>
  );
}
