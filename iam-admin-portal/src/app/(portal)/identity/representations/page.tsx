'use client';

import { useState } from 'react';
import { ColumnDef } from '@tanstack/react-table';
import { useRepresentations } from '@/services/identity';
import { Representation } from '@/types/identity';
import { DataTable } from '@/components/data-table/data-table';
import { PageHeader } from '@/components/layout/page-header';
import { StatusBadge } from '@/components/shared/status-badge';
import { formatDate } from '@/lib/utils';
import { Plus } from 'lucide-react';
import Link from 'next/link';

const columns: ColumnDef<Representation, unknown>[] = [
  {
    id: 'person',
    header: 'Person',
    cell: ({ row }) => <span className="font-medium">{row.original.naturalPersonName ?? row.original.naturalPersonId}</span>,
  },
  {
    id: 'entity',
    header: 'Entity',
    cell: ({ row }) => row.original.legalEntityName ?? row.original.legalEntityId,
  },
  {
    accessorKey: 'representativeRole',
    header: 'Role',
    cell: ({ row }) => <span className="text-xs">{row.original.representativeRole.replace(/_/g, ' ')}</span>,
  },
  {
    accessorKey: 'delegationScope',
    header: 'Scope',
  },
  {
    accessorKey: 'validFrom',
    header: 'Valid From',
    cell: ({ row }) => formatDate(row.original.validFrom),
  },
  {
    accessorKey: 'validUntil',
    header: 'Valid Until',
    cell: ({ row }) => row.original.validUntil ? formatDate(row.original.validUntil) : 'Indefinite',
  },
  {
    accessorKey: 'verificationStatus',
    header: 'Status',
    cell: ({ row }) => <StatusBadge status={row.original.verificationStatus} />,
  },
];

export default function RepresentationsPage() {
  const [pagination, setPagination] = useState({ pageIndex: 0, pageSize: 10 });

  const { data, isLoading } = useRepresentations({
    page: pagination.pageIndex,
    size: pagination.pageSize,
    sort: 'createdAt',
    direction: 'desc',
  });

  return (
    <div className="space-y-6">
      <PageHeader
        title="Representations"
        description="Manage person-to-entity representation relationships (អ្នកតំណាង)"
        actions={
          <Link href="/identity/representations/new" className="inline-flex items-center gap-2 rounded-md bg-primary px-4 py-2 text-sm font-medium text-primary-foreground hover:bg-primary/90">
            <Plus className="h-4 w-4" /> Add Representation
          </Link>
        }
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
