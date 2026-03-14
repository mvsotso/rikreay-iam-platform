'use client';

import { useState } from 'react';
import Link from 'next/link';
import { ColumnDef } from '@tanstack/react-table';
import { useTenants } from '@/services/admin';
import { Tenant } from '@/types/admin';
import { DataTable } from '@/components/data-table/data-table';
import { PageHeader } from '@/components/layout/page-header';
import { SearchInput } from '@/components/shared/search-input';
import { MemberClassBadge } from '@/components/shared/member-class-badge';
import { useDebounce } from '@/hooks/use-debounce';
import { formatDate } from '@/lib/utils';

const columns: ColumnDef<Tenant, unknown>[] = [
  {
    accessorKey: 'name',
    header: 'Organization',
    cell: ({ row }) => (
      <Link
        href={`/tenants/${row.original.id}`}
        className="text-primary hover:underline font-medium"
      >
        {row.original.name}
      </Link>
    ),
  },
  {
    accessorKey: 'realmName',
    header: 'Realm',
    cell: ({ row }) => (
      <span className="font-mono text-xs">{row.original.realmName}</span>
    ),
  },
  {
    accessorKey: 'memberClass',
    header: 'Sector',
    cell: ({ row }) => (
      <MemberClassBadge memberClass={row.original.memberClass} />
    ),
  },
  {
    accessorKey: 'adminEmail',
    header: 'Admin Contact',
    cell: ({ row }) => (
      <span className="text-sm">{row.original.adminEmail}</span>
    ),
  },
  {
    id: 'users',
    header: 'Users',
    cell: ({ row }) => (
      <span className="text-sm">
        {row.original.currentUsers}/{row.original.maxUsers}
      </span>
    ),
  },
  {
    accessorKey: 'enabled',
    header: 'Status',
    cell: ({ row }) => (
      <span
        className={`inline-flex rounded-full px-2 py-0.5 text-xs font-medium ${
          row.original.enabled
            ? 'bg-green-100 text-green-700 dark:bg-green-900/30 dark:text-green-400'
            : 'bg-red-100 text-red-700 dark:bg-red-900/30 dark:text-red-400'
        }`}
      >
        {row.original.enabled ? 'Active' : 'Inactive'}
      </span>
    ),
  },
  {
    accessorKey: 'createdAt',
    header: 'Created',
    cell: ({ row }) => formatDate(row.original.createdAt),
  },
];

export default function SectorOrganizationsPage() {
  const [search, setSearch] = useState('');
  const [memberClass, setMemberClass] = useState('');
  const debouncedSearch = useDebounce(search, 300);
  const [pagination, setPagination] = useState({ pageIndex: 0, pageSize: 10 });

  const { data, isLoading } = useTenants({
    page: pagination.pageIndex,
    size: pagination.pageSize,
    search: debouncedSearch || undefined,
    memberClass: memberClass || undefined,
    sort: 'createdAt',
    direction: 'desc',
  });

  return (
    <div className="space-y-6">
      <PageHeader
        title="Sector Organizations"
        description="Organizations under your sector oversight"
      />

      <div className="flex items-center gap-4">
        <SearchInput
          value={search}
          onChange={setSearch}
          placeholder="Search organizations..."
        />
        <select
          value={memberClass}
          onChange={(e) => setMemberClass(e.target.value)}
          className="rounded-md border bg-background px-3 py-2 text-sm"
        >
          <option value="">All Sectors</option>
          <option value="GOV">Government (GOV)</option>
          <option value="COM">Commercial (COM)</option>
          <option value="NGO">NGO</option>
          <option value="MUN">Municipal (MUN)</option>
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
