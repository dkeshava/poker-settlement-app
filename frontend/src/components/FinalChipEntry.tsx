import { useState, useEffect } from "react";
import type { Participant } from "../types/game";
import { CheckCircleIcon, AlertCircleIcon } from "./Icons";
import { formatChips, formatCurrency } from "../utils/formatters";

interface FinalChipEntryProps {
  participants: Participant[];
  chipConversionRate: number | string;
  isManager: boolean;
  busy: boolean;
  onSave: (values: { participantId: number; chipValue: number }[]) => void;
}

export default function FinalChipEntry({
  participants,
  chipConversionRate,
  isManager,
  busy,
  onSave,
}: FinalChipEntryProps) {
  const [chipValues, setChipValues] = useState<Record<number, string>>(() => {
    return Object.fromEntries(
      participants.map((p) => [
        p.participantId,
        p.finalChipValue !== null && p.finalChipValue !== undefined
          ? p.finalChipValue.toString()
          : "",
      ])
    );
  });
  const [validationError, setValidationError] = useState<string>("");

  useEffect(() => {
    setChipValues(
      Object.fromEntries(
        participants.map((p) => [
          p.participantId,
          p.finalChipValue !== null && p.finalChipValue !== undefined
            ? p.finalChipValue.toString()
            : "",
        ])
      )
    );
  }, [participants]);

  const rate = Number(chipConversionRate) || 1;

  // Total buy-ins recorded
  const totalBuyInChips = participants.reduce(
    (acc, p) => acc + p.buyIns.reduce((sum, b) => sum + b.chipValue, 0),
    0
  );

  // Total final chips entered
  let totalFinalChips = 0;
  let allEntered = true;

  for (const p of participants) {
    const val = chipValues[p.participantId];
    if (val === "" || val === undefined) {
      allEntered = false;
    } else {
      const parsed = parseInt(val, 10);
      if (!isNaN(parsed) && parsed >= 0) {
        totalFinalChips += parsed;
      } else {
        allEntered = false;
      }
    }
  }

  const chipsDifference = totalFinalChips - totalBuyInChips;
  const isBalanced = allEntered && chipsDifference === 0;

  const handleInputChange = (participantId: number, value: string) => {
    setChipValues((prev) => ({ ...prev, [participantId]: value }));
    setValidationError("");
  };

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();

    // Check all participants have values
    const entries = participants.map((p) => {
      const raw = chipValues[p.participantId];
      const parsed = parseInt(raw, 10);
      return {
        participantId: p.participantId,
        chipValue: parsed,
        raw,
      };
    });

    if (entries.some((e) => e.raw === "" || isNaN(e.chipValue) || e.chipValue < 0)) {
      setValidationError("Please enter a valid chip count for every player. A value of 0 is valid.");
      return;
    }

    if (chipsDifference !== 0) {
      setValidationError(
        `Total final chips (${totalFinalChips}) must exactly equal total buy-in chips (${totalBuyInChips}). Difference: ${chipsDifference > 0 ? "+" : ""}${chipsDifference} chips.`
      );
      return;
    }

    onSave(entries.map((e) => ({ participantId: e.participantId, chipValue: e.chipValue })));
  };

  return (
    <section className="dashboard-section final-chips-section">
      <div className="section-header">
        <div>
          <h2 className="section-title">Final Chip Counts</h2>
          <p className="section-sub">
            Count all player chips remaining on the table. The sum of all final chips must match the total chips bought in.
          </p>
        </div>
      </div>

      {validationError && (
        <div className="alert alert-error">
          <AlertCircleIcon size={18} />
          <span>{validationError}</span>
        </div>
      )}

      {/* Balance Tracker Banner */}
      <div className={`chips-balance-banner ${isBalanced ? "banner-balanced" : "banner-unbalanced"}`}>
        <div className="balance-metrics">
          <div className="balance-item">
            <span className="balance-item-label">Total Buy-In Chips:</span>
            <span className="balance-item-val font-mono">{formatChips(totalBuyInChips)}</span>
          </div>
          <div className="balance-arrow">vs</div>
          <div className="balance-item">
            <span className="balance-item-label">Entered Final Chips:</span>
            <span className="balance-item-val font-mono">{formatChips(totalFinalChips)}</span>
          </div>
        </div>

        <div className="balance-status-badge">
          {isBalanced ? (
            <div className="badge-balanced">
              <CheckCircleIcon size={18} />
              <span>Table Balanced (All chips accounted for)</span>
            </div>
          ) : (
            <div className="badge-difference">
              <AlertCircleIcon size={18} />
              <span>
                Difference: {chipsDifference > 0 ? `+${chipsDifference}` : chipsDifference} chips
                {!allEntered && " (Some player counts missing)"}
              </span>
            </div>
          )}
        </div>
      </div>

      {/* Form */}
      <form onSubmit={handleSubmit} className="final-chips-form">
        <div className="final-chips-grid">
          {participants.map((p) => {
            const playerBuyIn = p.buyIns.reduce((sum, b) => sum + b.chipValue, 0);
            const rawVal = chipValues[p.participantId] ?? "";
            const numVal = parseInt(rawVal, 10);
            const hasVal = rawVal !== "" && !isNaN(numVal) && numVal >= 0;
            const netChips = hasVal ? numVal - playerBuyIn : null;

            return (
              <div key={p.participantId} className="final-player-card">
                <div className="final-player-header">
                  <span className="final-player-name">{p.username}</span>
                  <span className="final-player-buyin font-mono">
                    Buy-In: {formatChips(playerBuyIn)} chips
                  </span>
                </div>

                <div className="final-input-wrap">
                  <label htmlFor={`final-chips-${p.participantId}`} className="sr-only">
                    Final chips for {p.username}
                  </label>
                  {isManager ? (
                    <div className="chip-input-container">
                      <span className="chip-input-prefix">🪙</span>
                      <input
                        id={`final-chips-${p.participantId}`}
                        type="number"
                        min="0"
                        step="1"
                        placeholder="0"
                        className="input final-chip-input font-mono"
                        value={rawVal}
                        onChange={(e) => handleInputChange(p.participantId, e.target.value)}
                        disabled={busy}
                      />
                    </div>
                  ) : (
                    <div className="final-readonly-display font-mono">
                      {hasVal ? `${formatChips(numVal)} chips` : "Not entered yet"}
                    </div>
                  )}
                </div>

                {hasVal && netChips !== null && (
                  <div className={`net-chip-indicator ${netChips >= 0 ? "net-positive" : "net-negative"}`}>
                    <span>
                      {netChips >= 0 ? "+" : ""}
                      {formatChips(netChips)} chips
                    </span>
                    <span className="net-money-val">
                      ({netChips >= 0 ? "+" : ""}
                      {formatCurrency(netChips * rate)})
                    </span>
                  </div>
                )}
              </div>
            );
          })}
        </div>

        {isManager && (
          <div className="final-form-actions">
            <button
              type="submit"
              className="btn btn-primary btn-save-finals"
              disabled={busy || !isBalanced}
            >
              <CheckCircleIcon size={18} />
              <span>{busy ? "Saving…" : "Save Final Chips & Generate Preview"}</span>
            </button>
          </div>
        )}
      </form>
    </section>
  );
}
