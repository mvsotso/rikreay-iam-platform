"use client";

import { useSession } from "next-auth/react";
import { Moon, Sun, Globe, UserCircle } from "lucide-react";
import { useTheme } from "next-themes";
import { useUiStore } from "@/stores/ui-store";
import { cn } from "@/lib/utils";

export function TopBar() {
  const { data: session } = useSession();
  const { theme, setTheme } = useTheme();
  const { sidebarCollapsed, locale, setLocale } = useUiStore();

  return (
    <header
      className={cn(
        "sticky top-0 z-40 flex h-16 items-center justify-between border-b bg-background/95 backdrop-blur supports-[backdrop-filter]:bg-background/60 px-6 transition-all duration-300",
        sidebarCollapsed ? "ml-16" : "ml-64"
      )}
    >
      <div className="flex items-center gap-4">
        <h2 className="text-lg font-semibold">IAM Platform</h2>
      </div>

      <div className="flex items-center gap-2">
        {/* Locale toggle */}
        <button
          onClick={() => setLocale(locale === "en" ? "km" : "en")}
          className="flex items-center gap-1.5 rounded-md px-2.5 py-1.5 text-sm text-muted-foreground hover:bg-accent hover:text-accent-foreground transition-colors"
          title={locale === "en" ? "Switch to Khmer" : "Switch to English"}
        >
          <Globe className="h-4 w-4" />
          <span className="uppercase font-medium">{locale}</span>
        </button>

        {/* Theme toggle */}
        <button
          onClick={() => setTheme(theme === "dark" ? "light" : "dark")}
          className="rounded-md p-2 text-muted-foreground hover:bg-accent hover:text-accent-foreground transition-colors"
          title="Toggle theme"
        >
          {theme === "dark" ? (
            <Sun className="h-5 w-5" />
          ) : (
            <Moon className="h-5 w-5" />
          )}
        </button>

        {/* User info */}
        <div className="flex items-center gap-2 rounded-md px-3 py-1.5 text-sm">
          <UserCircle className="h-5 w-5 text-muted-foreground" />
          <div className="flex flex-col">
            <span className="font-medium leading-tight">
              {session?.user?.name || "User"}
            </span>
            <span className="text-xs text-muted-foreground leading-tight">
              {(session?.roles as string[])?.slice(0, 2).join(", ")}
            </span>
          </div>
        </div>
      </div>
    </header>
  );
}
