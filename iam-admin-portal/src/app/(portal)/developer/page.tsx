'use client';

import { useState } from 'react';
import Link from 'next/link';
import { ColumnDef } from '@tanstack/react-table';
import { useApps, useDeleteApp } from '@/services/developer';
import { DeveloperApp } from '@/types/admin';
import { DataTable } from '@/components/data-table/data-table';
import { PageHeader } from '@/components/layout/page-header';
import { StatusBadge } from '@/components/shared/status-badge';
import { formatDateTime } from '@/lib/utils';
import { Plus, Trash2 } from 'lucide-react';
import { toast } from 'sonner';

function DeleteAppButton({ appId }: { appId: string }) {
  const deleteApp = useDeleteApp();

  const handleDelete = async () => {
    if (!confirm('Are you sure you want to delete this app?')) return;
    try {
      await deleteApp.mutateAsync(appId);
      toast.success('App deleted successfully');
    } catch {
      toast.error('Failed to delete app');
    }
  };

  return (
    <button
      onClick={handleDelete}
      disabled={deleteApp.isPending}
      className="inline-flex items-center gap-1 rounded-md px-2 py-1 text-xs text-destructive hover:bg-destructive/10 disabled:opacity-50"
    >
      <Trash2 className="h-3 w-3" />
      Delete
    </button>
  );
}

const columns: ColumnDef<DeveloperApp, unknown>[] = [
  {
    accessorKey: 'name',
    header: 'Name',
    cell: ({ row }) => (
      <span className="font-medium text-sm">{row.original.name}</span>
    ),
  },
  {
    accessorKey: 'clientId',
    header: 'Client ID',
    cell: ({ row }) => (
      <code className="rounded bg-muted px-1.5 py-0.5 text-xs">{row.original.clientId}</code>
    ),
  },
  {
    accessorKey: 'status',
    header: 'Status',
    cell: ({ row }) => <StatusBadge status={row.original.status} />,
  },
  {
    accessorKey: 'createdBy',
    header: 'Created By',
    cell: ({ row }) => (
      <span className="text-sm text-muted-foreground">{row.original.createdBy}</span>
    ),
  },
  {
    accessorKey: 'createdAt',
    header: 'Created',
    cell: ({ row }) => (
      <span className="text-sm text-muted-foreground">{formatDateTime(row.original.createdAt)}</span>
    ),
  },
  {
    id: 'actions',
    header: '',
    cell: ({ row }) => <DeleteAppButton appId={row.original.id} />,
  },
];

export default function DeveloperAppsPage() {
  const [pagination, setPagination] = useState({ pageIndex: 0, pageSize: 10 });

  const { data, isLoading } = useApps({
    page: pagination.pageIndex,
    size: pagination.pageSize,
    sort: 'createdAt',
    direction: 'desc',
  });

  return (
    <div className="space-y-6">
      <PageHeader
        title="Developer Apps"
        description="Manage registered applications and API clients"
        actions={
          <Link
            href="/developer/apps/new"
            className="inline-flex items-center gap-2 rounded-md bg-primary px-4 py-2 text-sm font-medium text-primary-foreground hover:bg-primary/90"
          >
            <Plus className="h-4 w-4" /> Register App
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
