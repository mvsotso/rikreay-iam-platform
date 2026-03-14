'use client';

import { useState } from 'react';
import { ColumnDef } from '@tanstack/react-table';
import { useAlerts, useAcknowledgeAlert, useResolveAlert } from '@/services/monitoring';
import { Alert } from '@/types/admin';
import { DataTable } from '@/components/data-table/data-table';
import { PageHeader } from '@/components/layout/page-header';
import { StatusBadge } from '@/components/shared/status-badge';
import { formatDateTime } from '@/lib/utils';
import { Bell, CheckCircle, Eye } from 'lucide-react';
import { toast } from 'sonner';

function AlertActions({ alert }: { alert: Alert }) {
  const acknowledgeMutation = useAcknowledgeAlert();
  const resolveMutation = useResolveAlert();

  const handleAcknowledge = async () => {
    try {
      await acknowledgeMutation.mutateAsync(alert.id);
      toast.success('Alert acknowledged');
    } catch {
      toast.error('Failed to acknowledge alert');
    }
  };

  const handleResolve = async () => {
    try {
      await resolveMutation.mutateAsync(alert.id);
      toast.success('Alert resolved');
    } catch {
      toast.error('Failed to resolve alert');
    }
  };

  return (
    <div className="flex items-center gap-2">
      {alert.status === 'ACTIVE' && (
        <button
          onClick={handleAcknowledge}
          disabled={acknowledgeMutation.isPending}
          className="inline-flex items-center gap-1 rounded-md border px-2 py-1 text-xs font-medium hover:bg-accent disabled:opacity-50"
          title="Acknowledge"
        >
          <Eye className="h-3 w-3" /> Ack
        </button>
      )}
      {(alert.status === 'ACTIVE' || alert.status === 'ACKNOWLEDGED') && (
        <button
          onClick={handleResolve}
          disabled={resolveMutation.isPending}
          className="inline-flex items-center gap-1 rounded-md bg-green-600 px-2 py-1 text-xs font-medium text-white hover:bg-green-700 disabled:opacity-50"
          title="Resolve"
        >
          <CheckCircle className="h-3 w-3" /> Resolve
        </button>
      )}
      {alert.status === 'RESOLVED' && (
        <span className="text-xs text-muted-foreground">Resolved</span>
      )}
    </div>
  );
}

const columns: ColumnDef<Alert, unknown>[] = [
  {
    accessorKey: 'title',
    header: 'Title',
    cell: ({ row }) => (
      <span className="font-medium text-sm">{row.original.title}</span>
    ),
  },
  {
    accessorKey: 'message',
    header: 'Message',
    cell: ({ row }) => (
      <span className="text-sm text-muted-foreground truncate max-w-[200px] block">
        {row.original.message.length > 80
          ? `${row.original.message.slice(0, 80)}...`
          : row.original.message}
      </span>
    ),
  },
  {
    accessorKey: 'severity',
    header: 'Severity',
    cell: ({ row }) => <StatusBadge status={row.original.severity} />,
  },
  {
    accessorKey: 'status',
    header: 'Status',
    cell: ({ row }) => <StatusBadge status={row.original.status} />,
  },
  {
    accessorKey: 'source',
    header: 'Source',
    cell: ({ row }) => <span className="text-sm">{row.original.source}</span>,
  },
  {
    accessorKey: 'createdAt',
    header: 'Created',
    cell: ({ row }) => (
      <span className="text-sm text-muted-foreground">
        {formatDateTime(row.original.createdAt)}
      </span>
    ),
  },
  {
    id: 'actions',
    header: 'Actions',
    cell: ({ row }) => <AlertActions alert={row.original} />,
  },
];

const STATUS_OPTIONS = [
  { label: 'All Statuses', value: '' },
  { label: 'Active', value: 'ACTIVE' },
  { label: 'Acknowledged', value: 'ACKNOWLEDGED' },
  { label: 'Resolved', value: 'RESOLVED' },
];

export default function AlertsPage() {
  const [pagination, setPagination] = useState({ pageIndex: 0, pageSize: 10 });
  const [statusFilter, setStatusFilter] = useState('');

  const { data, isLoading } = useAlerts({
    page: pagination.pageIndex,
    size: pagination.pageSize,
    sort: 'createdAt',
    direction: 'desc',
    status: statusFilter || undefined,
  });

  return (
    <div className="space-y-6">
      <PageHeader
        title="Alert Management"
        description="Monitor and respond to platform alerts"
      />

      <div className="flex items-center gap-4">
        <select
          value={statusFilter}
          onChange={(e) => {
            setStatusFilter(e.target.value);
            setPagination((prev) => ({ ...prev, pageIndex: 0 }));
          }}
          className="rounded-md border bg-background px-3 py-2 text-sm"
        >
          {STATUS_OPTIONS.map((opt) => (
            <option key={opt.value} value={opt.value}>
              {opt.label}
            </option>
          ))}
        </select>
      </div>

      <DataTable
        columns={columns}
        data={data?.content ?? []}
        pageCount={data?.totalPages ?? 0}
        pagination={pagination}
        onPaginationChange={setPagination}
        isLoading={isLoading}
      />
    </div>
  );
}
