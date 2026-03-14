import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { api, buildPageParams } from '@/lib/api-client';
import type { Page, PageParams } from '@/types/api';
import type { NotificationLog, NotificationTemplate } from '@/types/admin';

export const notificationKeys = {
  logs: ['notification-logs'] as const,
  logList: (params: PageParams & { channel?: string; status?: string }) => ['notification-logs', 'list', params] as const,
  templates: ['notification-templates'] as const,
  templateList: (params: PageParams) => ['notification-templates', 'list', params] as const,
};

export function useNotificationLogs(params: PageParams & { channel?: string; status?: string } = {}) {
  const { channel, status, ...pageParams } = params;
  let queryStr = buildPageParams(pageParams);
  if (channel) queryStr += `&channel=${channel}`;
  if (status) queryStr += `&status=${status}`;
  return useQuery({
    queryKey: notificationKeys.logList(params),
    queryFn: () => api.get<Page<NotificationLog>>(`/api/v1/notifications/logs?${queryStr}`),
  });
}

export function useNotificationTemplates(params: PageParams = {}) {
  return useQuery({
    queryKey: notificationKeys.templateList(params),
    queryFn: () => api.get<Page<NotificationTemplate>>(`/api/v1/notifications/templates?${buildPageParams(params)}`),
  });
}

export function useCreateTemplate() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (data: Partial<NotificationTemplate>) => api.post<NotificationTemplate>('/api/v1/notifications/templates', data),
    onSuccess: () => { queryClient.invalidateQueries({ queryKey: notificationKeys.templates }); },
  });
}

export function useUpdateTemplate(id: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (data: Partial<NotificationTemplate>) => api.put<NotificationTemplate>(`/api/v1/notifications/templates/${id}`, data),
    onSuccess: () => { queryClient.invalidateQueries({ queryKey: notificationKeys.templates }); },
  });
}
