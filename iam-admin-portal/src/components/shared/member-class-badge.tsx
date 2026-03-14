import { cn } from "@/lib/utils";

const classStyles: Record<string, string> = {
  GOV: "bg-blue-100 text-blue-800 dark:bg-blue-900/30 dark:text-blue-400",
  COM: "bg-emerald-100 text-emerald-800 dark:bg-emerald-900/30 dark:text-emerald-400",
  NGO: "bg-purple-100 text-purple-800 dark:bg-purple-900/30 dark:text-purple-400",
  MUN: "bg-orange-100 text-orange-800 dark:bg-orange-900/30 dark:text-orange-400",
};

export function MemberClassBadge({ memberClass }: { memberClass: string }) {
  return (
    <span
      className={cn(
        "inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-bold",
        classStyles[memberClass] || "bg-gray-100 text-gray-800"
      )}
    >
      {memberClass}
    </span>
  );
}
