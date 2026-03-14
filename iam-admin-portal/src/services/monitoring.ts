import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { api, buildPageParams } from '@/lib/api-client';
import type { Page, PageParams } from '@/types/api';
import type { Incident, Alert } from '@/types/admin';

export const monitoringKeys = {
  incidents: ['incidents'] as const,
  incidentList: (params: PageParams & { status?: string; severity?: string }) => ['incidents', 'list', params] as const,
  incidentDetail: (id: string) => ['incidents', 'detail', id] as const,
  alerts: ['alerts'] as const,
  alertList: (params: PageParams & { status?: string }) => ['alerts', 'list', params] as const,
};

export function useIncidents(params: PageParams & { status?: string; severity?: string } = {}) {
  const { status, severity, ...pageParams } = params;
  let queryStr = buildPageParams(pageParams);
  if (status) queryStr += `&status=${status}`;
  if (severity) queryStr += `&severity=${severity}`;
  return useQuery({
    queryKey: monitoringKeys.incidentList(params),
    queryFn: () => api.get<Page<Incident>>(`/api/v1/monitoring/incidents?${queryStr}`),
  });
}

export function useIncident(id: string) {
  return useQuery({
    queryKey: monitoringKeys.incidentDetail(id),
    queryFn: () => api.get<Incident>(`/api/v1/monitoring/incidents/${id}`),
    enabled: !!id,
  });
}

export function useCreateIncident() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (data: Partial<Incident>) => api.post<Incident>('/api/v1/monitoring/incidents', data),
    onSuccess: () => { queryClient.invalidateQueries({ queryKey: monitoringKeys.incidents }); },
  });
}

export function useUpdateIncident(id: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (data: Partial<Incident>) => api.patch<Incident>(`/api/v1/monitoring/incidents/${id}`, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: monitoringKeys.incidents });
      queryClient.invalidateQueries({ queryKey: monitoringKeys.incidentDetail(id) });
    },
  });
}

export function useAlerts(params: PageParams & { status?: string } = {}) {
  const { status, ...pageParams } = params;
  let queryStr = buildPageParams(pageParams);
  if (status) queryStr += `&status=${status}`;
  return useQuery({
    queryKey: monitoringKeys.alertList(params),
    queryFn: () => api.get<Page<Alert>>(`/api/v1/monitoring/alerts?${queryStr}`),
  });
}

export function useAcknowledgeAlert() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (id: string) => api.patch<Alert>(`/api/v1/monitoring/alerts/${id}/acknowledge`, {}),
    onSuccess: () => { queryClient.invalidateQueries({ queryKey: monitoringKeys.alerts }); },
  });
}

export function useResolveAlert() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (id: string) => api.patch<Alert>(`/api/v1/monitoring/alerts/${id}/resolve`, {}),
    onSuccess: () => { queryClient.invalidateQueries({ queryKey: monitoringKeys.alerts }); },
  });
}
