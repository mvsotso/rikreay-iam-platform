import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { api, buildPageParams } from '@/lib/api-client';
import type { Page, PageParams } from '@/types/api';
import type { AccessReviewCampaign, ConsentRecord, AccessPolicy, ApprovalWorkflow } from '@/types/admin';

export const governanceKeys = {
  campaigns: ['campaigns'] as const,
  campaignList: (params: PageParams & { status?: string }) => ['campaigns', 'list', params] as const,
  campaignDetail: (id: string) => ['campaigns', 'detail', id] as const,
  consents: ['consents'] as const,
  consentList: (params: PageParams) => ['consents', 'list', params] as const,
  policies: ['policies'] as const,
  policyList: (params: PageParams) => ['policies', 'list', params] as const,
  workflows: ['workflows'] as const,
  workflowList: (params: PageParams & { status?: string }) => ['workflows', 'list', params] as const,
};

export function useCampaigns(params: PageParams & { status?: string } = {}) {
  const { status, ...pageParams } = params;
  let queryStr = buildPageParams(pageParams);
  if (status) queryStr += `&status=${status}`;
  return useQuery({
    queryKey: governanceKeys.campaignList(params),
    queryFn: () => api.get<Page<AccessReviewCampaign>>(`/api/v1/governance/campaigns?${queryStr}`),
  });
}

export function useCampaign(id: string) {
  return useQuery({
    queryKey: governanceKeys.campaignDetail(id),
    queryFn: () => api.get<AccessReviewCampaign>(`/api/v1/governance/campaigns/${id}`),
    enabled: !!id,
  });
}

export function useCreateCampaign() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (data: Partial<AccessReviewCampaign>) => api.post<AccessReviewCampaign>('/api/v1/governance/campaigns', data),
    onSuccess: () => { queryClient.invalidateQueries({ queryKey: governanceKeys.campaigns }); },
  });
}

export function useConsents(params: PageParams = {}) {
  return useQuery({
    queryKey: governanceKeys.consentList(params),
    queryFn: () => api.get<Page<ConsentRecord>>(`/api/v1/governance/consents?${buildPageParams(params)}`),
  });
}

export function usePolicies(params: PageParams = {}) {
  return useQuery({
    queryKey: governanceKeys.policyList(params),
    queryFn: () => api.get<Page<AccessPolicy>>(`/api/v1/governance/policies?${buildPageParams(params)}`),
  });
}

export function useCreatePolicy() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (data: Partial<AccessPolicy>) => api.post<AccessPolicy>('/api/v1/governance/policies', data),
    onSuccess: () => { queryClient.invalidateQueries({ queryKey: governanceKeys.policies }); },
  });
}

export function useWorkflows(params: PageParams & { status?: string } = {}) {
  const { status, ...pageParams } = params;
  let queryStr = buildPageParams(pageParams);
  if (status) queryStr += `&status=${status}`;
  return useQuery({
    queryKey: governanceKeys.workflowList(params),
    queryFn: () => api.get<Page<ApprovalWorkflow>>(`/api/v1/governance/workflows?${queryStr}`),
  });
}

export function useApproveWorkflow() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (id: string) => api.patch<ApprovalWorkflow>(`/api/v1/governance/workflows/${id}/approve`, {}),
    onSuccess: () => { queryClient.invalidateQueries({ queryKey: governanceKeys.workflows }); },
  });
}

export function useRejectWorkflow() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (id: string) => api.patch<ApprovalWorkflow>(`/api/v1/governance/workflows/${id}/reject`, {}),
    onSuccess: () => { queryClient.invalidateQueries({ queryKey: governanceKeys.workflows }); },
  });
}
