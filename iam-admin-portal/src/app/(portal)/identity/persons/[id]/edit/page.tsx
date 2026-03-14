'use client';

import { useParams, useRouter } from 'next/navigation';
import Link from 'next/link';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { usePerson, useUpdatePerson } from '@/services/identity';
import { PageHeader } from '@/components/layout/page-header';
import { ArrowLeft } from 'lucide-react';
import { toast } from 'sonner';
import { useEffect } from 'react';

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

export default function EditPersonPage() {
  const { id } = useParams<{ id: string }>();
  const router = useRouter();
  const { data: person, isLoading } = usePerson(id);
  const updatePerson = useUpdatePerson(id);
  const { register, handleSubmit, reset, formState: { errors, isSubmitting } } = useForm<PersonFormValues>({
    resolver: zodResolver(personSchema),
  });

  useEffect(() => {
    if (person) {
      reset({
        personalIdCode: person.personalIdCode,
        nationalIdNumber: person.nationalIdNumber ?? '',
        firstNameKh: person.firstNameKh,
        lastNameKh: person.lastNameKh,
        firstNameEn: person.firstNameEn ?? '',
        lastNameEn: person.lastNameEn ?? '',
        dateOfBirth: person.dateOfBirth ?? '',
        gender: person.gender ?? undefined,
        email: person.email ?? '',
        phone: person.phone ?? '',
      });
    }
  }, [person, reset]);

  const onSubmit = async (data: PersonFormValues) => {
    try {
      await updatePerson.mutateAsync({ ...data, email: data.email || undefined });
      toast.success('Person updated');
      router.push(`/identity/persons/${id}`);
    } catch {
      toast.error('Failed to update person');
    }
  };

  if (isLoading) return <div className="h-96 rounded-lg bg-muted animate-pulse" />;

  return (
    <div className="space-y-6">
      <PageHeader
        title={`Edit: ${person?.lastNameKh ?? ''} ${person?.firstNameKh ?? ''}`}
        description="Update person information"
        actions={
          <Link href={`/identity/persons/${id}`} className="inline-flex items-center gap-2 rounded-md border px-3 py-2 text-sm hover:bg-accent">
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
              <input {...register('personalIdCode')} className="mt-1 w-full rounded-md border bg-background px-3 py-2 text-sm" />
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
              <input {...register('lastNameKh')} className="mt-1 w-full rounded-md border bg-background px-3 py-2 text-sm" />
              {errors.lastNameKh && <p className="mt-1 text-xs text-destructive">{errors.lastNameKh.message}</p>}
            </div>
            <div>
              <label className="text-sm font-medium">First Name (Khmer) *</label>
              <input {...register('firstNameKh')} className="mt-1 w-full rounded-md border bg-background px-3 py-2 text-sm" />
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
              <input {...register('phone')} type="tel" className="mt-1 w-full rounded-md border bg-background px-3 py-2 text-sm" />
            </div>
          </div>
        </div>

        <div className="flex gap-3">
          <button type="submit" disabled={isSubmitting} className="rounded-md bg-primary px-6 py-2 text-sm font-medium text-primary-foreground hover:bg-primary/90 disabled:opacity-50">
            {isSubmitting ? 'Saving...' : 'Save Changes'}
          </button>
          <Link href={`/identity/persons/${id}`} className="rounded-md border px-6 py-2 text-sm hover:bg-accent">Cancel</Link>
        </div>
      </form>
    </div>
  );
}
