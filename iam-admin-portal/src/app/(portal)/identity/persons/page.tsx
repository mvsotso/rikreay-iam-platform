'use client';

import { useState } from 'react';
import Link from 'next/link';
import { ColumnDef } from '@tanstack/react-table';
import { usePersons, useDeletePerson } from '@/services/identity';
import { NaturalPerson } from '@/types/identity';
import { DataTable } from '@/components/data-table/data-table';
import { PageHeader } from '@/components/layout/page-header';
import { SearchInput } from '@/components/shared/search-input';
import { StatusBadge } from '@/components/shared/status-badge';
import { useDebounce } from '@/hooks/use-debounce';
import { formatDate } from '@/lib/utils';
import { Plus, Eye, Pencil, Trash2, UserCheck } from 'lucide-react';
import { toast } from 'sonner';

const columns: ColumnDef<NaturalPerson, unknown>[] = [
  {
    accessorKey: 'personalIdCode',
    header: 'ID Code',
    cell: ({ row }) => (
      <Link href={`/identity/persons/${row.original.id}`} className="text-primary hover:underline font-medium">
        {row.original.personalIdCode}
      </Link>
    ),
  },
  {
    id: 'name',
    header: 'Name (Khmer)',
    cell: ({ row }) => `${row.original.lastNameKh} ${row.original.firstNameKh}`,
  },
  {
    id: 'nameEn',
    header: 'Name (English)',
    cell: ({ row }) => row.original.firstNameEn && row.original.lastNameEn ? `${row.original.firstNameEn} ${row.original.lastNameEn}` : '—',
  },
  {
    accessorKey: 'email',
    header: 'Email',
    cell: ({ row }) => row.original.email ?? '—',
  },
  {
    accessorKey: 'verificationStatus',
    header: 'Status',
    cell: ({ row }) => <StatusBadge status={row.original.verificationStatus} />,
  },
  {
    accessorKey: 'verificationLevel',
    header: 'Level',
    cell: ({ row }) => <span className="text-sm">L{row.original.verificationLevel}</span>,
  },
  {
    accessorKey: 'createdAt',
    header: 'Created',
    cell: ({ row }) => formatDate(row.original.createdAt),
  },
];

export default function PersonsPage() {
  const [search, setSearch] = useState('');
  const debouncedSearch = useDebounce(search, 300);
  const [pagination, setPagination] = useState({ pageIndex: 0, pageSize: 10 });

  const { data, isLoading } = usePersons({
    page: pagination.pageIndex,
    size: pagination.pageSize,
    search: debouncedSearch || undefined,
    sort: 'createdAt',
    direction: 'desc',
  });

  const deletePerson = useDeletePerson();

  return (
    <div className="space-y-6">
      <PageHeader
        title="Natural Persons"
        description="Manage natural person identities (រូបវន្តបុគ្គល)"
        actions={
          <Link href="/identity/persons/new" className="inline-flex items-center gap-2 rounded-md bg-primary px-4 py-2 text-sm font-medium text-primary-foreground hover:bg-primary/90">
            <Plus className="h-4 w-4" /> Add Person
          </Link>
        }
      />

      <div className="flex items-center gap-4">
        <SearchInput value={search} onChange={setSearch} placeholder="Search by name, ID code, or email..." />
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
