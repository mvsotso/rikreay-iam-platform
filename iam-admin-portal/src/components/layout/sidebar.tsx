"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { ChevronLeft, LogOut } from "lucide-react";
import { signOut } from "next-auth/react";
import { cn } from "@/lib/utils";
import { useUiStore } from "@/stores/ui-store";
import { useRoles } from "@/hooks/use-roles";
import { getVisibleNavigation } from "@/config/navigation";

export function Sidebar() {
  const pathname = usePathname();
  const { sidebarCollapsed, toggleSidebar } = useUiStore();
  const { roles } = useRoles();
  const sections = getVisibleNavigation(roles);

  return (
    <aside
      className={cn(
        "fixed inset-y-0 left-0 z-50 flex flex-col border-r bg-sidebar text-sidebar-foreground transition-all duration-300",
        sidebarCollapsed ? "w-16" : "w-64"
      )}
    >
      {/* Logo */}
      <div className="flex h-16 items-center justify-between border-b border-sidebar-border px-4">
        {!sidebarCollapsed && (
          <Link href="/dashboard" className="flex items-center gap-2">
            <div className="flex h-8 w-8 items-center justify-center rounded-lg bg-primary text-primary-foreground font-bold text-sm">
              R
            </div>
            <span className="text-lg font-bold">RikReay</span>
          </Link>
        )}
        <button
          onClick={toggleSidebar}
          className="rounded-md p-1.5 hover:bg-accent transition-colors"
          aria-label="Toggle sidebar"
        >
          <ChevronLeft
            className={cn(
              "h-5 w-5 transition-transform",
              sidebarCollapsed && "rotate-180"
            )}
          />
        </button>
      </div>

      {/* Navigation */}
      <nav className="flex-1 overflow-y-auto py-4">
        {sections.map((section) => (
          <div key={section.title} className="mb-4">
            {!sidebarCollapsed && (
              <p className="mb-1 px-4 text-xs font-semibold uppercase tracking-wider text-muted-foreground">
                {section.title}
              </p>
            )}
            {section.items.map((item) => {
              const isActive =
                pathname === item.href ||
                (item.href !== "/dashboard" && pathname.startsWith(item.href));
              const Icon = item.icon;
              return (
                <Link
                  key={item.href}
                  href={item.href}
                  className={cn(
                    "flex items-center gap-3 px-4 py-2 text-sm transition-colors",
                    isActive
                      ? "bg-primary/10 text-primary font-medium border-r-2 border-primary"
                      : "hover:bg-accent hover:text-accent-foreground",
                    sidebarCollapsed && "justify-center px-2"
                  )}
                  title={sidebarCollapsed ? item.title : undefined}
                >
                  <Icon className="h-5 w-5 shrink-0" />
                  {!sidebarCollapsed && <span>{item.title}</span>}
                </Link>
              );
            })}
          </div>
        ))}
      </nav>

      {/* Sign out */}
      <div className="border-t border-sidebar-border p-4">
        <button
          onClick={() => signOut({ callbackUrl: "/login" })}
          className={cn(
            "flex items-center gap-3 rounded-md px-3 py-2 text-sm text-muted-foreground hover:bg-accent hover:text-accent-foreground transition-colors w-full",
            sidebarCollapsed && "justify-center px-2"
          )}
        >
          <LogOut className="h-5 w-5 shrink-0" />
          {!sidebarCollapsed && <span>Sign out</span>}
        </button>
      </div>
    </aside>
  );
}
