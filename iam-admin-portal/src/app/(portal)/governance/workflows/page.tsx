'use client';

import { useState } from 'react';
import { ColumnDef } from '@tanstack/react-table';
import { useWorkflows, useApproveWorkflow, useRejectWorkflow } from '@/services/governance';
import { ApprovalWorkflow } from '@/types/admin';
import { DataTable } from '@/components/data-table/data-table';
import { PageHeader } from '@/components/layout/page-header';
import { StatusBadge } from '@/components/shared/status-badge';
import { formatDateTime } from '@/lib/utils';
import { CheckCircle, XCircle } from 'lucide-react';
import { toast } from 'sonner';

function WorkflowActions({ workflow }: { workflow: ApprovalWorkflow }) {
  const approveMutation = useApproveWorkflow();
  const rejectMutation = useRejectWorkflow();

  const handleApprove = async () => {
    try {
      await approveMutation.mutateAsync(workflow.id);
      toast.success('Workflow approved');
    } catch {
      toast.error('Failed to approve workflow');
    }
  };

  const handleReject = async () => {
    try {
      await rejectMutation.mutateAsync(workflow.id);
      toast.success('Workflow rejected');
    } catch {
      toast.error('Failed to reject workflow');
    }
  };

  if (workflow.status !== 'PENDING') {
    return (
      <span className="text-xs text-muted-foreground">
        {workflow.status === 'APPROVED' ? 'Approved' : workflow.status === 'REJECTED' ? 'Rejected' : 'Expired'}
      </span>
    );
  }

  return (
    <div className="flex items-center gap-2">
      <button
        onClick={handleApprove}
        disabled={approveMutation.isPending}
        className="inline-flex items-center gap-1 rounded-md bg-green-600 px-2 py-1 text-xs font-medium text-white hover:bg-green-700 disabled:opacity-50"
        title="Approve"
      >
        <CheckCircle className="h-3 w-3" /> Approve
      </button>
      <button
        onClick={handleReject}
        disabled={rejectMutation.isPending}
        className="inline-flex items-center gap-1 rounded-md bg-destructive px-2 py-1 text-xs font-medium text-white hover:bg-destructive/90 disabled:opacity-50"
        title="Reject"
      >
        <XCircle className="h-3 w-3" /> Reject
      </button>
    </div>
  );
}

const columns: ColumnDef<ApprovalWorkflow, unknown>[] = [
  {
    accessorKey: 'requestType',
    header: 'Request Type',
    cell: ({ row }) => (
      <span className="font-medium text-sm">{row.original.requestType}</span>
    ),
  },
  {
    accessorKey: 'requesterName',
    header: 'Requester',
    cell: ({ row }) => <span className="text-sm">{row.original.requesterName}</span>,
  },
  {
    accessorKey: 'status',
    header: 'Status',
    cell: ({ row }) => <StatusBadge status={row.original.status} />,
  },
  {
    id: 'approvers',
    header: 'Approvers',
    cell: ({ row }) => {
      const { approvers } = row.original;
      const decided = approvers.filter((a) => a.decision);
      return (
        <span className="text-sm text-muted-foreground">
          {decided.length}/{approvers.length} decided
        </span>
      );
    },
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
    header: 'Actions',
    cell: ({ row }) => <WorkflowActions workflow={row.original} />,
  },
];

const STATUS_OPTIONS = [
  { label: 'All Statuses', value: '' },
  { label: 'Pending', value: 'PENDING' },
  { label: 'Approved', value: 'APPROVED' },
  { label: 'Rejected', value: 'REJECTED' },
  { label: 'Expired', value: 'EXPIRED' },
];

export default function WorkflowsPage() {
  const [pagination, setPagination] = useState({ pageIndex: 0, pageSize: 10 });
  const [statusFilter, setStatusFilter] = useState('');

  const { data, isLoading } = useWorkflows({
    page: pagination.pageIndex,
    size: pagination.pageSize,
    sort: 'createdAt',
    direction: 'desc',
    status: statusFilter || undefined,
  });

  return (
    <div className="space-y-6">
      <PageHeader
        title="Approval Workflows"
        description="Review and process pending approval requests"
      />

      <div className="flex items-center gap-4">
        <select
          value={statusFilter}
          onChange={(e) => {
            setStatusFilter(e.target.value);
            setPagination((prev) => ({ ...prev, pageIndex: 0 }));
          }}
          className="rounded-md border bg-background px-3 py-2 text-sm"
        >
          {STATUS_OPTIONS.map((opt) => (
            <option key={opt.value} value={opt.value}>
              {opt.label}
            </option>
          ))}
        </select>
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
