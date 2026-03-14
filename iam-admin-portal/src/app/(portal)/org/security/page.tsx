'use client';

import { PageHeader } from '@/components/layout/page-header';
import {
  Shield,
  KeyRound,
  Smartphone,
  Clock,
  Lock,
  AlertTriangle,
} from 'lucide-react';

interface SecuritySection {
  title: string;
  description: string;
  icon: typeof Shield;
  items: { label: string; value: string }[];
}

const securitySections: SecuritySection[] = [
  {
    title: 'Password Policy',
    description: 'Minimum requirements for user passwords in this organization',
    icon: KeyRound,
    items: [
      { label: 'Minimum Length', value: '12 characters' },
      { label: 'Require Uppercase', value: 'Yes' },
      { label: 'Require Lowercase', value: 'Yes' },
      { label: 'Require Digits', value: 'Yes' },
      { label: 'Require Special Characters', value: 'Yes' },
      { label: 'Password History', value: 'Last 5 passwords' },
      { label: 'Max Password Age', value: '90 days' },
    ],
  },
  {
    title: 'Multi-Factor Authentication',
    description: 'MFA configuration for the organization realm',
    icon: Smartphone,
    items: [
      { label: 'MFA Required', value: 'Yes' },
      { label: 'Allowed Methods', value: 'TOTP, WebAuthn' },
      { label: 'Grace Period', value: '7 days for new users' },
      { label: 'Recovery Codes', value: 'Enabled (8 codes)' },
    ],
  },
  {
    title: 'Session Management',
    description: 'Session timeout and idle policies',
    icon: Clock,
    items: [
      { label: 'Session Timeout', value: '30 minutes' },
      { label: 'Idle Timeout', value: '15 minutes' },
      { label: 'Max Concurrent Sessions', value: '3' },
      { label: 'Remember Me', value: '30 days' },
    ],
  },
  {
    title: 'Brute Force Protection',
    description: 'Account lockout settings after failed login attempts',
    icon: Lock,
    items: [
      { label: 'Max Login Failures', value: '5 attempts' },
      { label: 'Lockout Duration', value: '15 minutes' },
      { label: 'Failure Reset Time', value: '12 hours' },
      { label: 'Permanent Lockout After', value: '20 failures' },
    ],
  },
];

export default function OrgSecurityPage() {
  return (
    <div className="space-y-6">
      <PageHeader
        title="Security Settings"
        description="View your organization's security configuration (managed by Keycloak realm)"
      />

      <div className="rounded-lg border border-amber-300/50 bg-amber-50/50 dark:bg-amber-950/20 p-4 flex items-start gap-3">
        <AlertTriangle className="h-5 w-5 text-amber-600 dark:text-amber-400 shrink-0 mt-0.5" />
        <div>
          <p className="text-sm font-medium text-amber-800 dark:text-amber-300">
            Read-Only Configuration
          </p>
          <p className="text-sm text-amber-700 dark:text-amber-400 mt-1">
            Security settings are managed through your Keycloak realm configuration.
            Contact a platform administrator to request changes.
          </p>
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {securitySections.map((section) => {
          const Icon = section.icon;
          return (
            <div
              key={section.title}
              className="rounded-xl border bg-card p-6 shadow-sm"
            >
              <div className="flex items-center gap-3 mb-4">
                <div className="flex h-10 w-10 items-center justify-center rounded-lg bg-primary/10">
                  <Icon className="h-5 w-5 text-primary" />
                </div>
                <div>
                  <h3 className="font-semibold">{section.title}</h3>
                  <p className="text-xs text-muted-foreground">
                    {section.description}
                  </p>
                </div>
              </div>
              <div className="space-y-3">
                {section.items.map((item) => (
                  <div
                    key={item.label}
                    className="flex items-center justify-between text-sm"
                  >
                    <span className="text-muted-foreground">{item.label}</span>
                    <span className="font-medium">{item.value}</span>
                  </div>
                ))}
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );
}
