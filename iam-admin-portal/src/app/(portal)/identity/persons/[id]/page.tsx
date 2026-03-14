'use client';

import { useParams, useRouter } from 'next/navigation';
import Link from 'next/link';
import { usePerson, useDeletePerson } from '@/services/identity';
import { PageHeader } from '@/components/layout/page-header';
import { StatusBadge } from '@/components/shared/status-badge';
import { formatDate, formatDateTime } from '@/lib/utils';
import { ArrowLeft, Pencil, Trash2, Mail, Phone, Shield, FileText } from 'lucide-react';
import { toast } from 'sonner';

export default function PersonDetailPage() {
  const { id } = useParams<{ id: string }>();
  const router = useRouter();
  const { data: person, isLoading } = usePerson(id);
  const deleteMutation = useDeletePerson();

  const handleDelete = async () => {
    if (!confirm('Are you sure you want to delete this person?')) return;
    try {
      await deleteMutation.mutateAsync(id);
      toast.success('Person deleted');
      router.push('/identity/persons');
    } catch {
      toast.error('Failed to delete person');
    }
  };

  if (isLoading) {
    return <div className="space-y-4">{Array.from({ length: 3 }).map((_, i) => <div key={i} className="h-24 rounded-lg bg-muted animate-pulse" />)}</div>;
  }

  if (!person) {
    return <div className="text-center py-12 text-muted-foreground">Person not found</div>;
  }

  return (
    <div className="space-y-6">
      <PageHeader
        title={`${person.lastNameKh} ${person.firstNameKh}`}
        description={person.firstNameEn ? `${person.firstNameEn} ${person.lastNameEn}` : person.personalIdCode}
        actions={
          <div className="flex gap-2">
            <Link href="/identity/persons" className="inline-flex items-center gap-2 rounded-md border px-3 py-2 text-sm hover:bg-accent">
              <ArrowLeft className="h-4 w-4" /> Back
            </Link>
            <Link href={`/identity/persons/${id}/edit`} className="inline-flex items-center gap-2 rounded-md bg-primary px-3 py-2 text-sm text-primary-foreground hover:bg-primary/90">
              <Pencil className="h-4 w-4" /> Edit
            </Link>
            <button onClick={handleDelete} className="inline-flex items-center gap-2 rounded-md bg-destructive px-3 py-2 text-sm text-destructive-foreground hover:bg-destructive/90">
              <Trash2 className="h-4 w-4" /> Delete
            </button>
          </div>
        }
      />

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Main Info */}
        <div className="lg:col-span-2 space-y-6">
          <div className="rounded-lg border bg-card p-6">
            <h3 className="text-lg font-semibold mb-4">Personal Information</h3>
            <dl className="grid grid-cols-2 gap-4 text-sm">
              <div><dt className="text-muted-foreground">ID Code</dt><dd className="font-medium">{person.personalIdCode}</dd></div>
              <div><dt className="text-muted-foreground">National ID</dt><dd className="font-medium">{person.nationalIdNumber ?? '—'}</dd></div>
              <div><dt className="text-muted-foreground">Name (Khmer)</dt><dd className="font-medium">{person.lastNameKh} {person.firstNameKh}</dd></div>
              <div><dt className="text-muted-foreground">Name (English)</dt><dd className="font-medium">{person.firstNameEn ? `${person.firstNameEn} ${person.lastNameEn}` : '—'}</dd></div>
              <div><dt className="text-muted-foreground">Date of Birth</dt><dd className="font-medium">{person.dateOfBirth ? formatDate(person.dateOfBirth) : '—'}</dd></div>
              <div><dt className="text-muted-foreground">Gender</dt><dd className="font-medium">{person.gender ?? '—'}</dd></div>
              <div><dt className="text-muted-foreground">CamDigiKey ID</dt><dd className="font-medium">{person.camDigiKeyId ?? '—'}</dd></div>
              <div><dt className="text-muted-foreground">Keycloak User ID</dt><dd className="font-mono text-xs">{person.keycloakUserId ?? '—'}</dd></div>
            </dl>
          </div>

          {/* Contact */}
          <div className="rounded-lg border bg-card p-6">
            <h3 className="text-lg font-semibold mb-4">Contact Information</h3>
            <div className="space-y-3">
              {person.email && (
                <div className="flex items-center gap-3"><Mail className="h-4 w-4 text-muted-foreground" /><span className="text-sm">{person.email}</span></div>
              )}
              {person.phone && (
                <div className="flex items-center gap-3"><Phone className="h-4 w-4 text-muted-foreground" /><span className="text-sm">{person.phone}</span></div>
              )}
              {person.contacts?.map((c) => (
                <div key={c.id} className="flex items-center gap-3 text-sm">
                  <span className="text-muted-foreground">{c.channelType}</span>
                  <span>{c.value}</span>
                  {c.verified && <span className="text-green-600 text-xs">Verified</span>}
                  {c.primary && <span className="text-primary text-xs">Primary</span>}
                </div>
              ))}
            </div>
          </div>

          {/* Documents */}
          {person.documents && person.documents.length > 0 && (
            <div className="rounded-lg border bg-card p-6">
              <h3 className="text-lg font-semibold mb-4 flex items-center gap-2"><FileText className="h-5 w-5" /> Documents</h3>
              <div className="space-y-3">
                {person.documents.map((doc) => (
                  <div key={doc.id} className="flex items-center justify-between border-b pb-2 last:border-0">
                    <div>
                      <p className="text-sm font-medium">{doc.documentType}</p>
                      <p className="text-xs text-muted-foreground">{doc.documentNumber}</p>
                    </div>
                    <span className={doc.verified ? 'text-green-600 text-xs' : 'text-muted-foreground text-xs'}>{doc.verified ? 'Verified' : 'Unverified'}</span>
                  </div>
                ))}
              </div>
            </div>
          )}
        </div>

        {/* Sidebar */}
        <div className="space-y-6">
          <div className="rounded-lg border bg-card p-6">
            <h3 className="text-sm font-semibold mb-3 flex items-center gap-2"><Shield className="h-4 w-4" /> Verification</h3>
            <div className="space-y-3">
              <div className="flex items-center justify-between">
                <span className="text-sm text-muted-foreground">Status</span>
                <StatusBadge status={person.verificationStatus} />
              </div>
              <div className="flex items-center justify-between">
                <span className="text-sm text-muted-foreground">Level</span>
                <span className="text-sm font-medium">Level {person.verificationLevel}</span>
              </div>
            </div>
          </div>

          <div className="rounded-lg border bg-card p-6">
            <h3 className="text-sm font-semibold mb-3">Timestamps</h3>
            <div className="space-y-2 text-sm">
              <div><span className="text-muted-foreground">Created:</span> <span>{formatDateTime(person.createdAt)}</span></div>
              <div><span className="text-muted-foreground">Updated:</span> <span>{formatDateTime(person.updatedAt)}</span></div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
