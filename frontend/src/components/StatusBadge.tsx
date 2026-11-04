import type { GameStatus } from "../types/game";
export default function StatusBadge({ status }: { status: GameStatus }) { return <span className={`badge ${status.toLowerCase()}`}>{status}</span>; }
