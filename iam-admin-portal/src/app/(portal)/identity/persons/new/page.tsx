'use client';

import { useRouter } from 'next/navigation';
import Link from 'next/link';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { useCreatePerson } from '@/services/identity';
import { PageHeader } from '@/components/layout/page-header';
import { ArrowLeft } from 'lucide-react';
import { toast } from 'sonner';

const personSchema = z.object({
  personalIdCode: z.string().min(1, 'ID code is required'),
  nationalIdNumber: z.string().optional(),
  firstNameKh: z.string().min(1, 'Khmer first name is required'),
  lastNameKh: z.string().min(1, 'Khmer last name is required'),
  firstNameEn: z.string().optional(),
  lastNameEn: z.string().optional(),
  dateOfBirth: z.string().optional(),
  gender: z.enum(['MALE', 'FEMALE', 'OTHER']).optional(),
  email: z.string().email('Invalid email').optional().or(z.literal('')),
  phone: z.string().optional(),
});

type PersonFormValues = z.infer<typeof personSchema>;

export default function NewPersonPage() {
  const router = useRouter();
  const createPerson = useCreatePerson();
  const { register, handleSubmit, formState: { errors, isSubmitting } } = useForm<PersonFormValues>({
    resolver: zodResolver(personSchema),
  });

  const onSubmit = async (data: PersonFormValues) => {
    try {
      const cleaned = { ...data, email: data.email || undefined };
      await createPerson.mutateAsync(cleaned);
      toast.success('Person created successfully');
      router.push('/identity/persons');
    } catch {
      toast.error('Failed to create person');
    }
  };

  return (
    <div className="space-y-6">
      <PageHeader
        title="New Natural Person"
        description="Register a new natural person identity"
        actions={
          <Link href="/identity/persons" className="inline-flex items-center gap-2 rounded-md border px-3 py-2 text-sm hover:bg-accent">
            <ArrowLeft className="h-4 w-4" /> Back
          </Link>
        }
      />

      <form onSubmit={handleSubmit(onSubmit)} className="max-w-2xl space-y-6">
        <div className="rounded-lg border bg-card p-6 space-y-4">
          <h3 className="font-semibold">Identification</h3>
          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="text-sm font-medium">Personal ID Code *</label>
              <input {...register('personalIdCode')} className="mt-1 w-full rounded-md border bg-background px-3 py-2 text-sm" placeholder="e.g., KH-123456789" />
              {errors.personalIdCode && <p className="mt-1 text-xs text-destructive">{errors.personalIdCode.message}</p>}
            </div>
            <div>
              <label className="text-sm font-medium">National ID Number</label>
              <input {...register('nationalIdNumber')} className="mt-1 w-full rounded-md border bg-background px-3 py-2 text-sm" />
            </div>
          </div>
        </div>

        <div className="rounded-lg border bg-card p-6 space-y-4">
          <h3 className="font-semibold">Names</h3>
          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="text-sm font-medium">Last Name (Khmer) *</label>
              <input {...register('lastNameKh')} className="mt-1 w-full rounded-md border bg-background px-3 py-2 text-sm" placeholder="នាមត្រកូល" />
              {errors.lastNameKh && <p className="mt-1 text-xs text-destructive">{errors.lastNameKh.message}</p>}
            </div>
            <div>
              <label className="text-sm font-medium">First Name (Khmer) *</label>
              <input {...register('firstNameKh')} className="mt-1 w-full rounded-md border bg-background px-3 py-2 text-sm" placeholder="នាមខ្លួន" />
              {errors.firstNameKh && <p className="mt-1 text-xs text-destructive">{errors.firstNameKh.message}</p>}
            </div>
            <div>
              <label className="text-sm font-medium">Last Name (English)</label>
              <input {...register('lastNameEn')} className="mt-1 w-full rounded-md border bg-background px-3 py-2 text-sm" />
            </div>
            <div>
              <label className="text-sm font-medium">First Name (English)</label>
              <input {...register('firstNameEn')} className="mt-1 w-full rounded-md border bg-background px-3 py-2 text-sm" />
            </div>
          </div>
        </div>

        <div className="rounded-lg border bg-card p-6 space-y-4">
          <h3 className="font-semibold">Personal Details</h3>
          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="text-sm font-medium">Date of Birth</label>
              <input {...register('dateOfBirth')} type="date" className="mt-1 w-full rounded-md border bg-background px-3 py-2 text-sm" />
            </div>
            <div>
              <label className="text-sm font-medium">Gender</label>
              <select {...register('gender')} className="mt-1 w-full rounded-md border bg-background px-3 py-2 text-sm">
                <option value="">Select...</option>
                <option value="MALE">Male</option>
                <option value="FEMALE">Female</option>
                <option value="OTHER">Other</option>
              </select>
            </div>
          </div>
        </div>

        <div className="rounded-lg border bg-card p-6 space-y-4">
          <h3 className="font-semibold">Contact</h3>
          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="text-sm font-medium">Email</label>
              <input {...register('email')} type="email" className="mt-1 w-full rounded-md border bg-background px-3 py-2 text-sm" />
              {errors.email && <p className="mt-1 text-xs text-destructive">{errors.email.message}</p>}
            </div>
            <div>
              <label className="text-sm font-medium">Phone</label>
              <input {...register('phone')} type="tel" className="mt-1 w-full rounded-md border bg-background px-3 py-2 text-sm" placeholder="+855..." />
            </div>
          </div>
        </div>

        <div className="flex gap-3">
          <button type="submit" disabled={isSubmitting} className="rounded-md bg-primary px-6 py-2 text-sm font-medium text-primary-foreground hover:bg-primary/90 disabled:opacity-50">
            {isSubmitting ? 'Creating...' : 'Create Person'}
          </button>
          <Link href="/identity/persons" className="rounded-md border px-6 py-2 text-sm hover:bg-accent">Cancel</Link>
        </div>
      </form>
    </div>
  );
}
