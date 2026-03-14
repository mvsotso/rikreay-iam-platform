import { cn } from "@/lib/utils";

type Status = "UP" | "DOWN" | "DEGRADED" | "UNKNOWN" | "ACTIVE" | "INACTIVE" | "PENDING" | "RESOLVED" | "OPEN" | "CLOSED";

const statusStyles: Record<string, string> = {
  UP: "bg-green-100 text-green-800 dark:bg-green-900/30 dark:text-green-400",
  DOWN: "bg-red-100 text-red-800 dark:bg-red-900/30 dark:text-red-400",
  DEGRADED: "bg-yellow-100 text-yellow-800 dark:bg-yellow-900/30 dark:text-yellow-400",
  UNKNOWN: "bg-gray-100 text-gray-800 dark:bg-gray-900/30 dark:text-gray-400",
  ACTIVE: "bg-green-100 text-green-800 dark:bg-green-900/30 dark:text-green-400",
  INACTIVE: "bg-gray-100 text-gray-800 dark:bg-gray-900/30 dark:text-gray-400",
  PENDING: "bg-yellow-100 text-yellow-800 dark:bg-yellow-900/30 dark:text-yellow-400",
  RESOLVED: "bg-blue-100 text-blue-800 dark:bg-blue-900/30 dark:text-blue-400",
  OPEN: "bg-red-100 text-red-800 dark:bg-red-900/30 dark:text-red-400",
  CLOSED: "bg-gray-100 text-gray-800 dark:bg-gray-900/30 dark:text-gray-400",
};

export function StatusBadge({ status }: { status: Status | string }) {
  return (
    <span
      className={cn(
        "inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-medium",
        statusStyles[status] || statusStyles.UNKNOWN
      )}
    >
      {status}
    </span>
  );
}
