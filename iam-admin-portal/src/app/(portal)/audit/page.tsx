'use client';

import Link from 'next/link';
import { useAuditStats } from '@/services/audit';
import { PageHeader } from '@/components/layout/page-header';
import { StatCard } from '@/components/charts/stat-card';
import { FileText, CheckCircle, XCircle, TrendingUp, Search, LogIn } from 'lucide-react';
import {
  BarChart,
  Bar,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
  Cell,
} from 'recharts';

const COLORS = [
  'hsl(var(--primary))',
  'hsl(var(--destructive))',
  '#f59e0b',
  '#10b981',
  '#8b5cf6',
  '#ec4899',
  '#06b6d4',
  '#84cc16',
  '#f97316',
  '#6366f1',
];

export default function AuditPage() {
  const { data: stats, isLoading } = useAuditStats('7d');

  const successRate =
    stats && stats.totalEvents > 0
      ? ((stats.successCount / stats.totalEvents) * 100).toFixed(1)
      : '0.0';

  const eventsByType = (stats?.eventsByType ?? [])
    .sort((a, b) => b.count - a.count)
    .slice(0, 10);

  return (
    <div className="space-y-6">
      <PageHeader
        title="Audit"
        description="Audit event statistics, trends, and compliance tracking"
        actions={
          <div className="flex gap-2">
            <Link
              href="/audit/events"
              className="inline-flex items-center gap-2 rounded-md border px-4 py-2 text-sm font-medium hover:bg-accent"
            >
              <Search className="h-4 w-4" /> Event Search
            </Link>
            <Link
              href="/audit/login-history"
              className="inline-flex items-center gap-2 rounded-md bg-primary px-4 py-2 text-sm font-medium text-primary-foreground hover:bg-primary/90"
            >
              <LogIn className="h-4 w-4" /> Login History
            </Link>
          </div>
        }
      />

      {/* Stat Cards */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
        <StatCard
          title="Total Events (7d)"
          value={stats?.totalEvents ?? 0}
          icon={FileText}
        />
        <StatCard
          title="Successful"
          value={stats?.successCount ?? 0}
          icon={CheckCircle}
        />
        <StatCard
          title="Failed"
          value={stats?.failureCount ?? 0}
          icon={XCircle}
          className={(stats?.failureCount ?? 0) > 50 ? 'border-destructive/50' : ''}
        />
        <StatCard
          title="Success Rate"
          value={`${successRate}%`}
          icon={TrendingUp}
        />
      </div>

      {/* Charts */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Events by Day */}
        <div className="rounded-lg border bg-card p-6">
          <h3 className="text-lg font-semibold mb-4">Events by Day (7 days)</h3>
          {isLoading ? (
            <div className="h-64 bg-muted animate-pulse rounded" />
          ) : (
            <ResponsiveContainer width="100%" height={260}>
              <BarChart data={stats?.eventsByDay ?? []}>
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
                  fill="hsl(var(--primary))"
                  radius={[4, 4, 0, 0]}
                  name="Events"
                />
              </BarChart>
            </ResponsiveContainer>
          )}
        </div>

        {/* Events by Type (Top 10) */}
        <div className="rounded-lg border bg-card p-6">
          <h3 className="text-lg font-semibold mb-4">Events by Type (Top 10)</h3>
          {isLoading ? (
            <div className="h-64 bg-muted animate-pulse rounded" />
          ) : eventsByType.length === 0 ? (
            <p className="text-sm text-muted-foreground">No event data available</p>
          ) : (
            <ResponsiveContainer width="100%" height={260}>
              <BarChart data={eventsByType} layout="vertical">
                <CartesianGrid strokeDasharray="3 3" className="stroke-muted" />
                <XAxis type="number" tick={{ fontSize: 12 }} />
                <YAxis
                  type="category"
                  dataKey="type"
                  tick={{ fontSize: 11 }}
                  width={120}
                />
                <Tooltip />
                <Bar dataKey="count" radius={[0, 4, 4, 0]} name="Count">
                  {eventsByType.map((_, index) => (
                    <Cell key={index} fill={COLORS[index % COLORS.length]} />
                  ))}
                </Bar>
              </BarChart>
            </ResponsiveContainer>
          )}
        </div>
      </div>
    </div>
  );
}
