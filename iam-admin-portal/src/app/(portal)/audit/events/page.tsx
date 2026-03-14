'use client';

import { useState } from 'react';
import { ColumnDef } from '@tanstack/react-table';
import { useAuditEvents } from '@/services/audit';
import { AuditEvent } from '@/types/admin';
import { DataTable } from '@/components/data-table/data-table';
import { PageHeader } from '@/components/layout/page-header';
import { SearchInput } from '@/components/shared/search-input';
import { StatusBadge } from '@/components/shared/status-badge';
import { useDebounce } from '@/hooks/use-debounce';
import { formatDateTime } from '@/lib/utils';

const columns: ColumnDef<AuditEvent, unknown>[] = [
  {
    accessorKey: 'timestamp',
    header: 'Timestamp',
    cell: ({ row }) => (
      <span className="text-sm text-muted-foreground whitespace-nowrap">
        {formatDateTime(row.original.timestamp)}
      </span>
    ),
  },
  {
    accessorKey: 'eventType',
    header: 'Event Type',
    cell: ({ row }) => (
      <span className="text-sm font-medium">{row.original.eventType}</span>
    ),
  },
  {
    accessorKey: 'username',
    header: 'Username',
    cell: ({ row }) => <span className="text-sm">{row.original.username}</span>,
  },
  {
    accessorKey: 'action',
    header: 'Action',
    cell: ({ row }) => <span className="text-sm">{row.original.action}</span>,
  },
  {
    accessorKey: 'resourceType',
    header: 'Resource Type',
    cell: ({ row }) => (
      <span className="text-sm">{row.original.resourceType}</span>
    ),
  },
  {
    accessorKey: 'outcome',
    header: 'Outcome',
    cell: ({ row }) => <StatusBadge status={row.original.outcome} />,
  },
  {
    accessorKey: 'ipAddress',
    header: 'IP Address',
    cell: ({ row }) => (
      <span className="text-sm text-muted-foreground">{row.original.ipAddress}</span>
    ),
  },
];

const EVENT_TYPE_OPTIONS = [
  { label: 'All Types', value: '' },
  { label: 'LOGIN', value: 'LOGIN' },
  { label: 'LOGOUT', value: 'LOGOUT' },
  { label: 'FAILED_LOGIN', value: 'FAILED_LOGIN' },
  { label: 'USER_CREATED', value: 'USER_CREATED' },
  { label: 'USER_UPDATED', value: 'USER_UPDATED' },
  { label: 'USER_DELETED', value: 'USER_DELETED' },
  { label: 'ROLE_ASSIGNED', value: 'ROLE_ASSIGNED' },
  { label: 'ROLE_REMOVED', value: 'ROLE_REMOVED' },
  { label: 'TENANT_CREATED', value: 'TENANT_CREATED' },
  { label: 'TENANT_UPDATED', value: 'TENANT_UPDATED' },
  { label: 'CONFIG_CHANGED', value: 'CONFIG_CHANGED' },
  { label: 'PERMISSION_CHANGED', value: 'PERMISSION_CHANGED' },
];

export default function AuditEventsPage() {
  const [pagination, setPagination] = useState({ pageIndex: 0, pageSize: 10 });
  const [eventType, setEventType] = useState('');
  const [usernameSearch, setUsernameSearch] = useState('');
  const [fromDate, setFromDate] = useState('');
  const [toDate, setToDate] = useState('');
  const debouncedUsername = useDebounce(usernameSearch, 300);

  const { data, isLoading } = useAuditEvents({
    page: pagination.pageIndex,
    size: pagination.pageSize,
    sort: 'timestamp',
    direction: 'desc',
    eventType: eventType || undefined,
    username: debouncedUsername || undefined,
    from: fromDate || undefined,
    to: toDate || undefined,
  });

  return (
    <div className="space-y-6">
      <PageHeader
        title="Audit Events"
        description="Search and filter audit events across the platform"
      />

      <div className="flex flex-wrap items-center gap-4">
        <select
          value={eventType}
          onChange={(e) => {
            setEventType(e.target.value);
            setPagination((prev) => ({ ...prev, pageIndex: 0 }));
          }}
          className="rounded-md border bg-background px-3 py-2 text-sm"
        >
          {EVENT_TYPE_OPTIONS.map((opt) => (
            <option key={opt.value} value={opt.value}>
              {opt.label}
            </option>
          ))}
        </select>

        <SearchInput
          value={usernameSearch}
          onChange={(v) => {
            setUsernameSearch(v);
            setPagination((prev) => ({ ...prev, pageIndex: 0 }));
          }}
          placeholder="Search username..."
          className="w-64"
        />

        <div className="flex items-center gap-2">
          <label className="text-sm text-muted-foreground">From</label>
          <input
            type="date"
            value={fromDate}
            onChange={(e) => {
              setFromDate(e.target.value);
              setPagination((prev) => ({ ...prev, pageIndex: 0 }));
            }}
            className="rounded-md border bg-background px-3 py-2 text-sm"
          />
        </div>

        <div className="flex items-center gap-2">
          <label className="text-sm text-muted-foreground">To</label>
          <input
            type="date"
            value={toDate}
            onChange={(e) => {
              setToDate(e.target.value);
              setPagination((prev) => ({ ...prev, pageIndex: 0 }));
            }}
            className="rounded-md border bg-background px-3 py-2 text-sm"
          />
        </div>
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
