'use client';

import { useState } from 'react';
import Link from 'next/link';
import { ColumnDef } from '@tanstack/react-table';
import { useXRoadServices } from '@/services/xroad';
import { XRoadService } from '@/types/admin';
import { DataTable } from '@/components/data-table/data-table';
import { PageHeader } from '@/components/layout/page-header';
import { StatusBadge } from '@/components/shared/status-badge';
import { MemberClassBadge } from '@/components/shared/member-class-badge';
import { formatDateTime } from '@/lib/utils';
import { Plus, Shield } from 'lucide-react';

const columns: ColumnDef<XRoadService, unknown>[] = [
  {
    accessorKey: 'serviceCode',
    header: 'Service Code',
    cell: ({ row }) => (
      <code className="rounded bg-muted px-1.5 py-0.5 text-xs font-medium">{row.original.serviceCode}</code>
    ),
  },
  {
    accessorKey: 'serviceName',
    header: 'Service Name',
    cell: ({ row }) => (
      <span className="font-medium text-sm">{row.original.serviceName}</span>
    ),
  },
  {
    accessorKey: 'memberClass',
    header: 'Member Class',
    cell: ({ row }) => <MemberClassBadge memberClass={row.original.memberClass} />,
  },
  {
    accessorKey: 'memberCode',
    header: 'Member Code',
    cell: ({ row }) => (
      <span className="text-sm text-muted-foreground">{row.original.memberCode}</span>
    ),
  },
  {
    accessorKey: 'subsystemCode',
    header: 'Subsystem',
    cell: ({ row }) => (
      <code className="rounded bg-muted px-1.5 py-0.5 text-xs">{row.original.subsystemCode}</code>
    ),
  },
  {
    accessorKey: 'enabled',
    header: 'Enabled',
    cell: ({ row }) => (
      <StatusBadge status={row.original.enabled ? 'ACTIVE' : 'INACTIVE'} />
    ),
  },
  {
    accessorKey: 'createdAt',
    header: 'Created',
    cell: ({ row }) => (
      <span className="text-sm text-muted-foreground">{formatDateTime(row.original.createdAt)}</span>
    ),
  },
];

export default function XRoadServicesPage() {
  const [pagination, setPagination] = useState({ pageIndex: 0, pageSize: 10 });

  const { data, isLoading } = useXRoadServices({
    page: pagination.pageIndex,
    size: pagination.pageSize,
    sort: 'serviceCode',
    direction: 'asc',
  });

  return (
    <div className="space-y-6">
      <PageHeader
        title="X-Road Services"
        description="Manage X-Road service registrations for CamDX interoperability"
        actions={
          <div className="flex items-center gap-2">
            <Link
              href="/xroad/acls"
              className="inline-flex items-center gap-2 rounded-md border px-4 py-2 text-sm font-medium hover:bg-accent"
            >
              <Shield className="h-4 w-4" /> Access Control Lists
            </Link>
            <Link
              href="/xroad/services/new"
              className="inline-flex items-center gap-2 rounded-md bg-primary px-4 py-2 text-sm font-medium text-primary-foreground hover:bg-primary/90"
            >
              <Plus className="h-4 w-4" /> Register Service
            </Link>
          </div>
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
