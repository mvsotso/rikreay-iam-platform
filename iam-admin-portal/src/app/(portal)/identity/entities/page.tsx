'use client';

import { useState } from 'react';
import Link from 'next/link';
import { ColumnDef } from '@tanstack/react-table';
import { useEntities } from '@/services/identity';
import { LegalEntity } from '@/types/identity';
import { DataTable } from '@/components/data-table/data-table';
import { PageHeader } from '@/components/layout/page-header';
import { SearchInput } from '@/components/shared/search-input';
import { StatusBadge } from '@/components/shared/status-badge';
import { MemberClassBadge } from '@/components/shared/member-class-badge';
import { useDebounce } from '@/hooks/use-debounce';
import { formatDate } from '@/lib/utils';
import { Plus } from 'lucide-react';
import { MemberClass } from '@/types/enums';

const columns: ColumnDef<LegalEntity, unknown>[] = [
  {
    accessorKey: 'registrationNumber',
    header: 'Reg. No.',
    cell: ({ row }) => (
      <Link href={`/identity/entities/${row.original.id}`} className="text-primary hover:underline font-medium">
        {row.original.registrationNumber}
      </Link>
    ),
  },
  {
    accessorKey: 'nameKh',
    header: 'Name (Khmer)',
  },
  {
    accessorKey: 'nameEn',
    header: 'Name (English)',
    cell: ({ row }) => row.original.nameEn ?? '—',
  },
  {
    accessorKey: 'memberClass',
    header: 'Sector',
    cell: ({ row }) => <MemberClassBadge memberClass={row.original.memberClass} />,
  },
  {
    accessorKey: 'entityType',
    header: 'Type',
    cell: ({ row }) => <span className="text-xs">{row.original.entityType.replace(/_/g, ' ')}</span>,
  },
  {
    accessorKey: 'verificationStatus',
    header: 'Status',
    cell: ({ row }) => <StatusBadge status={row.original.verificationStatus} />,
  },
  {
    accessorKey: 'createdAt',
    header: 'Created',
    cell: ({ row }) => formatDate(row.original.createdAt),
  },
];

export default function EntitiesPage() {
  const [search, setSearch] = useState('');
  const [memberClass, setMemberClass] = useState<string>('');
  const debouncedSearch = useDebounce(search, 300);
  const [pagination, setPagination] = useState({ pageIndex: 0, pageSize: 10 });

  const { data, isLoading } = useEntities({
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
        title="Legal Entities"
        description="Manage legal entity identities (នីតិបុគ្គល)"
        actions={
          <Link href="/identity/entities/new" className="inline-flex items-center gap-2 rounded-md bg-primary px-4 py-2 text-sm font-medium text-primary-foreground hover:bg-primary/90">
            <Plus className="h-4 w-4" /> Add Entity
          </Link>
        }
      />

      <div className="flex items-center gap-4">
        <SearchInput value={search} onChange={setSearch} placeholder="Search by name or registration..." />
        <select value={memberClass} onChange={(e) => setMemberClass(e.target.value)} className="rounded-md border bg-background px-3 py-2 text-sm">
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
