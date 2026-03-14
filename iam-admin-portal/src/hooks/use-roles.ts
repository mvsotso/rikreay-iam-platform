"use client";

import { useSession } from "next-auth/react";
import type { Role } from "@/lib/constants";

export function useRoles() {
  const { data: session } = useSession();
  const roles = (session?.roles ?? []) as Role[];

  const hasRole = (role: Role) => roles.includes(role);
  const hasAnyRole = (...checkRoles: Role[]) =>
    checkRoles.some((role) => roles.includes(role));

  return { roles, hasRole, hasAnyRole };
}
