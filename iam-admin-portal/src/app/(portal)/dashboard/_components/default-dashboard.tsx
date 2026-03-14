'use client';

import { useSession } from 'next-auth/react';
import { PageHeader } from '@/components/layout/page-header';
import { useRoles } from '@/hooks/use-roles';
import { Shield, User, ExternalLink } from 'lucide-react';

export function DefaultDashboard() {
  const { data: session } = useSession();
  const { roles } = useRoles();

  return (
    <div className="space-y-6">
      <PageHeader title="Welcome" description={`Signed in as ${session?.user?.name ?? session?.user?.email ?? 'User'}`} />

      <div className="rounded-lg border bg-card p-6">
        <h3 className="text-lg font-semibold mb-4 flex items-center gap-2">
          <Shield className="h-5 w-5" />
          Your Roles
        </h3>
        <div className="flex flex-wrap gap-2">
          {roles.map((role) => (
            <span key={role} className="inline-flex items-center rounded-full bg-primary/10 px-3 py-1 text-sm font-medium text-primary">
              {role}
            </span>
          ))}
          {roles.length === 0 && <p className="text-muted-foreground text-sm">No roles assigned</p>}
        </div>
      </div>

      <div className="rounded-lg border bg-card p-6">
        <h3 className="text-lg font-semibold mb-4 flex items-center gap-2">
          <User className="h-5 w-5" />
          Quick Links
        </h3>
        <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
          <a href="/profile" className="flex items-center gap-2 rounded-md border p-3 hover:bg-accent transition-colors">
            <User className="h-4 w-4" />
            <span className="text-sm">My Profile</span>
            <ExternalLink className="h-3 w-3 ml-auto text-muted-foreground" />
          </a>
        </div>
      </div>
    </div>
  );
}
