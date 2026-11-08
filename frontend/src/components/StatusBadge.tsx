import type { GameStatus } from "../types/game";

interface StatusBadgeProps {
  status: GameStatus;
  roundActive?: boolean;
}

const STATUS_CONFIG: Record<GameStatus, { label: string; className: string; dotClass: string }> = {
  OPEN: {
    label: "Open / Registration",
    className: "badge-open",
    dotClass: "dot-open",
  },
  RUNNING: {
    label: "In Progress",
    className: "badge-running",
    dotClass: "dot-running",
  },
  ENDED: {
    label: "Game Ended",
    className: "badge-ended",
    dotClass: "dot-ended",
  },
  SETTLED: {
    label: "Settled",
    className: "badge-settled",
    dotClass: "dot-settled",
  },
};

export default function StatusBadge({ status, roundActive }: StatusBadgeProps) {
  const config = STATUS_CONFIG[status] || STATUS_CONFIG.OPEN;

  return (
    <span className={`status-badge ${config.className}`}>
      <span className={`status-dot ${config.dotClass}`} />
      <span>{config.label}</span>
      {status === "RUNNING" && (
        <span className="status-round-tag">
          {roundActive ? "• Round Active" : "• Between Rounds"}
        </span>
      )}
    </span>
  );
}
