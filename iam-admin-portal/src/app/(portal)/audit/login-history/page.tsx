'use client';

import { useState } from 'react';
import { ColumnDef } from '@tanstack/react-table';
import { useLoginHistory } from '@/services/audit';
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
    accessorKey: 'username',
    header: 'Username',
    cell: ({ row }) => (
      <span className="text-sm font-medium">{row.original.username}</span>
    ),
  },
  {
    accessorKey: 'action',
    header: 'Action',
    cell: ({ row }) => <span className="text-sm">{row.original.action}</span>,
  },
  {
    accessorKey: 'ipAddress',
    header: 'IP Address',
    cell: ({ row }) => (
      <span className="text-sm text-muted-foreground">{row.original.ipAddress}</span>
    ),
  },
  {
    accessorKey: 'userAgent',
    header: 'User Agent',
    cell: ({ row }) => (
      <span className="text-sm text-muted-foreground truncate max-w-[200px] block" title={row.original.userAgent ?? ''}>
        {row.original.userAgent
          ? row.original.userAgent.length > 40
            ? `${row.original.userAgent.slice(0, 40)}...`
            : row.original.userAgent
          : '—'}
      </span>
    ),
  },
  {
    accessorKey: 'outcome',
    header: 'Outcome',
    cell: ({ row }) => <StatusBadge status={row.original.outcome} />,
  },
];

export default function LoginHistoryPage() {
  const [pagination, setPagination] = useState({ pageIndex: 0, pageSize: 10 });
  const [usernameSearch, setUsernameSearch] = useState('');
  const debouncedUsername = useDebounce(usernameSearch, 300);

  const { data, isLoading } = useLoginHistory({
    page: pagination.pageIndex,
    size: pagination.pageSize,
    sort: 'timestamp',
    direction: 'desc',
    username: debouncedUsername || undefined,
  });

  return (
    <div className="space-y-6">
      <PageHeader
        title="Login History"
        description="Track login, logout, and failed authentication events"
      />

      <div className="flex items-center gap-4">
        <SearchInput
          value={usernameSearch}
          onChange={(v) => {
            setUsernameSearch(v);
            setPagination((prev) => ({ ...prev, pageIndex: 0 }));
          }}
          placeholder="Search username..."
          className="w-72"
        />
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
