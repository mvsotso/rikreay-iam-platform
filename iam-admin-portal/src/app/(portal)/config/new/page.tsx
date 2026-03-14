'use client';

import { useRouter } from 'next/navigation';
import Link from 'next/link';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { useCreateFlag } from '@/services/config';
import { PageHeader } from '@/components/layout/page-header';
import { ArrowLeft } from 'lucide-react';
import { toast } from 'sonner';

const flagSchema = z.object({
  key: z
    .string()
    .min(1, 'Key is required')
    .max(100, 'Key must be under 100 characters')
    .regex(/^[a-z0-9._-]+$/, 'Key must be lowercase alphanumeric with dots, hyphens, or underscores'),
  name: z.string().min(1, 'Name is required').max(200, 'Name must be under 200 characters'),
  description: z.string().optional(),
  enabled: z.boolean().default(false),
});

type FlagFormValues = z.infer<typeof flagSchema>;

export default function NewFlagPage() {
  const router = useRouter();
  const createFlag = useCreateFlag();
  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<FlagFormValues>({
    resolver: zodResolver(flagSchema),
    defaultValues: {
      enabled: false,
    },
  });

  const onSubmit = async (data: FlagFormValues) => {
    try {
      await createFlag.mutateAsync(data);
      toast.success('Feature flag created successfully');
      router.push('/config');
    } catch {
      toast.error('Failed to create feature flag');
    }
  };

  return (
    <div className="space-y-6">
      <PageHeader
        title="New Feature Flag"
        description="Create a new feature flag"
        actions={
          <Link
            href="/config"
            className="inline-flex items-center gap-2 rounded-md border px-3 py-2 text-sm hover:bg-accent"
          >
            <ArrowLeft className="h-4 w-4" /> Back
          </Link>
        }
      />

      <form onSubmit={handleSubmit(onSubmit)} className="max-w-2xl space-y-6">
        <div className="rounded-lg border bg-card p-6 space-y-4">
          <h3 className="font-semibold">Flag Details</h3>

          <div>
            <label className="text-sm font-medium">Key *</label>
            <input
              {...register('key')}
              className="mt-1 w-full rounded-md border bg-background px-3 py-2 text-sm"
              placeholder="e.g., feature.new-dashboard"
            />
            <p className="mt-1 text-xs text-muted-foreground">
              Lowercase with dots, hyphens, or underscores
            </p>
            {errors.key && (
              <p className="mt-1 text-xs text-destructive">{errors.key.message}</p>
            )}
          </div>

          <div>
            <label className="text-sm font-medium">Name *</label>
            <input
              {...register('name')}
              className="mt-1 w-full rounded-md border bg-background px-3 py-2 text-sm"
              placeholder="e.g., New Dashboard Feature"
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
              placeholder="Describe what this flag controls..."
            />
          </div>

          <div className="flex items-end">
            <label className="flex items-center gap-2 text-sm font-medium">
              <input
                type="checkbox"
                {...register('enabled')}
                className="h-4 w-4 rounded border"
              />
              Enabled by default
            </label>
          </div>
        </div>

        <div className="flex gap-3">
          <button
            type="submit"
            disabled={isSubmitting}
            className="rounded-md bg-primary px-6 py-2 text-sm font-medium text-primary-foreground hover:bg-primary/90 disabled:opacity-50"
          >
            {isSubmitting ? 'Creating...' : 'Create Flag'}
          </button>
          <Link
            href="/config"
            className="rounded-md border px-6 py-2 text-sm hover:bg-accent"
          >
            Cancel
          </Link>
        </div>
      </form>
    </div>
  );
}
