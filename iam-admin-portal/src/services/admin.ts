import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { api, buildPageParams } from '@/lib/api-client';
import type { Page, PageParams } from '@/types/api';
import type { User, Tenant, CreateTenantRequest, PlatformStats, SectorStats, OrgStats, ServiceHealth, AuthAnalytics } from '@/types/admin';

export const adminKeys = {
  platformStats: ['admin', 'platform-stats'] as const,
  sectorStats: (memberClass?: string) => ['admin', 'sector-stats', memberClass] as const,
  orgStats: ['admin', 'org-stats'] as const,
  users: ['users'] as const,
  userList: (params: PageParams & { search?: string }) => ['users', 'list', params] as const,
  userDetail: (id: string) => ['users', 'detail', id] as const,
  tenants: ['tenants'] as const,
  tenantList: (params: PageParams & { search?: string; memberClass?: string }) => ['tenants', 'list', params] as const,
  tenantDetail: (id: string) => ['tenants', 'detail', id] as const,
  serviceHealth: ['monitoring', 'health'] as const,
  authAnalytics: (period?: string) => ['monitoring', 'auth-analytics', period] as const,
};

// --- Dashboard ---
export function usePlatformStats() {
  return useQuery({
    queryKey: adminKeys.platformStats,
    queryFn: () => api.get<PlatformStats>('/api/v1/platform-admin/platform/stats'),
    refetchInterval: 30000,
  });
}

export function useSectorStats(memberClass?: string) {
  return useQuery({
    queryKey: adminKeys.sectorStats(memberClass),
    queryFn: () => api.get<SectorStats[]>(`/api/v1/platform-admin/sector/stats${memberClass ? `?memberClass=${memberClass}` : ''}`),
  });
}

export function useOrgStats() {
  return useQuery({
    queryKey: adminKeys.orgStats,
    queryFn: () => api.get<OrgStats>('/api/v1/platform-admin/org/stats'),
  });
}

export function useServiceHealth() {
  return useQuery({
    queryKey: adminKeys.serviceHealth,
    queryFn: () => api.get<ServiceHealth[]>('/api/v1/monitoring/health/services'),
    refetchInterval: 15000,
  });
}

export function useAuthAnalytics(period: string = '7d') {
  return useQuery({
    queryKey: adminKeys.authAnalytics(period),
    queryFn: () => api.get<AuthAnalytics>(`/api/v1/monitoring/auth/analytics?period=${period}`),
  });
}

// --- Users ---
export function useUsers(params: PageParams & { search?: string } = {}) {
  const { search, ...pageParams } = params;
  const queryStr = buildPageParams(pageParams) + (search ? `&search=${encodeURIComponent(search)}` : '');
  return useQuery({
    queryKey: adminKeys.userList(params),
    queryFn: () => api.get<Page<User>>(`/api/v1/users?${queryStr}`),
  });
}

export function useUser(id: string) {
  return useQuery({
    queryKey: adminKeys.userDetail(id),
    queryFn: () => api.get<User>(`/api/v1/users/${id}`),
    enabled: !!id,
  });
}

export function useToggleUser() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, enabled }: { id: string; enabled: boolean }) =>
      api.patch<User>(`/api/v1/users/${id}`, { enabled }),
    onSuccess: () => { queryClient.invalidateQueries({ queryKey: adminKeys.users }); },
  });
}

export function useDeleteUser() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (id: string) => api.delete<void>(`/api/v1/users/${id}`),
    onSuccess: () => { queryClient.invalidateQueries({ queryKey: adminKeys.users }); },
  });
}

export function useBulkImportUsers() {
  return useMutation({
    mutationFn: (file: File) => {
      const formData = new FormData();
      formData.append('file', file);
      return api.post<{ imported: number; failed: number }>('/api/v1/users/bulk-import', formData);
    },
  });
}

// --- Tenants ---
export function useTenants(params: PageParams & { search?: string; memberClass?: string } = {}) {
  const { search, memberClass, ...pageParams } = params;
  let queryStr = buildPageParams(pageParams);
  if (search) queryStr += `&search=${encodeURIComponent(search)}`;
  if (memberClass) queryStr += `&memberClass=${memberClass}`;
  return useQuery({
    queryKey: adminKeys.tenantList(params),
    queryFn: () => api.get<Page<Tenant>>(`/api/v1/tenants?${queryStr}`),
  });
}

export function useTenant(id: string) {
  return useQuery({
    queryKey: adminKeys.tenantDetail(id),
    queryFn: () => api.get<Tenant>(`/api/v1/tenants/${id}`),
    enabled: !!id,
  });
}

export function useCreateTenant() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (data: CreateTenantRequest) => api.post<Tenant>('/api/v1/tenants', data),
    onSuccess: () => { queryClient.invalidateQueries({ queryKey: adminKeys.tenants }); },
  });
}

export function useUpdateTenant(id: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (data: Partial<CreateTenantRequest>) => api.put<Tenant>(`/api/v1/tenants/${id}`, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: adminKeys.tenants });
      queryClient.invalidateQueries({ queryKey: adminKeys.tenantDetail(id) });
    },
  });
}

export function useDeleteTenant() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (id: string) => api.delete<void>(`/api/v1/tenants/${id}`),
    onSuccess: () => { queryClient.invalidateQueries({ queryKey: adminKeys.tenants }); },
  });
}
