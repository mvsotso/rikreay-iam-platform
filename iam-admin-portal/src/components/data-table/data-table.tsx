"use client";

import {
  type ColumnDef,
  type ColumnFiltersState,
  type SortingState,
  type VisibilityState,
  flexRender,
  getCoreRowModel,
  useReactTable,
} from "@tanstack/react-table";
import { useState } from "react";
import { cn } from "@/lib/utils";

interface DataTableProps<TData, TValue> {
  columns: ColumnDef<TData, TValue>[];
  data: TData[];
  pageCount?: number;
  pageIndex?: number;
  pageSize?: number;
  pagination?: { pageIndex: number; pageSize: number };
  onPaginationChange?:
    | ((page: number, size: number) => void)
    | React.Dispatch<React.SetStateAction<{ pageIndex: number; pageSize: number }>>;
  onSortingChange?: (sorting: SortingState) => void;
  isLoading?: boolean;
}

export function DataTable<TData, TValue>({
  columns,
  data,
  pageCount = 0,
  pageIndex: pageIndexProp,
  pageSize: pageSizeProp,
  pagination: paginationProp,
  onPaginationChange,
  onSortingChange,
  isLoading,
}: DataTableProps<TData, TValue>) {
  const pageIndex = paginationProp?.pageIndex ?? pageIndexProp ?? 0;
  const pageSize = paginationProp?.pageSize ?? pageSizeProp ?? 20;

  const handlePageChange = (newPage: number, newSize: number) => {
    if (!onPaginationChange) return;
    // If it looks like a setState dispatch, call it with an object
    if (onPaginationChange.length <= 1) {
      (onPaginationChange as React.Dispatch<React.SetStateAction<{ pageIndex: number; pageSize: number }>>)(
        { pageIndex: newPage, pageSize: newSize }
      );
    } else {
      (onPaginationChange as (page: number, size: number) => void)(newPage, newSize);
    }
  };

  const [sorting, setSorting] = useState<SortingState>([]);
  const [columnFilters, setColumnFilters] = useState<ColumnFiltersState>([]);
  const [columnVisibility, setColumnVisibility] = useState<VisibilityState>({});

  const table = useReactTable({
    data,
    columns,
    pageCount,
    state: {
      sorting,
      columnFilters,
      columnVisibility,
      pagination: { pageIndex, pageSize },
    },
    onSortingChange: (updater) => {
      const newSorting = typeof updater === "function" ? updater(sorting) : updater;
      setSorting(newSorting);
      onSortingChange?.(newSorting);
    },
    onColumnFiltersChange: setColumnFilters,
    onColumnVisibilityChange: setColumnVisibility,
    getCoreRowModel: getCoreRowModel(),
    manualPagination: true,
    manualSorting: true,
  });

  return (
    <div className="space-y-4">
      <div className="rounded-md border">
        <table className="w-full">
          <thead>
            {table.getHeaderGroups().map((headerGroup) => (
              <tr key={headerGroup.id} className="border-b bg-muted/50">
                {headerGroup.headers.map((header) => (
                  <th
                    key={header.id}
                    className="h-12 px-4 text-left align-middle text-sm font-medium text-muted-foreground"
                  >
                    {header.isPlaceholder
                      ? null
                      : flexRender(
                          header.column.columnDef.header,
                          header.getContext()
                        )}
                  </th>
                ))}
              </tr>
            ))}
          </thead>
          <tbody>
            {isLoading ? (
              <tr>
                <td
                  colSpan={columns.length}
                  className="h-24 text-center text-sm text-muted-foreground"
                >
                  Loading...
                </td>
              </tr>
            ) : table.getRowModel().rows.length === 0 ? (
              <tr>
                <td
                  colSpan={columns.length}
                  className="h-24 text-center text-sm text-muted-foreground"
                >
                  No results found.
                </td>
              </tr>
            ) : (
              table.getRowModel().rows.map((row) => (
                <tr
                  key={row.id}
                  className="border-b transition-colors hover:bg-muted/50"
                >
                  {row.getVisibleCells().map((cell) => (
                    <td key={cell.id} className="px-4 py-3 text-sm">
                      {flexRender(
                        cell.column.columnDef.cell,
                        cell.getContext()
                      )}
                    </td>
                  ))}
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>

      {/* Pagination */}
      {pageCount > 0 && (
        <div className="flex items-center justify-between">
          <p className="text-sm text-muted-foreground">
            Page {pageIndex + 1} of {pageCount}
          </p>
          <div className="flex items-center gap-2">
            <button
              onClick={() => handlePageChange(pageIndex - 1, pageSize)}
              disabled={pageIndex === 0}
              className={cn(
                "rounded-md border px-3 py-1.5 text-sm font-medium transition-colors",
                pageIndex === 0
                  ? "opacity-50 cursor-not-allowed"
                  : "hover:bg-accent"
              )}
            >
              Previous
            </button>
            <button
              onClick={() => handlePageChange(pageIndex + 1, pageSize)}
              disabled={pageIndex >= pageCount - 1}
              className={cn(
                "rounded-md border px-3 py-1.5 text-sm font-medium transition-colors",
                pageIndex >= pageCount - 1
                  ? "opacity-50 cursor-not-allowed"
                  : "hover:bg-accent"
              )}
            >
              Next
            </button>
          </div>
        </div>
      )}
    </div>
  );
}
