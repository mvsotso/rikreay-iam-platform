import type { Role } from "@/lib/constants";

export interface UserProfile {
  id: string;
  username: string;
  email: string;
  firstName: string;
  lastName: string;
  roles: Role[];
  realmName?: string;
  memberClass?: string;
}
