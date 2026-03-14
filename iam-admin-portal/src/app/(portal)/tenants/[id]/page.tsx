'use client';

import { useParams, useRouter } from 'next/navigation';
import Link from 'next/link';
import { useTenant, useDeleteTenant } from '@/services/admin';
import { PageHeader } from '@/components/layout/page-header';
import { MemberClassBadge } from '@/components/shared/member-class-badge';
import { formatDate, formatDateTime } from '@/lib/utils';
import { ArrowLeft, Pencil, Trash2, Users, Mail, Shield } from 'lucide-react';
import { toast } from 'sonner';

export default function TenantDetailPage() {
  const { id } = useParams<{ id: string }>();
  const router = useRouter();
  const { data: tenant, isLoading } = useTenant(id);
  const deleteMutation = useDeleteTenant();

  const handleDelete = async () => {
    if (!confirm('Are you sure? This will delete the tenant and its Keycloak realm.')) return;
    try {
      await deleteMutation.mutateAsync(id);
      toast.success('Tenant deleted');
      router.push('/tenants');
    } catch {
      toast.error('Failed to delete tenant');
    }
  };

  if (isLoading) return <div className="space-y-4">{Array.from({ length: 3 }).map((_, i) => <div key={i} className="h-24 rounded-lg bg-muted animate-pulse" />)}</div>;
  if (!tenant) return <div className="text-center py-12 text-muted-foreground">Tenant not found</div>;

  const usagePercent = tenant.maxUsers > 0 ? Math.round((tenant.currentUsers / tenant.maxUsers) * 100) : 0;

  return (
    <div className="space-y-6">
      <PageHeader
        title={tenant.name}
        description={tenant.description ?? tenant.realmName}
        actions={
          <div className="flex gap-2">
            <Link href="/tenants" className="inline-flex items-center gap-2 rounded-md border px-3 py-2 text-sm hover:bg-accent"><ArrowLeft className="h-4 w-4" /> Back</Link>
            <Link href={`/tenants/${id}/edit`} className="inline-flex items-center gap-2 rounded-md bg-primary px-3 py-2 text-sm text-primary-foreground hover:bg-primary/90"><Pencil className="h-4 w-4" /> Edit</Link>
            <button onClick={handleDelete} className="inline-flex items-center gap-2 rounded-md bg-destructive px-3 py-2 text-sm text-destructive-foreground hover:bg-destructive/90"><Trash2 className="h-4 w-4" /> Delete</button>
          </div>
        }
      />

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        <div className="lg:col-span-2 space-y-6">
          <div className="rounded-lg border bg-card p-6">
            <h3 className="text-lg font-semibold mb-4">Tenant Information</h3>
            <dl className="grid grid-cols-2 gap-4 text-sm">
              <div><dt className="text-muted-foreground">Name</dt><dd className="font-medium">{tenant.name}</dd></div>
              <div><dt className="text-muted-foreground">Realm</dt><dd className="font-mono">{tenant.realmName}</dd></div>
              <div><dt className="text-muted-foreground">Sector</dt><dd><MemberClassBadge memberClass={tenant.memberClass} /></dd></div>
              <div><dt className="text-muted-foreground">Status</dt><dd><span className={`inline-flex rounded-full px-2 py-0.5 text-xs font-medium ${tenant.enabled ? 'bg-green-100 text-green-700 dark:bg-green-900/30 dark:text-green-400' : 'bg-red-100 text-red-700 dark:bg-red-900/30 dark:text-red-400'}`}>{tenant.enabled ? 'Active' : 'Inactive'}</span></dd></div>
              {tenant.description && <div className="col-span-2"><dt className="text-muted-foreground">Description</dt><dd>{tenant.description}</dd></div>}
            </dl>
          </div>

          <div className="rounded-lg border bg-card p-6">
            <h3 className="text-lg font-semibold mb-4 flex items-center gap-2"><Mail className="h-5 w-5" /> Admin</h3>
            <dl className="grid grid-cols-2 gap-4 text-sm">
              <div><dt className="text-muted-foreground">Username</dt><dd className="font-medium">{tenant.adminUsername}</dd></div>
              <div><dt className="text-muted-foreground">Email</dt><dd>{tenant.adminEmail}</dd></div>
            </dl>
          </div>
        </div>

        <div className="space-y-6">
          <div className="rounded-lg border bg-card p-6">
            <h3 className="text-sm font-semibold mb-3 flex items-center gap-2"><Users className="h-4 w-4" /> User Usage</h3>
            <div className="space-y-3">
              <div className="flex justify-between text-sm">
                <span>{tenant.currentUsers} / {tenant.maxUsers} users</span>
                <span>{usagePercent}%</span>
              </div>
              <div className="h-2 rounded-full bg-muted overflow-hidden">
                <div className={`h-full rounded-full transition-all ${usagePercent > 90 ? 'bg-destructive' : usagePercent > 70 ? 'bg-yellow-500' : 'bg-primary'}`} style={{ width: `${usagePercent}%` }} />
              </div>
            </div>
          </div>

          <div className="rounded-lg border bg-card p-6">
            <h3 className="text-sm font-semibold mb-3">Timestamps</h3>
            <div className="space-y-2 text-sm">
              <div><span className="text-muted-foreground">Created:</span> {formatDateTime(tenant.createdAt)}</div>
              <div><span className="text-muted-foreground">Updated:</span> {formatDateTime(tenant.updatedAt)}</div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
