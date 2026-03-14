'use client';

import { useState } from 'react';
import Link from 'next/link';
import { ColumnDef } from '@tanstack/react-table';
import { useFeatureFlags, useToggleFlag } from '@/services/config';
import { FeatureFlag } from '@/types/admin';
import { DataTable } from '@/components/data-table/data-table';
import { PageHeader } from '@/components/layout/page-header';
import { formatDateTime } from '@/lib/utils';
import { Plus, Settings } from 'lucide-react';
import { toast } from 'sonner';

function ToggleFlagSwitch({ flag }: { flag: FeatureFlag }) {
  const toggleFlag = useToggleFlag();

  const handleToggle = async () => {
    try {
      await toggleFlag.mutateAsync({ id: flag.id, enabled: !flag.enabled });
      toast.success(`Flag "${flag.key}" ${!flag.enabled ? 'enabled' : 'disabled'}`);
    } catch {
      toast.error('Failed to toggle flag');
    }
  };

  return (
    <button
      onClick={handleToggle}
      disabled={toggleFlag.isPending}
      className={`relative inline-flex h-5 w-9 items-center rounded-full transition-colors disabled:opacity-50 ${
        flag.enabled ? 'bg-primary' : 'bg-muted-foreground/30'
      }`}
    >
      <span
        className={`inline-block h-3.5 w-3.5 transform rounded-full bg-white transition-transform ${
          flag.enabled ? 'translate-x-4.5' : 'translate-x-0.5'
        }`}
      />
    </button>
  );
}

const columns: ColumnDef<FeatureFlag, unknown>[] = [
  {
    accessorKey: 'key',
    header: 'Key',
    cell: ({ row }) => (
      <code className="rounded bg-muted px-1.5 py-0.5 text-xs">{row.original.key}</code>
    ),
  },
  {
    accessorKey: 'name',
    header: 'Name',
    cell: ({ row }) => (
      <span className="font-medium text-sm">{row.original.name}</span>
    ),
  },
  {
    accessorKey: 'description',
    header: 'Description',
    cell: ({ row }) => (
      <span className="text-sm text-muted-foreground">{row.original.description || '-'}</span>
    ),
  },
  {
    accessorKey: 'enabled',
    header: 'Enabled',
    cell: ({ row }) => <ToggleFlagSwitch flag={row.original} />,
  },
  {
    accessorKey: 'updatedAt',
    header: 'Updated',
    cell: ({ row }) => (
      <span className="text-sm text-muted-foreground">{formatDateTime(row.original.updatedAt)}</span>
    ),
  },
];

export default function FeatureFlagsPage() {
  const [pagination, setPagination] = useState({ pageIndex: 0, pageSize: 10 });

  const { data, isLoading } = useFeatureFlags({
    page: pagination.pageIndex,
    size: pagination.pageSize,
    sort: 'key',
    direction: 'asc',
  });

  return (
    <div className="space-y-6">
      <PageHeader
        title="Feature Flags"
        description="Manage platform feature flags and configuration"
        actions={
          <div className="flex items-center gap-2">
            <Link
              href="/config/settings"
              className="inline-flex items-center gap-2 rounded-md border px-4 py-2 text-sm font-medium hover:bg-accent"
            >
              <Settings className="h-4 w-4" /> Platform Settings
            </Link>
            <Link
              href="/config/new"
              className="inline-flex items-center gap-2 rounded-md bg-primary px-4 py-2 text-sm font-medium text-primary-foreground hover:bg-primary/90"
            >
              <Plus className="h-4 w-4" /> New Flag
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
