'use client';

import { useState, useRef } from 'react';
import { ColumnDef } from '@tanstack/react-table';
import { useUsers, useToggleUser, useDeleteUser, useBulkImportUsers } from '@/services/admin';
import { User } from '@/types/admin';
import { DataTable } from '@/components/data-table/data-table';
import { PageHeader } from '@/components/layout/page-header';
import { SearchInput } from '@/components/shared/search-input';
import { useDebounce } from '@/hooks/use-debounce';
import { formatDateTime } from '@/lib/utils';
import { Upload, Download, ToggleLeft, ToggleRight, Trash2 } from 'lucide-react';
import { toast } from 'sonner';

export default function UsersPage() {
  const [search, setSearch] = useState('');
  const debouncedSearch = useDebounce(search, 300);
  const [pagination, setPagination] = useState({ pageIndex: 0, pageSize: 10 });
  const fileInputRef = useRef<HTMLInputElement>(null);

  const { data, isLoading } = useUsers({
    page: pagination.pageIndex,
    size: pagination.pageSize,
    search: debouncedSearch || undefined,
    sort: 'createdTimestamp',
    direction: 'desc',
  });

  const toggleUser = useToggleUser();
  const deleteUser = useDeleteUser();
  const bulkImport = useBulkImportUsers();

  const handleToggle = async (user: User) => {
    try {
      await toggleUser.mutateAsync({ id: user.id, enabled: !user.enabled });
      toast.success(`User ${user.enabled ? 'disabled' : 'enabled'}`);
    } catch {
      toast.error('Failed to toggle user');
    }
  };

  const handleDelete = async (id: string) => {
    if (!confirm('Are you sure you want to delete this user?')) return;
    try {
      await deleteUser.mutateAsync(id);
      toast.success('User deleted');
    } catch {
      toast.error('Failed to delete user');
    }
  };

  const handleBulkImport = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;
    try {
      const result = await bulkImport.mutateAsync(file);
      toast.success(`Imported ${result.imported} users, ${result.failed} failed`);
    } catch {
      toast.error('Bulk import failed');
    }
    if (fileInputRef.current) fileInputRef.current.value = '';
  };

  const columns: ColumnDef<User, unknown>[] = [
    {
      accessorKey: 'username',
      header: 'Username',
      cell: ({ row }) => <span className="font-medium">{row.original.username}</span>,
    },
    {
      accessorKey: 'email',
      header: 'Email',
    },
    {
      id: 'name',
      header: 'Name',
      cell: ({ row }) => [row.original.firstName, row.original.lastName].filter(Boolean).join(' ') || '—',
    },
    {
      accessorKey: 'realmRoles',
      header: 'Roles',
      cell: ({ row }) => (
        <div className="flex flex-wrap gap-1">
          {row.original.realmRoles.slice(0, 3).map((role) => (
            <span key={role} className="inline-flex rounded-full bg-primary/10 px-2 py-0.5 text-xs text-primary">{role}</span>
          ))}
          {row.original.realmRoles.length > 3 && <span className="text-xs text-muted-foreground">+{row.original.realmRoles.length - 3}</span>}
        </div>
      ),
    },
    {
      accessorKey: 'enabled',
      header: 'Status',
      cell: ({ row }) => (
        <span className={`inline-flex rounded-full px-2 py-0.5 text-xs font-medium ${row.original.enabled ? 'bg-green-100 text-green-700 dark:bg-green-900/30 dark:text-green-400' : 'bg-red-100 text-red-700 dark:bg-red-900/30 dark:text-red-400'}`}>
          {row.original.enabled ? 'Enabled' : 'Disabled'}
        </span>
      ),
    },
    {
      accessorKey: 'createdTimestamp',
      header: 'Created',
      cell: ({ row }) => row.original.createdTimestamp ? formatDateTime(new Date(row.original.createdTimestamp).toISOString()) : '—',
    },
    {
      id: 'actions',
      header: '',
      cell: ({ row }) => (
        <div className="flex items-center gap-1">
          <button onClick={() => handleToggle(row.original)} className="rounded p-1 hover:bg-accent" title={row.original.enabled ? 'Disable' : 'Enable'}>
            {row.original.enabled ? <ToggleRight className="h-4 w-4 text-green-600" /> : <ToggleLeft className="h-4 w-4 text-muted-foreground" />}
          </button>
          <button onClick={() => handleDelete(row.original.id)} className="rounded p-1 hover:bg-accent text-destructive" title="Delete">
            <Trash2 className="h-4 w-4" />
          </button>
        </div>
      ),
    },
  ];

  return (
    <div className="space-y-6">
      <PageHeader
        title="User Management"
        description="Manage platform users and their roles"
        actions={
          <div className="flex gap-2">
            <input ref={fileInputRef} type="file" accept=".csv" onChange={handleBulkImport} className="hidden" />
            <button onClick={() => fileInputRef.current?.click()} className="inline-flex items-center gap-2 rounded-md border px-3 py-2 text-sm hover:bg-accent">
              <Upload className="h-4 w-4" /> Import CSV
            </button>
            <button className="inline-flex items-center gap-2 rounded-md border px-3 py-2 text-sm hover:bg-accent">
              <Download className="h-4 w-4" /> Export
            </button>
          </div>
        }
      />

      <div className="flex items-center gap-4">
        <SearchInput value={search} onChange={setSearch} placeholder="Search by username, email, or name..." />
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
