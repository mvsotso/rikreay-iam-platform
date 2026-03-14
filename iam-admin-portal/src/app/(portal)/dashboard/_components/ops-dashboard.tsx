'use client';

import { PageHeader } from '@/components/layout/page-header';
import { StatCard } from '@/components/charts/stat-card';
import { StatusBadge } from '@/components/shared/status-badge';
import { useServiceHealth, useAuthAnalytics } from '@/services/admin';
import { useIncidents, useAlerts } from '@/services/monitoring';
import { Server, AlertTriangle, Bell, Activity, Clock } from 'lucide-react';
import { formatRelative } from '@/lib/utils';

export function OpsDashboard() {
  const { data: health } = useServiceHealth();
  const { data: incidents } = useIncidents({ size: 5, sort: 'createdAt', direction: 'desc' });
  const { data: alerts } = useAlerts({ size: 5, sort: 'createdAt', direction: 'desc' });
  const { data: analytics } = useAuthAnalytics('24h');

  const healthyCount = health?.filter(h => h.status === 'UP').length ?? 0;
  const totalCount = health?.length ?? 0;
  const openIncidents = incidents?.content?.filter(i => i.status !== 'RESOLVED').length ?? 0;
  const activeAlerts = alerts?.content?.filter(a => a.status === 'ACTIVE').length ?? 0;

  return (
    <div className="space-y-6">
      <PageHeader title="Operations Dashboard" description="Service health, incidents, and alerts" />

      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
        <StatCard title="Services Status" value={`${healthyCount}/${totalCount} UP`} icon={Server} />
        <StatCard title="Open Incidents" value={openIncidents} icon={AlertTriangle} className={openIncidents > 0 ? 'border-destructive/50' : ''} />
        <StatCard title="Active Alerts" value={activeAlerts} icon={Bell} className={activeAlerts > 0 ? 'border-yellow-500/50' : ''} />
        <StatCard title="Logins (24h)" value={analytics?.totalLogins ?? 0} icon={Activity} />
      </div>

      {/* Service Health Grid - Larger */}
      <div className="rounded-lg border bg-card p-6">
        <h3 className="text-lg font-semibold mb-4">Service Health Overview</h3>
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
          {health?.map((service) => (
            <div key={service.serviceName} className="flex items-center justify-between rounded-md border p-4">
              <div>
                <p className="font-medium">{service.serviceName}</p>
                <p className="text-xs text-muted-foreground">{service.responseTime}ms</p>
              </div>
              <StatusBadge status={service.status} />
            </div>
          ))}
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Recent Incidents */}
        <div className="rounded-lg border bg-card p-6">
          <h3 className="text-lg font-semibold mb-4">Recent Incidents</h3>
          <div className="space-y-3">
            {incidents?.content?.length === 0 && <p className="text-muted-foreground text-sm">No recent incidents</p>}
            {incidents?.content?.map((incident) => (
              <div key={incident.id} className="flex items-start justify-between border-b pb-3 last:border-0">
                <div className="min-w-0 flex-1">
                  <p className="font-medium text-sm truncate">{incident.title}</p>
                  <div className="flex items-center gap-2 mt-1">
                    <StatusBadge status={incident.severity} />
                    <span className="text-xs text-muted-foreground flex items-center gap-1">
                      <Clock className="h-3 w-3" />
                      {formatRelative(incident.createdAt)}
                    </span>
                  </div>
                </div>
                <StatusBadge status={incident.status} />
              </div>
            ))}
          </div>
        </div>

        {/* Recent Alerts */}
        <div className="rounded-lg border bg-card p-6">
          <h3 className="text-lg font-semibold mb-4">Recent Alerts</h3>
          <div className="space-y-3">
            {alerts?.content?.length === 0 && <p className="text-muted-foreground text-sm">No recent alerts</p>}
            {alerts?.content?.map((alert) => (
              <div key={alert.id} className="flex items-start justify-between border-b pb-3 last:border-0">
                <div className="min-w-0 flex-1">
                  <p className="font-medium text-sm truncate">{alert.title}</p>
                  <p className="text-xs text-muted-foreground mt-1">{alert.source}</p>
                </div>
                <StatusBadge status={alert.status} />
              </div>
            ))}
          </div>
        </div>
      </div>
    </div>
  );
}
