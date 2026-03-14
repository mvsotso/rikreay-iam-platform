'use client';

import { useSession } from 'next-auth/react';
import { useMyProfile } from '@/services/identity';
import { useRoles } from '@/hooks/use-roles';
import { PageHeader } from '@/components/layout/page-header';
import { StatusBadge } from '@/components/shared/status-badge';
import { formatDate, formatDateTime } from '@/lib/utils';
import { User, Mail, Phone, Shield, Key } from 'lucide-react';

export default function ProfilePage() {
  const { data: session } = useSession();
  const { data: profile, isLoading } = useMyProfile();
  const { roles } = useRoles();

  return (
    <div className="space-y-6">
      <PageHeader title="My Profile" description="Your account information and roles" />

      {/* Session Info */}
      <div className="rounded-lg border bg-card p-6">
        <h3 className="text-lg font-semibold mb-4 flex items-center gap-2"><Key className="h-5 w-5" /> Account</h3>
        <dl className="grid grid-cols-2 gap-4 text-sm">
          <div><dt className="text-muted-foreground">Name</dt><dd className="font-medium">{session?.user?.name ?? '—'}</dd></div>
          <div><dt className="text-muted-foreground">Email</dt><dd className="font-medium">{session?.user?.email ?? '—'}</dd></div>
        </dl>
      </div>

      {/* Roles */}
      <div className="rounded-lg border bg-card p-6">
        <h3 className="text-lg font-semibold mb-4 flex items-center gap-2"><Shield className="h-5 w-5" /> Roles</h3>
        <div className="flex flex-wrap gap-2">
          {roles.map((role) => (
            <span key={role} className="inline-flex rounded-full bg-primary/10 px-3 py-1 text-sm font-medium text-primary">{role}</span>
          ))}
          {roles.length === 0 && <p className="text-muted-foreground text-sm">No roles assigned</p>}
        </div>
      </div>

      {/* Profile from backend */}
      {isLoading ? (
        <div className="h-48 rounded-lg bg-muted animate-pulse" />
      ) : profile ? (
        <div className="rounded-lg border bg-card p-6">
          <h3 className="text-lg font-semibold mb-4 flex items-center gap-2"><User className="h-5 w-5" /> Identity Profile</h3>
          <dl className="grid grid-cols-2 gap-4 text-sm">
            <div><dt className="text-muted-foreground">Name (Khmer)</dt><dd className="font-medium">{profile.lastNameKh} {profile.firstNameKh}</dd></div>
            <div><dt className="text-muted-foreground">Name (English)</dt><dd className="font-medium">{profile.firstNameEn ? `${profile.firstNameEn} ${profile.lastNameEn}` : '—'}</dd></div>
            <div><dt className="text-muted-foreground">ID Code</dt><dd className="font-medium">{profile.personalIdCode}</dd></div>
            <div><dt className="text-muted-foreground">Verification</dt><dd><StatusBadge status={profile.verificationStatus} /></dd></div>
            <div><dt className="text-muted-foreground">Level</dt><dd>Level {profile.verificationLevel}</dd></div>
            <div><dt className="text-muted-foreground">Date of Birth</dt><dd>{profile.dateOfBirth ? formatDate(profile.dateOfBirth) : '—'}</dd></div>
            {profile.email && <div className="flex items-center gap-2"><Mail className="h-4 w-4 text-muted-foreground" /><dd>{profile.email}</dd></div>}
            {profile.phone && <div className="flex items-center gap-2"><Phone className="h-4 w-4 text-muted-foreground" /><dd>{profile.phone}</dd></div>}
          </dl>
        </div>
      ) : (
        <div className="rounded-lg border bg-card p-6 text-center text-muted-foreground">
          <User className="h-8 w-8 mx-auto mb-2" />
          <p>No identity profile linked to your account yet.</p>
        </div>
      )}
    </div>
  );
}
