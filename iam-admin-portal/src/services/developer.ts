import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { api, buildPageParams } from '@/lib/api-client';
import type { Page, PageParams } from '@/types/api';
import type { DeveloperApp, Webhook } from '@/types/admin';

export const developerKeys = {
  apps: ['developer-apps'] as const,
  appList: (params: PageParams) => ['developer-apps', 'list', params] as const,
  appDetail: (id: string) => ['developer-apps', 'detail', id] as const,
  webhooks: ['webhooks'] as const,
  webhookList: (params: PageParams & { appId?: string }) => ['webhooks', 'list', params] as const,
};

export function useApps(params: PageParams = {}) {
  return useQuery({
    queryKey: developerKeys.appList(params),
    queryFn: () => api.get<Page<DeveloperApp>>(`/api/v1/apps?${buildPageParams(params)}`),
  });
}

export function useApp(id: string) {
  return useQuery({
    queryKey: developerKeys.appDetail(id),
    queryFn: () => api.get<DeveloperApp>(`/api/v1/apps/${id}`),
    enabled: !!id,
  });
}

export function useCreateApp() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (data: Partial<DeveloperApp>) => api.post<DeveloperApp>('/api/v1/apps', data),
    onSuccess: () => { queryClient.invalidateQueries({ queryKey: developerKeys.apps }); },
  });
}

export function useDeleteApp() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (id: string) => api.delete<void>(`/api/v1/apps/${id}`),
    onSuccess: () => { queryClient.invalidateQueries({ queryKey: developerKeys.apps }); },
  });
}

export function useWebhooks(params: PageParams & { appId?: string } = {}) {
  const { appId, ...pageParams } = params;
  let queryStr = buildPageParams(pageParams);
  if (appId) queryStr += `&appId=${appId}`;
  return useQuery({
    queryKey: developerKeys.webhookList(params),
    queryFn: () => api.get<Page<Webhook>>(`/api/v1/webhooks?${queryStr}`),
  });
}

export function useCreateWebhook() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (data: Partial<Webhook>) => api.post<Webhook>('/api/v1/webhooks', data),
    onSuccess: () => { queryClient.invalidateQueries({ queryKey: developerKeys.webhooks }); },
  });
}

export function useTestWebhook() {
  return useMutation({
    mutationFn: (id: string) => api.post<{ success: boolean; statusCode: number }>(`/api/v1/webhooks/${id}/test`, {}),
  });
}
