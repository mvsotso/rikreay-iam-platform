import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { api, buildPageParams } from '@/lib/api-client';
import type { Page, PageParams } from '@/types/api';
import type { XRoadService, XRoadAcl } from '@/types/admin';

export const xroadKeys = {
  services: ['xroad-services'] as const,
  serviceList: (params: PageParams & { memberClass?: string }) => ['xroad-services', 'list', params] as const,
  acls: ['xroad-acls'] as const,
  aclList: (params: PageParams & { serviceId?: string }) => ['xroad-acls', 'list', params] as const,
};

export function useXRoadServices(params: PageParams & { memberClass?: string } = {}) {
  const { memberClass, ...pageParams } = params;
  let queryStr = buildPageParams(pageParams);
  if (memberClass) queryStr += `&memberClass=${memberClass}`;
  return useQuery({
    queryKey: xroadKeys.serviceList(params),
    queryFn: () => api.get<Page<XRoadService>>(`/api/v1/xroad/services?${queryStr}`),
  });
}

export function useCreateXRoadService() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (data: Partial<XRoadService>) => api.post<XRoadService>('/api/v1/xroad/services', data),
    onSuccess: () => { queryClient.invalidateQueries({ queryKey: xroadKeys.services }); },
  });
}

export function useXRoadAcls(params: PageParams & { serviceId?: string } = {}) {
  const { serviceId, ...pageParams } = params;
  let queryStr = buildPageParams(pageParams);
  if (serviceId) queryStr += `&serviceId=${serviceId}`;
  return useQuery({
    queryKey: xroadKeys.aclList(params),
    queryFn: () => api.get<Page<XRoadAcl>>(`/api/v1/xroad/acls?${queryStr}`),
  });
}

export function useCreateXRoadAcl() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (data: Partial<XRoadAcl>) => api.post<XRoadAcl>('/api/v1/xroad/acls', data),
    onSuccess: () => { queryClient.invalidateQueries({ queryKey: xroadKeys.acls }); },
  });
}

export function useDeleteXRoadAcl() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (id: string) => api.delete<void>(`/api/v1/xroad/acls/${id}`),
    onSuccess: () => { queryClient.invalidateQueries({ queryKey: xroadKeys.acls }); },
  });
}
