import { useState } from "react";
import type { SettlementPreview, Settlement, Participant } from "../types/game";
import { formatCurrency, formatChips } from "../utils/formatters";
import { CheckCircleIcon, ArrowRightIcon, CopyIcon, ShieldIcon, CheckIcon } from "./Icons";

interface SettlementSectionProps {
  preview?: SettlementPreview;
  settlement?: Settlement;
  participants: Participant[];
  chipConversionRate: number | string;
  isSettled: boolean;
  isManager: boolean;
  busy: boolean;
  onFinalize: () => void;
}

export default function SettlementSection({
  preview,
  settlement,
  participants,
  chipConversionRate,
  isSettled,
  isManager,
  busy,
  onFinalize,
}: SettlementSectionProps) {
  const [copiedIndex, setCopiedIndex] = useState<number | null>(null);
  const [allCopied, setAllCopied] = useState(false);

  const rate = Number(chipConversionRate) || 1;

  const transfers = isSettled
    ? settlement?.transfers || []
    : preview?.transfers || [];

  // Compute player ledger scorecard for both preview and settled state
  const playerBalances = participants.map((p) => {
    const totalBuyInChips = p.buyIns.reduce((sum, b) => sum + b.chipValue, 0);
    const finalChips = p.finalChipValue ?? 0;
    const netChips = finalChips - totalBuyInChips;
    const netAmount = netChips * rate;
    return {
      participantId: p.participantId,
      username: p.username,
      totalBuyInChips,
      finalChipValue: finalChips,
      netChipValue: netChips,
      netAmount,
    };
  });

  // Calculate session metrics
  const totalSessionChips = playerBalances.reduce((sum, p) => sum + p.totalBuyInChips, 0);
  const totalSessionPot = totalSessionChips * rate;

  // Find top winner and biggest loser
  const sortedByProfit = [...playerBalances].sort((a, b) => b.netAmount - a.netAmount);
  const topWinner = sortedByProfit.length > 0 && sortedByProfit[0].netAmount > 0 ? sortedByProfit[0] : null;

  const handleCopySingle = (from: string, to: string, amount: string | number, idx: number) => {
    const text = `${from} pays ${to} ${formatCurrency(amount)}`;
    void navigator.clipboard.writeText(text);
    setCopiedIndex(idx);
    setTimeout(() => setCopiedIndex(null), 2000);
  };

  const handleCopyAll = () => {
    if (!transfers.length && !playerBalances.length) return;

    const lines = [
      "♠️ POKER SESSION SETTLEMENT ♠️",
      "=============================",
      `🪙 Total Table Pot: ${formatCurrency(totalSessionPot)} (${formatChips(totalSessionChips)} chips)`,
      `Rate: ${formatCurrency(rate)} / chip`,
      "",
      "📊 PLAYER SCORECARD (P&L):",
      "-----------------------------",
      ...playerBalances.map((p) => {
        const sign = p.netAmount > 0 ? "+" : "";
        return `• ${p.username}: Buy-in ${formatChips(p.totalBuyInChips)} chips ➡️ Final ${formatChips(p.finalChipValue)} chips (${sign}${formatCurrency(p.netAmount)})`;
      }),
      "",
      "💸 TRANSFERS (WHO PAYS WHOM):",
      "-----------------------------",
      ...(transfers.length > 0
        ? transfers.map((t, idx) => `${idx + 1}. ${t.fromUsername} ➡️ pays ${formatCurrency(t.amount)} to ${t.toUsername}`)
        : ["All players broke even! No payments needed."]),
      "=============================",
      "Settle up promptly via UPI / Cash! 🥂",
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
              {isSettled ? "Finalized Settlement & Scorecard" : "Settlement Preview & Scorecard"}
            </h2>
            <span className={`status-pill ${isSettled ? "pill-settled" : "pill-preview"}`}>
              {isSettled ? "Permanent Ledger" : "Preview Calculation"}
            </span>
          </div>
          <p className="section-sub">
            {isSettled
              ? "Session completed. Total buy-ins, final chip counts, and settlement transactions are permanently recorded."
              : "Review the full session ledger and optimized minimum transfers before finalizing."}
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

      {/* Session Highlights Banner */}
      <div className="session-highlights-bar">
        <div className="highlight-item">
          <span className="highlight-label">Total Session Pot</span>
          <span className="highlight-value text-emerald font-mono">
            {formatCurrency(totalSessionPot)}
          </span>
        </div>
        <div className="highlight-item">
          <span className="highlight-label">Total Chips in Play</span>
          <span className="highlight-value font-mono">
            🪙 {formatChips(totalSessionChips)} chips
          </span>
        </div>
        {topWinner && (
          <div className="highlight-item highlight-mvp">
            <span className="highlight-label">🏆 Top Winner (MVP)</span>
            <span className="highlight-value text-amber">
              {topWinner.username} (+{formatCurrency(topWinner.netAmount)})
            </span>
          </div>
        )}
      </div>

      {/* Permanent Player Ledger & P&L Scorecard */}
      <div className="net-balances-wrap">
        <div className="scorecard-title-row">
          <h3 className="subsection-title">
            Session Scorecard (Total Buy-Ins & Final Chips)
          </h3>
          <span className="scorecard-note">
            Rate: {formatCurrency(rate)} per chip
          </span>
        </div>

        <div className="table-responsive">
          <table className="poker-table">
            <thead>
              <tr>
                <th>Player</th>
                <th className="text-right">Total Buy-In</th>
                <th className="text-right">Final Cash-Out</th>
                <th className="text-right">Net Chips</th>
                <th className="text-right">Net Profit / Loss</th>
              </tr>
            </thead>
            <tbody>
              {playerBalances.map((nb) => {
                const net = nb.netAmount;
                const isPositive = net > 0;
                const isZero = net === 0;
                const buyInCash = nb.totalBuyInChips * rate;
                const finalCash = nb.finalChipValue * rate;

                return (
                  <tr key={nb.participantId}>
                    <td className="font-semibold player-cell">
                      <span>{nb.username}</span>
                      {topWinner?.participantId === nb.participantId && (
                        <span className="winner-tag" title="Top Winner">🏆</span>
                      )}
                    </td>
                    <td className="text-right font-mono">
                      <div className="cell-split">
                        <span>{formatChips(nb.totalBuyInChips)} chips</span>
                        <small className="text-dim">({formatCurrency(buyInCash)})</small>
                      </div>
                    </td>
                    <td className="text-right font-mono">
                      <div className="cell-split">
                        <span className="font-bold">{formatChips(nb.finalChipValue)} chips</span>
                        <small className="text-dim">({formatCurrency(finalCash)})</small>
                      </div>
                    </td>
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
                : "Exact peer-to-peer transfers required to settle the table."}
            </p>
          </div>

          <button
            type="button"
            className="btn btn-outline btn-copy-all"
            onClick={handleCopyAll}
            title="Copy complete session scorecard & settlements to WhatsApp"
          >
            {allCopied ? <CheckIcon size={16} /> : <CopyIcon size={16} />}
            <span>{allCopied ? "Copied Scorecard!" : "Share to WhatsApp / Chat"}</span>
          </button>
        </div>

        {transfers.length > 0 ? (
          <div className="transfers-grid">
            {transfers.map((t, idx) => {
              const upiPayUrl = `upi://pay?pn=${encodeURIComponent(t.toUsername)}&am=${t.amount}&cu=INR&tn=Poker%20Settlement`;

              return (
                <div key={idx} className="transfer-card">
                  <div className="transfer-from">
                    <span className="transfer-role">Debtor (Pays)</span>
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
                    <span className="transfer-role">Creditor (Receives)</span>
                    <strong className="transfer-player-name">{t.toUsername}</strong>
                  </div>

                  <div className="transfer-card-actions">
                    <a
                      href={upiPayUrl}
                      className="btn-upi-pay"
                      title={`Pay ${t.toUsername} ${formatCurrency(t.amount)} via UPI`}
                    >
                      UPI
                    </a>
                    <button
                      type="button"
                      className="btn-copy-transfer"
                      title="Copy transfer text"
                      onClick={() => handleCopySingle(t.fromUsername, t.toUsername, t.amount, idx)}
                    >
                      {copiedIndex === idx ? (
                        <span className="copied-tag">✓</span>
                      ) : (
                        <CopyIcon size={14} />
                      )}
                    </button>
                  </div>
                </div>
              );
            })}
          </div>
        ) : (
          <div className="zero-transfers-box">
            <CheckCircleIcon size={32} className="text-emerald" />
            <p className="font-semibold">Everyone is completely settled!</p>
            <small>No peer-to-peer transfers are required for this session.</small>
          </div>
        )}
      </div>
    </section>
  );
}
