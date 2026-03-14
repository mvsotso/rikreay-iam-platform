import { Sidebar } from "@/components/layout/sidebar";
import { MobileSidebar } from "@/components/layout/mobile-sidebar";
import { TopBar } from "@/components/layout/top-bar";
import { Breadcrumbs } from "@/components/layout/breadcrumbs";
import { CommandPalette } from "@/components/shared/command-palette";

export default function PortalLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <div className="min-h-screen bg-background">
      {/* Desktop sidebar — hidden on mobile */}
      <div className="hidden md:block">
        <Sidebar />
      </div>

      {/* Mobile sidebar — visible on small screens */}
      <MobileSidebar />

      {/* Command palette (Cmd+K / Ctrl+K) */}
      <CommandPalette />

      <TopBar />
      <main className="md:ml-64 pt-16 transition-all duration-300">
        <div className="container mx-auto max-w-7xl px-4 md:px-6 py-6">
          <Breadcrumbs />
          {children}
        </div>
      </main>
    </div>
  );
}
