'use client';

import { useState } from 'react';
import Link from 'next/link';
import { ColumnDef } from '@tanstack/react-table';
import { useWebhooks, useTestWebhook } from '@/services/developer';
import { Webhook } from '@/types/admin';
import { DataTable } from '@/components/data-table/data-table';
import { PageHeader } from '@/components/layout/page-header';
import { StatusBadge } from '@/components/shared/status-badge';
import { formatDateTime } from '@/lib/utils';
import { Plus, Zap } from 'lucide-react';
import { toast } from 'sonner';

function TestWebhookButton({ webhookId }: { webhookId: string }) {
  const testWebhook = useTestWebhook();

  const handleTest = async () => {
    try {
      const result = await testWebhook.mutateAsync(webhookId);
      if (result.success) {
        toast.success(`Webhook test succeeded (${result.statusCode})`);
      } else {
        toast.error(`Webhook test failed (${result.statusCode})`);
      }
    } catch {
      toast.error('Failed to test webhook');
    }
  };

  return (
    <button
      onClick={handleTest}
      disabled={testWebhook.isPending}
      className="inline-flex items-center gap-1 rounded-md px-2 py-1 text-xs text-primary hover:bg-primary/10 disabled:opacity-50"
    >
      <Zap className="h-3 w-3" />
      {testWebhook.isPending ? 'Testing...' : 'Test'}
    </button>
  );
}

const columns: ColumnDef<Webhook, unknown>[] = [
  {
    accessorKey: 'url',
    header: 'URL',
    cell: ({ row }) => (
      <code className="rounded bg-muted px-1.5 py-0.5 text-xs break-all">{row.original.url}</code>
    ),
  },
  {
    accessorKey: 'events',
    header: 'Events',
    cell: ({ row }) => (
      <span className="text-sm text-muted-foreground">{row.original.events.join(', ')}</span>
    ),
  },
  {
    accessorKey: 'active',
    header: 'Active',
    cell: ({ row }) => (
      <StatusBadge status={row.original.active ? 'ACTIVE' : 'INACTIVE'} />
    ),
  },
  {
    accessorKey: 'lastDeliveryStatus',
    header: 'Last Status',
    cell: ({ row }) => (
      <span className="text-sm text-muted-foreground">
        {row.original.lastDeliveryStatus != null ? row.original.lastDeliveryStatus : '-'}
      </span>
    ),
  },
  {
    accessorKey: 'lastDeliveryAt',
    header: 'Last Delivery',
    cell: ({ row }) => (
      <span className="text-sm text-muted-foreground">
        {row.original.lastDeliveryAt ? formatDateTime(row.original.lastDeliveryAt) : '-'}
      </span>
    ),
  },
  {
    id: 'actions',
    header: '',
    cell: ({ row }) => <TestWebhookButton webhookId={row.original.id} />,
  },
];

export default function WebhooksPage() {
  const [pagination, setPagination] = useState({ pageIndex: 0, pageSize: 10 });

  const { data, isLoading } = useWebhooks({
    page: pagination.pageIndex,
    size: pagination.pageSize,
  });

  return (
    <div className="space-y-6">
      <PageHeader
        title="Webhooks"
        description="Manage webhook subscriptions for event notifications"
        actions={
          <Link
            href="/developer/webhooks/new"
            className="inline-flex items-center gap-2 rounded-md bg-primary px-4 py-2 text-sm font-medium text-primary-foreground hover:bg-primary/90"
          >
            <Plus className="h-4 w-4" /> New Webhook
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
