'use client';

import { useState } from 'react';
import Link from 'next/link';
import { ColumnDef } from '@tanstack/react-table';
import { usePlatformSettings, useUpdateSetting } from '@/services/config';
import { PlatformSettings } from '@/types/admin';
import { DataTable } from '@/components/data-table/data-table';
import { PageHeader } from '@/components/layout/page-header';
import { formatDateTime } from '@/lib/utils';
import { ArrowLeft, Check, X, Pencil } from 'lucide-react';
import { toast } from 'sonner';

function EditableValueCell({ setting }: { setting: PlatformSettings }) {
  const [editing, setEditing] = useState(false);
  const [value, setValue] = useState(setting.value);
  const updateSetting = useUpdateSetting();

  const handleSave = async () => {
    try {
      await updateSetting.mutateAsync({ id: setting.id, value });
      toast.success(`Setting "${setting.key}" updated`);
      setEditing(false);
    } catch {
      toast.error('Failed to update setting');
    }
  };

  const handleCancel = () => {
    setValue(setting.value);
    setEditing(false);
  };

  if (editing) {
    return (
      <div className="flex items-center gap-1">
        <input
          value={value}
          onChange={(e) => setValue(e.target.value)}
          className="w-full rounded-md border bg-background px-2 py-1 text-sm"
          autoFocus
          onKeyDown={(e) => {
            if (e.key === 'Enter') handleSave();
            if (e.key === 'Escape') handleCancel();
          }}
        />
        <button
          onClick={handleSave}
          disabled={updateSetting.isPending}
          className="rounded p-1 text-green-600 hover:bg-green-100 dark:hover:bg-green-900/30 disabled:opacity-50"
        >
          <Check className="h-3.5 w-3.5" />
        </button>
        <button
          onClick={handleCancel}
          className="rounded p-1 text-destructive hover:bg-destructive/10"
        >
          <X className="h-3.5 w-3.5" />
        </button>
      </div>
    );
  }

  return (
    <div className="flex items-center gap-1 group">
      <code className="rounded bg-muted px-1.5 py-0.5 text-xs">{setting.value}</code>
      <button
        onClick={() => setEditing(true)}
        className="rounded p-1 opacity-0 group-hover:opacity-100 hover:bg-accent transition-opacity"
      >
        <Pencil className="h-3 w-3 text-muted-foreground" />
      </button>
    </div>
  );
}

const columns: ColumnDef<PlatformSettings, unknown>[] = [
  {
    accessorKey: 'key',
    header: 'Key',
    cell: ({ row }) => (
      <code className="rounded bg-muted px-1.5 py-0.5 text-xs">{row.original.key}</code>
    ),
  },
  {
    accessorKey: 'value',
    header: 'Value',
    cell: ({ row }) => <EditableValueCell setting={row.original} />,
  },
  {
    accessorKey: 'category',
    header: 'Category',
    cell: ({ row }) => (
      <span className="inline-flex items-center rounded-full bg-muted px-2.5 py-0.5 text-xs font-medium">
        {row.original.category}
      </span>
    ),
  },
  {
    accessorKey: 'updatedAt',
    header: 'Updated',
    cell: ({ row }) => (
      <span className="text-sm text-muted-foreground">{formatDateTime(row.original.updatedAt)}</span>
    ),
  },
  {
    accessorKey: 'updatedBy',
    header: 'Updated By',
    cell: ({ row }) => (
      <span className="text-sm text-muted-foreground">{row.original.updatedBy}</span>
    ),
  },
];

export default function PlatformSettingsPage() {
  const [pagination, setPagination] = useState({ pageIndex: 0, pageSize: 20 });

  const { data, isLoading } = usePlatformSettings({
    page: pagination.pageIndex,
    size: pagination.pageSize,
    sort: 'category',
    direction: 'asc',
  });

  return (
    <div className="space-y-6">
      <PageHeader
        title="Platform Settings"
        description="View and edit platform configuration settings"
        actions={
          <Link
            href="/config"
            className="inline-flex items-center gap-2 rounded-md border px-3 py-2 text-sm hover:bg-accent"
          >
            <ArrowLeft className="h-4 w-4" /> Back to Flags
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
