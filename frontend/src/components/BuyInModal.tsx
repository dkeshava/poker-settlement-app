import { useState } from "react";
import { ChipIcon, XIcon } from "./Icons";
import { formatCurrency, formatChips } from "../utils/formatters";

interface BuyInModalProps {
  participantName: string;
  chipConversionRate: number | string;
  initialValue?: number;
  isEdit?: boolean;
  busy?: boolean;
  onSave: (chipValue: number) => void;
  onClose: () => void;
}

const PRESET_CHIPS = [100, 200, 500, 1000, 2000, 5000];

export default function BuyInModal({
  participantName,
  chipConversionRate,
  initialValue,
  isEdit = false,
  busy = false,
  onSave,
  onClose,
}: BuyInModalProps) {
  const [chipAmount, setChipAmount] = useState<string>(
    initialValue ? initialValue.toString() : "500"
  );
  const [error, setError] = useState<string>("");

  const rate = Number(chipConversionRate) || 1;
  const numericChips = parseInt(chipAmount, 10) || 0;
  const cashEquivalent = numericChips * rate;

  const handleSetPreset = (preset: number) => {
    setChipAmount(preset.toString());
    setError("");
  };

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    const value = parseInt(chipAmount, 10);
    if (isNaN(value) || value <= 0) {
      setError("Please enter a positive whole number of chips.");
      return;
    }
    onSave(value);
  };

  return (
    <div className="modal-backdrop" onClick={onClose}>
      <div
        className="modal buyin-modal"
        onClick={(e) => e.stopPropagation()}
        role="dialog"
        aria-modal="true"
      >
        <div className="modal-header">
          <div className="modal-title-wrap">
            <div className="modal-icon-chip">
              <ChipIcon size={20} className="text-emerald" />
            </div>
            <div>
              <h2 className="modal-title">
                {isEdit ? "Edit Buy-In" : "Record Buy-In"}
              </h2>
              <p className="modal-sub">
                Player: <strong className="text-white">{participantName}</strong>
              </p>
            </div>
          </div>
          <button
            type="button"
            className="btn-icon-close"
            onClick={onClose}
            disabled={busy}
          >
            <XIcon size={18} />
          </button>
        </div>

        <form onSubmit={handleSubmit} className="modal-body">
          {error && <div className="alert alert-error">{error}</div>}

          <div className="form-group">
            <label className="input-label" htmlFor="chipCountInput">
              Chip Count (Chips to Issue)
            </label>
            <div className="chip-input-container">
              <span className="chip-input-prefix">🪙 Chips</span>
              <input
                id="chipCountInput"
                type="number"
                min="1"
                step="1"
                autoFocus
                className="input chip-input"
                placeholder="e.g. 500"
                value={chipAmount}
                onChange={(e) => {
                  setChipAmount(e.target.value);
                  setError("");
                }}
                required
                disabled={busy}
              />
            </div>
          </div>

          {/* Real-time cash value conversion preview */}
          <div className="conversion-preview-card">
            <div className="conversion-row">
              <span className="conversion-label">Chips:</span>
              <span className="conversion-val font-mono">{formatChips(numericChips)} chips</span>
            </div>
            <div className="conversion-row">
              <span className="conversion-label">Cash Value:</span>
              <span className="conversion-val cash-highlight font-mono">
                {formatCurrency(cashEquivalent)}
              </span>
            </div>
            <div className="conversion-note">
              Rate: {formatCurrency(rate)} per chip
            </div>
          </div>

          {/* Quick preset chips */}
          <div className="preset-chips-section">
            <span className="preset-title">Quick Chip Presets:</span>
            <div className="preset-buttons-grid">
              {PRESET_CHIPS.map((preset) => (
                <button
                  key={preset}
                  type="button"
                  className="btn-preset"
                  onClick={() => handleSetPreset(preset)}
                  disabled={busy}
                >
                  +{formatChips(preset)}
                </button>
              ))}
            </div>
          </div>

          <div className="modal-actions">
            <button
              type="button"
              className="btn btn-ghost"
              onClick={onClose}
              disabled={busy}
            >
              Cancel
            </button>
            <button
              type="submit"
              className="btn btn-primary"
              disabled={busy || numericChips <= 0}
            >
              <ChipIcon size={16} />
              <span>{busy ? "Saving…" : isEdit ? "Update Buy-In" : "Confirm Buy-In"}</span>
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
