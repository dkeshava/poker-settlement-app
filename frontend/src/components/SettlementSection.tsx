import { useState } from "react";
import type { SettlementPreview, Settlement } from "../types/game";
import { formatCurrency, formatChips } from "../utils/formatters";
import { CheckCircleIcon, ArrowRightIcon, CopyIcon, ShieldIcon, CheckIcon } from "./Icons";

interface SettlementSectionProps {
  preview?: SettlementPreview;
  settlement?: Settlement;
  isSettled: boolean;
  isManager: boolean;
  busy: boolean;
  onFinalize: () => void;
}

export default function SettlementSection({
  preview,
  settlement,
  isSettled,
  isManager,
  busy,
  onFinalize,
}: SettlementSectionProps) {
  const [copiedIndex, setCopiedIndex] = useState<number | null>(null);
  const [allCopied, setAllCopied] = useState(false);

  const transfers = isSettled
    ? settlement?.transfers || []
    : preview?.transfers || [];

  const handleCopySingle = (from: string, to: string, amount: string | number, idx: number) => {
    const text = `${from} pays ${to} ${formatCurrency(amount)}`;
    void navigator.clipboard.writeText(text);
    setCopiedIndex(idx);
    setTimeout(() => setCopiedIndex(null), 2000);
  };

  const handleCopyAll = () => {
    if (!transfers.length) return;
    const lines = [
      "♠️ POKER SETTLEMENT PLAN ♠️",
      "-------------------------",
      ...transfers.map((t, idx) => `${idx + 1}. ${t.fromUsername} ➡️ pays ${formatCurrency(t.amount)} to ${t.toUsername}`),
      "-------------------------",
      "Please settle via UPI / Cash! 🪙",
    ];
    void navigator.clipboard.writeText(lines.join("\n"));
    setAllCopied(true);
    setTimeout(() => setAllCopied(false), 2500);
  };

  return (
    <section className="dashboard-section settlement-section">
      <div className="section-header">
        <div>
          <div className="section-title-wrap">
            <h2 className="section-title">
              {isSettled ? "Finalized Settlement Plan" : "Settlement Preview"}
            </h2>
            <span className={`status-pill ${isSettled ? "pill-settled" : "pill-preview"}`}>
              {isSettled ? "Permanent & Finalized" : "Preview Calculation"}
            </span>
          </div>
          <p className="section-sub">
            {isSettled
              ? "All payments have been permanently finalized and recorded."
              : "Algorithmically optimized to settle the table with the absolute minimum number of financial transfers."}
          </p>
        </div>

        {/* Action button */}
        {!isSettled && isManager && (
          <button
            type="button"
            className="btn btn-danger btn-finalize"
            onClick={onFinalize}
            disabled={busy}
          >
            <ShieldIcon size={16} />
            <span>{busy ? "Finalizing…" : "Finalize & Lock Settlement"}</span>
          </button>
        )}
      </div>

      {/* Net Balances Table (available during preview) */}
      {preview?.netBalances && preview.netBalances.length > 0 && (
        <div className="net-balances-wrap">
          <h3 className="subsection-title">Player P&L Summary</h3>
          <div className="table-responsive">
            <table className="poker-table">
              <thead>
                <tr>
                  <th>Player</th>
                  <th className="text-right">Total Buy-In</th>
                  <th className="text-right">Final Chips</th>
                  <th className="text-right">Net Chips</th>
                  <th className="text-right">Net Profit / Loss</th>
                </tr>
              </thead>
              <tbody>
                {preview.netBalances.map((nb) => {
                  const net = Number(nb.netAmount);
                  const isPositive = net > 0;
                  const isZero = net === 0;

                  return (
                    <tr key={nb.participantId}>
                      <td className="font-semibold">{nb.username}</td>
                      <td className="text-right font-mono">{formatChips(nb.totalBuyInChips)} chips</td>
                      <td className="text-right font-mono">{formatChips(nb.finalChipValue)} chips</td>
                      <td className="text-right font-mono">
                        <span className={isPositive ? "text-emerald" : isZero ? "text-gray" : "text-rose"}>
                          {nb.netChipValue > 0 ? "+" : ""}
                          {formatChips(nb.netChipValue)}
                        </span>
                      </td>
                      <td className="text-right font-mono font-bold">
                        <span className={isPositive ? "pnl-profit" : isZero ? "pnl-even" : "pnl-loss"}>
                          {isPositive ? "+" : ""}
                          {formatCurrency(nb.netAmount)}
                        </span>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {/* Transfers Section */}
      <div className="transfers-wrap">
        <div className="transfers-header">
          <div>
            <h3 className="subsection-title">
              Payment Transfers ({transfers.length})
            </h3>
            <p className="transfers-sub">
              {transfers.length === 0
                ? "All players broke even! No monetary transfers required."
                : "Exact peer-to-peer payments required to balance the game."}
            </p>
          </div>

          {transfers.length > 0 && (
            <button
              type="button"
              className="btn btn-outline btn-copy-all"
              onClick={handleCopyAll}
              title="Copy formatted settlement plan for WhatsApp group"
            >
              {allCopied ? <CheckIcon size={16} /> : <CopyIcon size={16} />}
              <span>{allCopied ? "Copied All!" : "Share to WhatsApp / Chat"}</span>
            </button>
          )}
        </div>

        {transfers.length > 0 ? (
          <div className="transfers-grid">
            {transfers.map((t, idx) => (
              <div key={idx} className="transfer-card">
                <div className="transfer-from">
                  <span className="transfer-role">Debtor</span>
                  <strong className="transfer-player-name">{t.fromUsername}</strong>
                </div>

                <div className="transfer-arrow-box">
                  <span className="transfer-amount font-mono">
                    {formatCurrency(t.amount)}
                  </span>
                  <div className="arrow-line">
                    <ArrowRightIcon size={16} className="text-emerald" />
                  </div>
                </div>

                <div className="transfer-to">
                  <span className="transfer-role">Creditor</span>
                  <strong className="transfer-player-name">{t.toUsername}</strong>
                </div>

                <button
                  type="button"
                  className="btn-copy-transfer"
                  title="Copy this transfer"
                  onClick={() => handleCopySingle(t.fromUsername, t.toUsername, t.amount, idx)}
                >
                  {copiedIndex === idx ? (
                    <span className="copied-tag">✓ Copied</span>
                  ) : (
                    <CopyIcon size={14} />
                  )}
                </button>
              </div>
            ))}
          </div>
        ) : (
          <div className="zero-transfers-box">
            <CheckCircleIcon size={32} className="text-emerald" />
            <p className="font-semibold">Everyone is completely settled!</p>
            <small>No transfers are required for this session.</small>
          </div>
        )}
      </div>
    </section>
  );
}
