'use client';

import { useRouter } from 'next/navigation';
import Link from 'next/link';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { useCreateApp } from '@/services/developer';
import { PageHeader } from '@/components/layout/page-header';
import { ArrowLeft } from 'lucide-react';
import { toast } from 'sonner';

const appSchema = z.object({
  name: z.string().min(1, 'Name is required').max(200, 'Name must be under 200 characters'),
  description: z.string().optional(),
  redirectUris: z.string().min(1, 'At least one redirect URI is required'),
});

type AppFormValues = z.infer<typeof appSchema>;

export default function NewAppPage() {
  const router = useRouter();
  const createApp = useCreateApp();
  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<AppFormValues>({
    resolver: zodResolver(appSchema),
  });

  const onSubmit = async (data: AppFormValues) => {
    try {
      const redirectUris = data.redirectUris
        .split(',')
        .map((uri) => uri.trim())
        .filter(Boolean);
      await createApp.mutateAsync({ name: data.name, description: data.description, redirectUris });
      toast.success('App registered successfully');
      router.push('/developer');
    } catch {
      toast.error('Failed to register app');
    }
  };

  return (
    <div className="space-y-6">
      <PageHeader
        title="Register App"
        description="Register a new developer application"
        actions={
          <Link
            href="/developer"
            className="inline-flex items-center gap-2 rounded-md border px-3 py-2 text-sm hover:bg-accent"
          >
            <ArrowLeft className="h-4 w-4" /> Back
          </Link>
        }
      />

      <form onSubmit={handleSubmit(onSubmit)} className="max-w-2xl space-y-6">
        <div className="rounded-lg border bg-card p-6 space-y-4">
          <h3 className="font-semibold">Application Details</h3>

          <div>
            <label className="text-sm font-medium">Name *</label>
            <input
              {...register('name')}
              className="mt-1 w-full rounded-md border bg-background px-3 py-2 text-sm"
              placeholder="e.g., My Integration App"
            />
            {errors.name && (
              <p className="mt-1 text-xs text-destructive">{errors.name.message}</p>
            )}
          </div>

          <div>
            <label className="text-sm font-medium">Description</label>
            <textarea
              {...register('description')}
              rows={3}
              className="mt-1 w-full rounded-md border bg-background px-3 py-2 text-sm"
              placeholder="Describe what this application does..."
            />
          </div>

          <div>
            <label className="text-sm font-medium">Redirect URIs *</label>
            <input
              {...register('redirectUris')}
              className="mt-1 w-full rounded-md border bg-background px-3 py-2 text-sm"
              placeholder="https://app.example.com/callback, https://app.example.com/auth"
            />
            <p className="mt-1 text-xs text-muted-foreground">Comma-separated list of allowed redirect URIs</p>
            {errors.redirectUris && (
              <p className="mt-1 text-xs text-destructive">{errors.redirectUris.message}</p>
            )}
          </div>
        </div>

        <div className="flex gap-3">
          <button
            type="submit"
            disabled={isSubmitting}
            className="rounded-md bg-primary px-6 py-2 text-sm font-medium text-primary-foreground hover:bg-primary/90 disabled:opacity-50"
          >
            {isSubmitting ? 'Registering...' : 'Register App'}
          </button>
          <Link
            href="/developer"
            className="rounded-md border px-6 py-2 text-sm hover:bg-accent"
          >
            Cancel
          </Link>
        </div>
      </form>
    </div>
  );
}
