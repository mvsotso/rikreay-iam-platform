'use client';

import { useState } from 'react';
import Link from 'next/link';
import { ColumnDef } from '@tanstack/react-table';
import { useNotificationLogs } from '@/services/notifications';
import { NotificationLog } from '@/types/admin';
import { DataTable } from '@/components/data-table/data-table';
import { PageHeader } from '@/components/layout/page-header';
import { StatusBadge } from '@/components/shared/status-badge';
import { formatDateTime } from '@/lib/utils';
import { FileText } from 'lucide-react';

const columns: ColumnDef<NotificationLog, unknown>[] = [
  {
    accessorKey: 'recipientId',
    header: 'Recipient',
    cell: ({ row }) => (
      <span className="text-sm font-medium">{row.original.recipientId}</span>
    ),
  },
  {
    accessorKey: 'channel',
    header: 'Channel',
    cell: ({ row }) => (
      <span className="text-sm">{row.original.channel}</span>
    ),
  },
  {
    accessorKey: 'subject',
    header: 'Subject',
    cell: ({ row }) => (
      <span className="text-sm truncate max-w-[250px] block">{row.original.subject}</span>
    ),
  },
  {
    accessorKey: 'status',
    header: 'Status',
    cell: ({ row }) => <StatusBadge status={row.original.status} />,
  },
  {
    accessorKey: 'sentAt',
    header: 'Sent At',
    cell: ({ row }) => (
      <span className="text-sm text-muted-foreground whitespace-nowrap">
        {formatDateTime(row.original.sentAt)}
      </span>
    ),
  },
  {
    accessorKey: 'error',
    header: 'Error',
    cell: ({ row }) => (
      <span className="text-sm text-destructive truncate max-w-[200px] block" title={row.original.error ?? ''}>
        {row.original.error ?? '—'}
      </span>
    ),
  },
];

const CHANNEL_OPTIONS = [
  { label: 'All Channels', value: '' },
  { label: 'Email', value: 'EMAIL' },
  { label: 'SMS', value: 'SMS' },
  { label: 'Telegram', value: 'TELEGRAM' },
];

const STATUS_OPTIONS = [
  { label: 'All Statuses', value: '' },
  { label: 'Sent', value: 'SENT' },
  { label: 'Failed', value: 'FAILED' },
  { label: 'Pending', value: 'PENDING' },
  { label: 'Delivered', value: 'DELIVERED' },
];

export default function NotificationLogsPage() {
  const [pagination, setPagination] = useState({ pageIndex: 0, pageSize: 10 });
  const [channelFilter, setChannelFilter] = useState('');
  const [statusFilter, setStatusFilter] = useState('');

  const { data, isLoading } = useNotificationLogs({
    page: pagination.pageIndex,
    size: pagination.pageSize,
    sort: 'sentAt',
    direction: 'desc',
    channel: channelFilter || undefined,
    status: statusFilter || undefined,
  });

  return (
    <div className="space-y-6">
      <PageHeader
        title="Notification Logs"
        description="View notification delivery history and status"
        actions={
          <Link
            href="/notifications/templates"
            className="inline-flex items-center gap-2 rounded-md border px-4 py-2 text-sm font-medium hover:bg-accent"
          >
            <FileText className="h-4 w-4" /> Templates
          </Link>
        }
      />

      <div className="flex items-center gap-4">
        <select
          value={channelFilter}
          onChange={(e) => {
            setChannelFilter(e.target.value);
            setPagination((prev) => ({ ...prev, pageIndex: 0 }));
          }}
          className="rounded-md border bg-background px-3 py-2 text-sm"
        >
          {CHANNEL_OPTIONS.map((opt) => (
            <option key={opt.value} value={opt.value}>
              {opt.label}
            </option>
          ))}
        </select>

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
