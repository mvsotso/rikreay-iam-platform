'use client';

import { useParams, useRouter } from 'next/navigation';
import Link from 'next/link';
import { useIncident, useUpdateIncident } from '@/services/monitoring';
import type { Incident } from '@/types/admin';
import { PageHeader } from '@/components/layout/page-header';
import { StatusBadge } from '@/components/shared/status-badge';
import { formatDateTime } from '@/lib/utils';
import { ArrowLeft, Search, CheckCircle, XCircle, Shield } from 'lucide-react';
import { toast } from 'sonner';

export default function IncidentDetailPage() {
  const { id } = useParams<{ id: string }>();
  const router = useRouter();
  const { data: incident, isLoading } = useIncident(id);
  const updateIncident = useUpdateIncident(id);

  const handleStatusUpdate = async (status: string) => {
    try {
      await updateIncident.mutateAsync({ status } as Partial<Incident>);
      toast.success(`Incident marked as ${status.toLowerCase()}`);
    } catch {
      toast.error('Failed to update incident status');
    }
  };

  if (isLoading) {
    return (
      <div className="space-y-4">
        {Array.from({ length: 3 }).map((_, i) => (
          <div key={i} className="h-24 rounded-lg bg-muted animate-pulse" />
        ))}
      </div>
    );
  }

  if (!incident) {
    return (
      <div className="text-center py-12 text-muted-foreground">Incident not found</div>
    );
  }

  const isTerminal = incident.status === 'RESOLVED' || incident.status === 'CLOSED';

  return (
    <div className="space-y-6">
      <PageHeader
        title={incident.title}
        description={`${incident.serviceName} — ${incident.severity}`}
        actions={
          <div className="flex gap-2">
            <Link
              href="/monitoring/incidents"
              className="inline-flex items-center gap-2 rounded-md border px-3 py-2 text-sm hover:bg-accent"
            >
              <ArrowLeft className="h-4 w-4" /> Back
            </Link>
          </div>
        }
      />

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Main Content */}
        <div className="lg:col-span-2 space-y-6">
          <div className="rounded-lg border bg-card p-6">
            <h3 className="text-lg font-semibold mb-4">Description</h3>
            <p className="text-sm text-muted-foreground whitespace-pre-wrap">
              {incident.description || 'No description provided.'}
            </p>
          </div>

          {/* Status Actions */}
          {!isTerminal && (
            <div className="rounded-lg border bg-card p-6">
              <h3 className="text-lg font-semibold mb-4">Update Status</h3>
              <div className="flex flex-wrap gap-3">
                {incident.status === 'OPEN' && (
                  <button
                    onClick={() => handleStatusUpdate('INVESTIGATING')}
                    disabled={updateIncident.isPending}
                    className="inline-flex items-center gap-2 rounded-md bg-yellow-600 px-4 py-2 text-sm font-medium text-white hover:bg-yellow-700 disabled:opacity-50"
                  >
                    <Search className="h-4 w-4" /> Mark Investigating
                  </button>
                )}
                {(incident.status === 'OPEN' || incident.status === 'INVESTIGATING') && (
                  <button
                    onClick={() => handleStatusUpdate('MITIGATED')}
                    disabled={updateIncident.isPending}
                    className="inline-flex items-center gap-2 rounded-md bg-blue-600 px-4 py-2 text-sm font-medium text-white hover:bg-blue-700 disabled:opacity-50"
                  >
                    <Shield className="h-4 w-4" /> Mark Mitigated
                  </button>
                )}
                {incident.status !== 'RESOLVED' && (
                  <button
                    onClick={() => handleStatusUpdate('RESOLVED')}
                    disabled={updateIncident.isPending}
                    className="inline-flex items-center gap-2 rounded-md bg-green-600 px-4 py-2 text-sm font-medium text-white hover:bg-green-700 disabled:opacity-50"
                  >
                    <CheckCircle className="h-4 w-4" /> Mark Resolved
                  </button>
                )}
                {incident.status !== 'CLOSED' && (
                  <button
                    onClick={() => handleStatusUpdate('CLOSED')}
                    disabled={updateIncident.isPending}
                    className="inline-flex items-center gap-2 rounded-md border px-4 py-2 text-sm font-medium hover:bg-accent disabled:opacity-50"
                  >
                    <XCircle className="h-4 w-4" /> Close
                  </button>
                )}
              </div>
            </div>
          )}
        </div>

        {/* Sidebar */}
        <div className="space-y-6">
          <div className="rounded-lg border bg-card p-6">
            <h3 className="text-sm font-semibold mb-3">Details</h3>
            <dl className="space-y-3 text-sm">
              <div className="flex items-center justify-between">
                <dt className="text-muted-foreground">Status</dt>
                <dd><StatusBadge status={incident.status} /></dd>
              </div>
              <div className="flex items-center justify-between">
                <dt className="text-muted-foreground">Severity</dt>
                <dd><StatusBadge status={incident.severity} /></dd>
              </div>
              <div className="flex items-center justify-between">
                <dt className="text-muted-foreground">Service</dt>
                <dd className="font-medium">{incident.serviceName}</dd>
              </div>
              <div className="flex items-center justify-between">
                <dt className="text-muted-foreground">Assigned To</dt>
                <dd className="font-medium">{incident.assignedTo ?? '—'}</dd>
              </div>
            </dl>
          </div>

          <div className="rounded-lg border bg-card p-6">
            <h3 className="text-sm font-semibold mb-3">Timestamps</h3>
            <div className="space-y-2 text-sm">
              <div>
                <span className="text-muted-foreground">Created:</span>{' '}
                <span>{formatDateTime(incident.createdAt)}</span>
              </div>
              <div>
                <span className="text-muted-foreground">Updated:</span>{' '}
                <span>{formatDateTime(incident.updatedAt)}</span>
              </div>
              {incident.resolvedAt && (
                <div>
                  <span className="text-muted-foreground">Resolved:</span>{' '}
                  <span>{formatDateTime(incident.resolvedAt)}</span>
                </div>
              )}
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
