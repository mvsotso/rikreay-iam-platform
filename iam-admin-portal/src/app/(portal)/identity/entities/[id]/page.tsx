'use client';

import { useParams, useRouter } from 'next/navigation';
import Link from 'next/link';
import { useEntity, useDeleteEntity } from '@/services/identity';
import { PageHeader } from '@/components/layout/page-header';
import { StatusBadge } from '@/components/shared/status-badge';
import { MemberClassBadge } from '@/components/shared/member-class-badge';
import { formatDate, formatDateTime } from '@/lib/utils';
import { ArrowLeft, Pencil, Trash2, Globe, Mail, Phone, Users } from 'lucide-react';
import { toast } from 'sonner';

export default function EntityDetailPage() {
  const { id } = useParams<{ id: string }>();
  const router = useRouter();
  const { data: entity, isLoading } = useEntity(id);
  const deleteMutation = useDeleteEntity();

  const handleDelete = async () => {
    if (!confirm('Are you sure you want to delete this entity?')) return;
    try {
      await deleteMutation.mutateAsync(id);
      toast.success('Entity deleted');
      router.push('/identity/entities');
    } catch {
      toast.error('Failed to delete entity');
    }
  };

  if (isLoading) return <div className="space-y-4">{Array.from({ length: 3 }).map((_, i) => <div key={i} className="h-24 rounded-lg bg-muted animate-pulse" />)}</div>;
  if (!entity) return <div className="text-center py-12 text-muted-foreground">Entity not found</div>;

  return (
    <div className="space-y-6">
      <PageHeader
        title={entity.nameKh}
        description={entity.nameEn ?? entity.registrationNumber}
        actions={
          <div className="flex gap-2">
            <Link href="/identity/entities" className="inline-flex items-center gap-2 rounded-md border px-3 py-2 text-sm hover:bg-accent"><ArrowLeft className="h-4 w-4" /> Back</Link>
            <Link href={`/identity/entities/${id}/edit`} className="inline-flex items-center gap-2 rounded-md bg-primary px-3 py-2 text-sm text-primary-foreground hover:bg-primary/90"><Pencil className="h-4 w-4" /> Edit</Link>
            <button onClick={handleDelete} className="inline-flex items-center gap-2 rounded-md bg-destructive px-3 py-2 text-sm text-destructive-foreground hover:bg-destructive/90"><Trash2 className="h-4 w-4" /> Delete</button>
          </div>
        }
      />

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        <div className="lg:col-span-2 space-y-6">
          <div className="rounded-lg border bg-card p-6">
            <h3 className="text-lg font-semibold mb-4">Entity Information</h3>
            <dl className="grid grid-cols-2 gap-4 text-sm">
              <div><dt className="text-muted-foreground">Registration Number</dt><dd className="font-medium">{entity.registrationNumber}</dd></div>
              <div><dt className="text-muted-foreground">TIN</dt><dd className="font-medium">{entity.tin ?? '—'}</dd></div>
              <div><dt className="text-muted-foreground">Name (Khmer)</dt><dd className="font-medium">{entity.nameKh}</dd></div>
              <div><dt className="text-muted-foreground">Name (English)</dt><dd className="font-medium">{entity.nameEn ?? '—'}</dd></div>
              <div><dt className="text-muted-foreground">Entity Type</dt><dd className="font-medium">{entity.entityType.replace(/_/g, ' ')}</dd></div>
              <div><dt className="text-muted-foreground">Member Class</dt><dd><MemberClassBadge memberClass={entity.memberClass} /></dd></div>
              <div><dt className="text-muted-foreground">Realm</dt><dd className="font-mono text-xs">{entity.realmName ?? '—'}</dd></div>
            </dl>
          </div>

          <div className="rounded-lg border bg-card p-6">
            <h3 className="text-lg font-semibold mb-4">Contact</h3>
            <div className="space-y-3">
              {entity.email && <div className="flex items-center gap-3"><Mail className="h-4 w-4 text-muted-foreground" /><span className="text-sm">{entity.email}</span></div>}
              {entity.phone && <div className="flex items-center gap-3"><Phone className="h-4 w-4 text-muted-foreground" /><span className="text-sm">{entity.phone}</span></div>}
              {entity.website && <div className="flex items-center gap-3"><Globe className="h-4 w-4 text-muted-foreground" /><span className="text-sm">{entity.website}</span></div>}
            </div>
          </div>

          {/* Representatives */}
          {entity.representatives && entity.representatives.length > 0 && (
            <div className="rounded-lg border bg-card p-6">
              <h3 className="text-lg font-semibold mb-4 flex items-center gap-2"><Users className="h-5 w-5" /> Representatives</h3>
              <div className="space-y-3">
                {entity.representatives.map((rep) => (
                  <div key={rep.id} className="flex items-center justify-between border-b pb-3 last:border-0">
                    <div>
                      <p className="text-sm font-medium">{rep.naturalPersonName ?? rep.naturalPersonId}</p>
                      <p className="text-xs text-muted-foreground">{rep.representativeRole.replace(/_/g, ' ')} · {rep.delegationScope}</p>
                    </div>
                    <StatusBadge status={rep.verificationStatus} />
                  </div>
                ))}
              </div>
            </div>
          )}
        </div>

        <div className="space-y-6">
          <div className="rounded-lg border bg-card p-6">
            <h3 className="text-sm font-semibold mb-3">Verification</h3>
            <StatusBadge status={entity.verificationStatus} />
          </div>
          <div className="rounded-lg border bg-card p-6">
            <h3 className="text-sm font-semibold mb-3">Timestamps</h3>
            <div className="space-y-2 text-sm">
              <div><span className="text-muted-foreground">Created:</span> {formatDateTime(entity.createdAt)}</div>
              <div><span className="text-muted-foreground">Updated:</span> {formatDateTime(entity.updatedAt)}</div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
