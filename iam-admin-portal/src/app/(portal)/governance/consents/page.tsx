'use client';

import { useState } from 'react';
import { ColumnDef } from '@tanstack/react-table';
import { useConsents } from '@/services/governance';
import { ConsentRecord } from '@/types/admin';
import { DataTable } from '@/components/data-table/data-table';
import { PageHeader } from '@/components/layout/page-header';
import { StatusBadge } from '@/components/shared/status-badge';
import { formatDateTime } from '@/lib/utils';

const columns: ColumnDef<ConsentRecord, unknown>[] = [
  {
    accessorKey: 'dataSubjectId',
    header: 'Data Subject',
    cell: ({ row }) => (
      <span className="font-medium text-sm font-mono">{row.original.dataSubjectId}</span>
    ),
  },
  {
    accessorKey: 'purpose',
    header: 'Purpose',
    cell: ({ row }) => <span className="text-sm">{row.original.purpose}</span>,
  },
  {
    accessorKey: 'legalBasis',
    header: 'Legal Basis',
    cell: ({ row }) => <span className="text-sm">{row.original.legalBasis}</span>,
  },
  {
    accessorKey: 'consentMethod',
    header: 'Method',
    cell: ({ row }) => <StatusBadge status={row.original.consentMethod} />,
  },
  {
    accessorKey: 'granted',
    header: 'Granted',
    cell: ({ row }) => (
      <StatusBadge status={row.original.granted ? 'GRANTED' : 'DENIED'} />
    ),
  },
  {
    accessorKey: 'grantedAt',
    header: 'Granted At',
    cell: ({ row }) => (
      <span className="text-sm text-muted-foreground">{formatDateTime(row.original.grantedAt)}</span>
    ),
  },
  {
    accessorKey: 'expiresAt',
    header: 'Expires At',
    cell: ({ row }) => (
      <span className="text-sm text-muted-foreground">
        {row.original.expiresAt ? formatDateTime(row.original.expiresAt) : '—'}
      </span>
    ),
  },
  {
    accessorKey: 'revokedAt',
    header: 'Revoked At',
    cell: ({ row }) => (
      <span className="text-sm text-muted-foreground">
        {row.original.revokedAt ? formatDateTime(row.original.revokedAt) : '—'}
      </span>
    ),
  },
];

export default function ConsentsPage() {
  const [pagination, setPagination] = useState({ pageIndex: 0, pageSize: 10 });

  const { data, isLoading } = useConsents({
    page: pagination.pageIndex,
    size: pagination.pageSize,
    sort: 'grantedAt',
    direction: 'desc',
  });

  return (
    <div className="space-y-6">
      <PageHeader
        title="Consent Records"
        description="LPDP consent tracking for data subjects — read-only view"
      />

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
