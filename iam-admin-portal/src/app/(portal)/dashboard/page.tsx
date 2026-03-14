'use client';

import { useRoles } from '@/hooks/use-roles';
import { Roles } from '@/lib/constants';
import { PlatformDashboard } from './_components/platform-dashboard';
import { OpsDashboard } from './_components/ops-dashboard';
import { OrgDashboard } from './_components/org-dashboard';
import { SectorDashboard } from './_components/sector-dashboard';
import { DefaultDashboard } from './_components/default-dashboard';

export default function DashboardPage() {
  const { hasRole } = useRoles();

  if (hasRole(Roles.IAM_ADMIN)) return <PlatformDashboard />;
  if (hasRole(Roles.OPS_ADMIN)) return <OpsDashboard />;
  if (hasRole(Roles.SECTOR_ADMIN)) return <SectorDashboard />;
  if (hasRole(Roles.TENANT_ADMIN)) return <OrgDashboard />;
  return <DefaultDashboard />;
}
