'use client';

import { useState } from 'react';
import Link from 'next/link';
import { ColumnDef } from '@tanstack/react-table';
import { useNotificationTemplates, useUpdateTemplate } from '@/services/notifications';
import { NotificationTemplate } from '@/types/admin';
import { DataTable } from '@/components/data-table/data-table';
import { PageHeader } from '@/components/layout/page-header';
import { StatusBadge } from '@/components/shared/status-badge';
import { Plus, Pencil } from 'lucide-react';
import { toast } from 'sonner';

export default function NotificationTemplatesPage() {
  const [pagination, setPagination] = useState({ pageIndex: 0, pageSize: 10 });

  const { data, isLoading } = useNotificationTemplates({
    page: pagination.pageIndex,
    size: pagination.pageSize,
    sort: 'name',
    direction: 'asc',
  });

  const columns: ColumnDef<NotificationTemplate, unknown>[] = [
    {
      accessorKey: 'name',
      header: 'Name',
      cell: ({ row }) => (
        <span className="text-sm font-medium">{row.original.name}</span>
      ),
    },
    {
      accessorKey: 'channel',
      header: 'Channel',
      cell: ({ row }) => <span className="text-sm">{row.original.channel}</span>,
    },
    {
      accessorKey: 'subject',
      header: 'Subject',
      cell: ({ row }) => (
        <span className="text-sm truncate max-w-[200px] block">{row.original.subject}</span>
      ),
    },
    {
      accessorKey: 'variables',
      header: 'Variables',
      cell: ({ row }) => (
        <span className="text-sm text-muted-foreground">
          {row.original.variables?.length > 0 ? row.original.variables.join(', ') : '—'}
        </span>
      ),
    },
    {
      accessorKey: 'active',
      header: 'Active',
      cell: ({ row }) => (
        <ActiveToggle template={row.original} />
      ),
    },
    {
      id: 'actions',
      header: 'Actions',
      cell: ({ row }) => (
        <Link
          href={`/notifications/templates/${row.original.id}/edit`}
          className="inline-flex items-center gap-1 rounded-md border px-2 py-1 text-xs hover:bg-accent"
        >
          <Pencil className="h-3 w-3" /> Edit
        </Link>
      ),
    },
  ];

  return (
    <div className="space-y-6">
      <PageHeader
        title="Notification Templates"
        description="Manage email, SMS, and Telegram notification templates"
        actions={
          <Link
            href="/notifications/templates/new"
            className="inline-flex items-center gap-2 rounded-md bg-primary px-4 py-2 text-sm font-medium text-primary-foreground hover:bg-primary/90"
          >
            <Plus className="h-4 w-4" /> New Template
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

function ActiveToggle({ template }: { template: NotificationTemplate }) {
  const updateTemplate = useUpdateTemplate(template.id);

  const handleToggle = async () => {
    try {
      await updateTemplate.mutateAsync({ active: !template.active });
      toast.success(`Template ${template.active ? 'deactivated' : 'activated'}`);
    } catch {
      toast.error('Failed to update template status');
    }
  };

  return (
    <button
      onClick={handleToggle}
      disabled={updateTemplate.isPending}
      className="text-sm"
    >
      <StatusBadge status={template.active ? 'ACTIVE' : 'INACTIVE'} />
    </button>
  );
}
