'use client';

import { PageHeader } from '@/components/layout/page-header';
import { StatCard } from '@/components/charts/stat-card';
import { StatusBadge } from '@/components/shared/status-badge';
import { usePlatformStats, useServiceHealth, useAuthAnalytics } from '@/services/admin';
import { Users, Building2, Shield, Activity, AlertTriangle, Server, UserCheck, FileText } from 'lucide-react';
import { AreaChart, Area, BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer } from 'recharts';

export function PlatformDashboard() {
  const { data: stats, isLoading: statsLoading } = usePlatformStats();
  const { data: health, isLoading: healthLoading } = useServiceHealth();
  const { data: analytics } = useAuthAnalytics('7d');

  return (
    <div className="space-y-6">
      <PageHeader title="Platform Dashboard" description="Overview of the entire IAM platform" />

      {/* Stat Cards */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
        <StatCard title="Total Users" value={stats?.totalUsers ?? 0} icon={Users} />
        <StatCard title="Organizations" value={stats?.totalOrganizations ?? 0} icon={Building2} />
        <StatCard title="Natural Persons" value={stats?.totalPersons ?? 0} icon={UserCheck} />
        <StatCard title="Legal Entities" value={stats?.totalEntities ?? 0} icon={FileText} />
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
        <StatCard title="Active Incidents" value={stats?.activeIncidents ?? 0} icon={AlertTriangle} className={stats?.activeIncidents ? 'border-destructive/50' : ''} />
        <StatCard title="Healthy Services" value={`${stats?.healthyServices ?? 0}/${stats?.totalServices ?? 0}`} icon={Server} />
        <StatCard title="Audit Events (24h)" value={stats?.recentAuditEvents ?? 0} icon={Shield} />
        <StatCard title="Platform Health" value={health?.every(h => h.status === 'UP') ? 'All UP' : 'Issues'} icon={Activity} />
      </div>

      {/* Service Health Grid */}
      <div className="rounded-lg border bg-card p-6">
        <h3 className="text-lg font-semibold mb-4">Service Health</h3>
        {healthLoading ? (
          <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-3">
            {Array.from({ length: 8 }).map((_, i) => (
              <div key={i} className="h-16 rounded-md bg-muted animate-pulse" />
            ))}
          </div>
        ) : (
          <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-3">
            {health?.map((service) => (
              <div key={service.serviceName} className="flex items-center justify-between rounded-md border p-3">
                <span className="text-sm font-medium truncate">{service.serviceName}</span>
                <StatusBadge status={service.status} />
              </div>
            ))}
          </div>
        )}
      </div>

      {/* Charts Row */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Login Trends */}
        <div className="rounded-lg border bg-card p-6">
          <h3 className="text-lg font-semibold mb-4">Login Trends (7 days)</h3>
          <div className="h-64">
            <ResponsiveContainer width="100%" height="100%">
              <AreaChart data={analytics?.loginTrend ?? []}>
                <CartesianGrid strokeDasharray="3 3" className="stroke-muted" />
                <XAxis dataKey="date" className="text-xs" tick={{ fill: 'hsl(var(--muted-foreground))' }} />
                <YAxis className="text-xs" tick={{ fill: 'hsl(var(--muted-foreground))' }} />
                <Tooltip contentStyle={{ backgroundColor: 'hsl(var(--card))', border: '1px solid hsl(var(--border))' }} />
                <Area type="monotone" dataKey="count" stroke="hsl(var(--primary))" fill="hsl(var(--primary)/0.1)" name="Logins" />
              </AreaChart>
            </ResponsiveContainer>
          </div>
        </div>

        {/* Failed Logins */}
        <div className="rounded-lg border bg-card p-6">
          <h3 className="text-lg font-semibold mb-4">Failed Login Attempts</h3>
          <div className="h-64">
            <ResponsiveContainer width="100%" height="100%">
              <BarChart data={analytics?.failedLoginTrend ?? []}>
                <CartesianGrid strokeDasharray="3 3" className="stroke-muted" />
                <XAxis dataKey="date" className="text-xs" tick={{ fill: 'hsl(var(--muted-foreground))' }} />
                <YAxis className="text-xs" tick={{ fill: 'hsl(var(--muted-foreground))' }} />
                <Tooltip contentStyle={{ backgroundColor: 'hsl(var(--card))', border: '1px solid hsl(var(--border))' }} />
                <Bar dataKey="count" fill="hsl(var(--destructive))" name="Failed Logins" radius={[4, 4, 0, 0]} />
              </BarChart>
            </ResponsiveContainer>
          </div>
        </div>
      </div>
    </div>
  );
}
