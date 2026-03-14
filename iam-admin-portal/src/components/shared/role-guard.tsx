"use client";

import { useRoles } from "@/hooks/use-roles";
import type { Role } from "@/lib/constants";

interface RoleGuardProps {
  roles: Role[];
  children: React.ReactNode;
  fallback?: React.ReactNode;
}

export function RoleGuard({ roles, children, fallback = null }: RoleGuardProps) {
  const { hasAnyRole } = useRoles();

  if (!hasAnyRole(...roles)) {
    return <>{fallback}</>;
  }

  return <>{children}</>;
}
