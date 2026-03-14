'use client';

import Link from 'next/link';
import { useCampaigns, useConsents, usePolicies, useWorkflows } from '@/services/governance';
import { PageHeader } from '@/components/layout/page-header';
import { StatCard } from '@/components/charts/stat-card';
import { ClipboardCheck, FileText, Shield, GitPullRequest } from 'lucide-react';

export default function GovernancePage() {
  const { data: campaigns, isLoading: campaignsLoading } = useCampaigns({ size: 1 });
  const { data: consents, isLoading: consentsLoading } = useConsents({ size: 1 });
  const { data: policies, isLoading: policiesLoading } = usePolicies({ size: 1 });
  const { data: workflows, isLoading: workflowsLoading } = useWorkflows({ size: 1 });

  const sections = [
    {
      title: 'Access Review Campaigns',
      description: 'Manage periodic access reviews and certifications',
      href: '/governance/campaigns',
      icon: ClipboardCheck,
      count: campaigns?.totalElements ?? 0,
      loading: campaignsLoading,
    },
    {
      title: 'Consent Records',
      description: 'LPDP consent tracking for data subjects',
      href: '/governance/consents',
      icon: FileText,
      count: consents?.totalElements ?? 0,
      loading: consentsLoading,
    },
    {
      title: 'Access Policies',
      description: 'Define and manage access control policies',
      href: '/governance/policies',
      icon: Shield,
      count: policies?.totalElements ?? 0,
      loading: policiesLoading,
    },
    {
      title: 'Approval Workflows',
      description: 'Review and process pending approval requests',
      href: '/governance/workflows',
      icon: GitPullRequest,
      count: workflows?.totalElements ?? 0,
      loading: workflowsLoading,
    },
  ];

  return (
    <div className="space-y-6">
      <PageHeader
        title="Governance"
        description="Identity governance, access reviews, consent management, and compliance"
      />

      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
        {sections.map((section) => (
          <StatCard
            key={section.href}
            title={section.title}
            value={section.loading ? '...' : section.count}
            icon={section.icon}
          />
        ))}
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
        {sections.map((section) => (
          <Link
            key={section.href}
            href={section.href}
            className="rounded-lg border bg-card p-6 hover:border-primary/50 transition-colors"
          >
            <div className="flex items-start gap-4">
              <div className="rounded-md bg-primary/10 p-3">
                <section.icon className="h-6 w-6 text-primary" />
              </div>
              <div>
                <h3 className="font-semibold">{section.title}</h3>
                <p className="text-sm text-muted-foreground mt-1">{section.description}</p>
                <p className="text-sm text-primary mt-2 font-medium">
                  {section.loading ? 'Loading...' : `${section.count} records`} &rarr;
                </p>
              </div>
            </div>
          </Link>
        ))}
      </div>
    </div>
  );
}
