import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { api, buildPageParams } from '@/lib/api-client';
import type { Page, PageParams } from '@/types/api';
import type { FeatureFlag, PlatformSettings } from '@/types/admin';

export const configKeys = {
  flags: ['feature-flags'] as const,
  flagList: (params: PageParams) => ['feature-flags', 'list', params] as const,
  settings: ['platform-settings'] as const,
  settingList: (params: PageParams & { category?: string }) => ['platform-settings', 'list', params] as const,
};

export function useFeatureFlags(params: PageParams = {}) {
  return useQuery({
    queryKey: configKeys.flagList(params),
    queryFn: () => api.get<Page<FeatureFlag>>(`/api/v1/config/flags?${buildPageParams(params)}`),
  });
}

export function useToggleFlag() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, enabled }: { id: string; enabled: boolean }) =>
      api.patch<FeatureFlag>(`/api/v1/config/flags/${id}`, { enabled }),
    onSuccess: () => { queryClient.invalidateQueries({ queryKey: configKeys.flags }); },
  });
}

export function useCreateFlag() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (data: Partial<FeatureFlag>) => api.post<FeatureFlag>('/api/v1/config/flags', data),
    onSuccess: () => { queryClient.invalidateQueries({ queryKey: configKeys.flags }); },
  });
}

export function usePlatformSettings(params: PageParams & { category?: string } = {}) {
  const { category, ...pageParams } = params;
  let queryStr = buildPageParams(pageParams);
  if (category) queryStr += `&category=${category}`;
  return useQuery({
    queryKey: configKeys.settingList(params),
    queryFn: () => api.get<Page<PlatformSettings>>(`/api/v1/config/settings?${queryStr}`),
  });
}

export function useUpdateSetting() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, value }: { id: string; value: string }) =>
      api.patch<PlatformSettings>(`/api/v1/config/settings/${id}`, { value }),
    onSuccess: () => { queryClient.invalidateQueries({ queryKey: configKeys.settings }); },
  });
}
