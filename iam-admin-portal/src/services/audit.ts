import { useQuery } from '@tanstack/react-query';
import { api, buildPageParams } from '@/lib/api-client';
import type { Page, PageParams } from '@/types/api';
import type { AuditEvent, AuditStats } from '@/types/admin';

export const auditKeys = {
  events: ['audit-events'] as const,
  eventList: (params: PageParams & { eventType?: string; username?: string; from?: string; to?: string }) => ['audit-events', 'list', params] as const,
  stats: (period?: string) => ['audit-stats', period] as const,
  loginHistory: (params: PageParams & { username?: string }) => ['audit-login-history', params] as const,
};

export function useAuditEvents(params: PageParams & { eventType?: string; username?: string; from?: string; to?: string } = {}) {
  const { eventType, username, from, to, ...pageParams } = params;
  let queryStr = buildPageParams(pageParams);
  if (eventType) queryStr += `&eventType=${eventType}`;
  if (username) queryStr += `&username=${encodeURIComponent(username)}`;
  if (from) queryStr += `&from=${from}`;
  if (to) queryStr += `&to=${to}`;
  return useQuery({
    queryKey: auditKeys.eventList(params),
    queryFn: () => api.get<Page<AuditEvent>>(`/api/v1/audit/events?${queryStr}`),
  });
}

export function useAuditStats(period: string = '7d') {
  return useQuery({
    queryKey: auditKeys.stats(period),
    queryFn: () => api.get<AuditStats>(`/api/v1/audit/stats?period=${period}`),
  });
}

export function useLoginHistory(params: PageParams & { username?: string } = {}) {
  const { username, ...pageParams } = params;
  const queryStr = buildPageParams(pageParams) + (username ? `&username=${encodeURIComponent(username)}` : '');
  return useQuery({
    queryKey: auditKeys.loginHistory(params),
    queryFn: () => api.get<Page<AuditEvent>>(`/api/v1/audit/login-history?${queryStr}`),
  });
}
