'use client';

import { useState } from 'react';
import Link from 'next/link';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { ColumnDef } from '@tanstack/react-table';
import { useXRoadAcls, useCreateXRoadAcl, useDeleteXRoadAcl } from '@/services/xroad';
import { XRoadAcl } from '@/types/admin';
import { DataTable } from '@/components/data-table/data-table';
import { PageHeader } from '@/components/layout/page-header';
import { StatusBadge } from '@/components/shared/status-badge';
import { MemberClassBadge } from '@/components/shared/member-class-badge';
import { formatDateTime } from '@/lib/utils';
import { ArrowLeft, Plus, Trash2 } from 'lucide-react';
import { toast } from 'sonner';

function DeleteAclButton({ aclId }: { aclId: string }) {
  const deleteAcl = useDeleteXRoadAcl();

  const handleDelete = async () => {
    if (!confirm('Are you sure you want to delete this ACL entry?')) return;
    try {
      await deleteAcl.mutateAsync(aclId);
      toast.success('ACL entry deleted');
    } catch {
      toast.error('Failed to delete ACL entry');
    }
  };

  return (
    <button
      onClick={handleDelete}
      disabled={deleteAcl.isPending}
      className="inline-flex items-center gap-1 rounded-md px-2 py-1 text-xs text-destructive hover:bg-destructive/10 disabled:opacity-50"
    >
      <Trash2 className="h-3 w-3" />
      Delete
    </button>
  );
}

const columns: ColumnDef<XRoadAcl, unknown>[] = [
  {
    accessorKey: 'serviceId',
    header: 'Service ID',
    cell: ({ row }) => (
      <code className="rounded bg-muted px-1.5 py-0.5 text-xs">{row.original.serviceId}</code>
    ),
  },
  {
    accessorKey: 'clientMemberClass',
    header: 'Client Class',
    cell: ({ row }) => <MemberClassBadge memberClass={row.original.clientMemberClass} />,
  },
  {
    accessorKey: 'clientMemberCode',
    header: 'Client Code',
    cell: ({ row }) => (
      <span className="text-sm font-medium">{row.original.clientMemberCode}</span>
    ),
  },
  {
    accessorKey: 'clientSubsystemCode',
    header: 'Client Subsystem',
    cell: ({ row }) => (
      <span className="text-sm text-muted-foreground">
        {row.original.clientSubsystemCode || '-'}
      </span>
    ),
  },
  {
    accessorKey: 'allowed',
    header: 'Allowed',
    cell: ({ row }) => (
      <StatusBadge status={row.original.allowed ? 'ACTIVE' : 'INACTIVE'} />
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
    cell: ({ row }) => <DeleteAclButton aclId={row.original.id} />,
  },
];

const aclSchema = z.object({
  serviceId: z.string().min(1, 'Service ID is required'),
  clientMemberClass: z.enum(['GOV', 'COM', 'NGO', 'MUN'], {
    required_error: 'Client member class is required',
  }),
  clientMemberCode: z.string().min(1, 'Client member code is required'),
  clientSubsystemCode: z.string().optional(),
});

type AclFormValues = z.infer<typeof aclSchema>;

export default function XRoadAclsPage() {
  const [pagination, setPagination] = useState({ pageIndex: 0, pageSize: 10 });
  const [showForm, setShowForm] = useState(false);

  const { data, isLoading } = useXRoadAcls({
    page: pagination.pageIndex,
    size: pagination.pageSize,
  });

  const createAcl = useCreateXRoadAcl();
  const {
    register,
    handleSubmit,
    reset,
    formState: { errors, isSubmitting },
  } = useForm<AclFormValues>({
    resolver: zodResolver(aclSchema),
  });

  const onSubmit = async (data: AclFormValues) => {
    try {
      await createAcl.mutateAsync({
        ...data,
        clientSubsystemCode: data.clientSubsystemCode || undefined,
        allowed: true,
      });
      toast.success('ACL entry created');
      reset();
      setShowForm(false);
    } catch {
      toast.error('Failed to create ACL entry');
    }
  };

  return (
    <div className="space-y-6">
      <PageHeader
        title="X-Road Access Control Lists"
        description="Manage service access permissions for X-Road clients"
        actions={
          <div className="flex items-center gap-2">
            <Link
              href="/xroad"
              className="inline-flex items-center gap-2 rounded-md border px-3 py-2 text-sm hover:bg-accent"
            >
              <ArrowLeft className="h-4 w-4" /> Back to Services
            </Link>
            <button
              onClick={() => setShowForm(!showForm)}
              className="inline-flex items-center gap-2 rounded-md bg-primary px-4 py-2 text-sm font-medium text-primary-foreground hover:bg-primary/90"
            >
              <Plus className="h-4 w-4" /> New ACL
            </button>
          </div>
        }
      />

      {showForm && (
        <form onSubmit={handleSubmit(onSubmit)} className="rounded-lg border bg-card p-6 space-y-4">
          <h3 className="font-semibold">New ACL Entry</h3>

          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
            <div>
              <label className="text-sm font-medium">Service ID *</label>
              <input
                {...register('serviceId')}
                className="mt-1 w-full rounded-md border bg-background px-3 py-2 text-sm"
                placeholder="Service ID"
              />
              {errors.serviceId && (
                <p className="mt-1 text-xs text-destructive">{errors.serviceId.message}</p>
              )}
            </div>

            <div>
              <label className="text-sm font-medium">Client Class *</label>
              <select
                {...register('clientMemberClass')}
                className="mt-1 w-full rounded-md border bg-background px-3 py-2 text-sm"
              >
                <option value="">Select...</option>
                <option value="GOV">GOV</option>
                <option value="COM">COM</option>
                <option value="NGO">NGO</option>
                <option value="MUN">MUN</option>
              </select>
              {errors.clientMemberClass && (
                <p className="mt-1 text-xs text-destructive">{errors.clientMemberClass.message}</p>
              )}
            </div>

            <div>
              <label className="text-sm font-medium">Client Code *</label>
              <input
                {...register('clientMemberCode')}
                className="mt-1 w-full rounded-md border bg-background px-3 py-2 text-sm"
                placeholder="e.g., GDT"
              />
              {errors.clientMemberCode && (
                <p className="mt-1 text-xs text-destructive">{errors.clientMemberCode.message}</p>
              )}
            </div>

            <div>
              <label className="text-sm font-medium">Client Subsystem</label>
              <input
                {...register('clientSubsystemCode')}
                className="mt-1 w-full rounded-md border bg-background px-3 py-2 text-sm"
                placeholder="Optional"
              />
            </div>
          </div>

          <div className="flex gap-3">
            <button
              type="submit"
              disabled={isSubmitting}
              className="rounded-md bg-primary px-4 py-2 text-sm font-medium text-primary-foreground hover:bg-primary/90 disabled:opacity-50"
            >
              {isSubmitting ? 'Creating...' : 'Create ACL'}
            </button>
            <button
              type="button"
              onClick={() => { reset(); setShowForm(false); }}
              className="rounded-md border px-4 py-2 text-sm hover:bg-accent"
            >
              Cancel
            </button>
          </div>
        </form>
      )}

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
