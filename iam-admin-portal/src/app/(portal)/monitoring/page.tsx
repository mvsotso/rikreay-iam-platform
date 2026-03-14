'use client';

import Link from 'next/link';
import { useServiceHealth, useAuthAnalytics } from '@/services/admin';
import { PageHeader } from '@/components/layout/page-header';
import { StatCard } from '@/components/charts/stat-card';
import { StatusBadge } from '@/components/shared/status-badge';
import { formatRelative } from '@/lib/utils';
import { Server, AlertTriangle, Bell, Users, LogIn, XCircle, Activity } from 'lucide-react';
import {
  AreaChart,
  Area,
  BarChart,
  Bar,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
} from 'recharts';

export default function MonitoringPage() {
  const { data: health, isLoading: healthLoading } = useServiceHealth();
  const { data: analytics, isLoading: analyticsLoading } = useAuthAnalytics('7d');

  const healthyCount = health?.filter((h) => h.status === 'UP').length ?? 0;
  const totalCount = health?.length ?? 0;
  const degradedCount = health?.filter((h) => h.status === 'DEGRADED').length ?? 0;
  const downCount = health?.filter((h) => h.status === 'DOWN').length ?? 0;

  return (
    <div className="space-y-6">
      <PageHeader
        title="Monitoring"
        description="Service health, authentication analytics, and platform operations"
        actions={
          <div className="flex gap-2">
            <Link
              href="/monitoring/incidents"
              className="inline-flex items-center gap-2 rounded-md border px-4 py-2 text-sm font-medium hover:bg-accent"
            >
              <AlertTriangle className="h-4 w-4" /> Incidents
            </Link>
            <Link
              href="/monitoring/alerts"
              className="inline-flex items-center gap-2 rounded-md bg-primary px-4 py-2 text-sm font-medium text-primary-foreground hover:bg-primary/90"
            >
              <Bell className="h-4 w-4" /> Alerts
            </Link>
          </div>
        }
      />

      {/* Stat Cards */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
        <StatCard
          title="Services Healthy"
          value={`${healthyCount}/${totalCount}`}
          icon={Server}
          className={downCount > 0 ? 'border-destructive/50' : ''}
        />
        <StatCard
          title="Total Logins (7d)"
          value={analytics?.totalLogins ?? 0}
          icon={LogIn}
        />
        <StatCard
          title="Failed Logins (7d)"
          value={analytics?.failedLogins ?? 0}
          icon={XCircle}
          className={(analytics?.failedLogins ?? 0) > 100 ? 'border-destructive/50' : ''}
        />
        <StatCard
          title="Unique Users (7d)"
          value={analytics?.uniqueUsers ?? 0}
          icon={Users}
        />
      </div>

      {/* Service Health Grid */}
      <div className="rounded-lg border bg-card p-6">
        <h3 className="text-lg font-semibold mb-4 flex items-center gap-2">
          <Activity className="h-5 w-5" /> Service Health
        </h3>
        {healthLoading ? (
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
            {Array.from({ length: 6 }).map((_, i) => (
              <div key={i} className="h-20 rounded-md bg-muted animate-pulse" />
            ))}
          </div>
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
            {health?.map((service) => (
              <div
                key={service.serviceName}
                className="flex items-center justify-between rounded-md border p-4"
              >
                <div>
                  <p className="font-medium text-sm">{service.serviceName}</p>
                  <p className="text-xs text-muted-foreground mt-1">
                    {service.responseTime}ms &middot; {formatRelative(service.lastChecked)}
                  </p>
                </div>
                <StatusBadge status={service.status} />
              </div>
            ))}
            {health?.length === 0 && (
              <p className="text-sm text-muted-foreground col-span-full">No services registered</p>
            )}
          </div>
        )}
      </div>

      {/* Charts */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Login Trend */}
        <div className="rounded-lg border bg-card p-6">
          <h3 className="text-lg font-semibold mb-4">Login Trend (7 days)</h3>
          {analyticsLoading ? (
            <div className="h-64 bg-muted animate-pulse rounded" />
          ) : (
            <ResponsiveContainer width="100%" height={260}>
              <AreaChart data={analytics?.loginTrend ?? []}>
                <CartesianGrid strokeDasharray="3 3" className="stroke-muted" />
                <XAxis
                  dataKey="date"
                  tick={{ fontSize: 12 }}
                  tickFormatter={(v: string) => v.slice(5)}
                />
                <YAxis tick={{ fontSize: 12 }} />
                <Tooltip />
                <Area
                  type="monotone"
                  dataKey="count"
                  stroke="hsl(var(--primary))"
                  fill="hsl(var(--primary))"
                  fillOpacity={0.1}
                  name="Logins"
                />
              </AreaChart>
            </ResponsiveContainer>
          )}
        </div>

        {/* Failed Login Trend */}
        <div className="rounded-lg border bg-card p-6">
          <h3 className="text-lg font-semibold mb-4">Failed Logins (7 days)</h3>
          {analyticsLoading ? (
            <div className="h-64 bg-muted animate-pulse rounded" />
          ) : (
            <ResponsiveContainer width="100%" height={260}>
              <BarChart data={analytics?.failedLoginTrend ?? []}>
                <CartesianGrid strokeDasharray="3 3" className="stroke-muted" />
                <XAxis
                  dataKey="date"
                  tick={{ fontSize: 12 }}
                  tickFormatter={(v: string) => v.slice(5)}
                />
                <YAxis tick={{ fontSize: 12 }} />
                <Tooltip />
                <Bar
                  dataKey="count"
                  fill="hsl(var(--destructive))"
                  radius={[4, 4, 0, 0]}
                  name="Failed Logins"
                />
              </BarChart>
            </ResponsiveContainer>
          )}
        </div>
      </div>
    </div>
  );
}
