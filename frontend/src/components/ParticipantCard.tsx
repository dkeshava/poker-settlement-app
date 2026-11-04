import type { Participant, BuyIn } from "../types/game";
import { PlusIcon, TrashIcon, EditIcon } from "./Icons";
import { formatCurrency, formatChips, getInitials } from "../utils/formatters";

interface ParticipantCardProps {
  participant: Participant;
  chipConversionRate: number | string;
  isManager: boolean;
  canModifyBuyIns: boolean;
  canRemoveParticipant: boolean;
  onOpenAddBuyIn: (participant: Participant) => void;
  onEditBuyIn: (participant: Participant, buyIn: BuyIn) => void;
  onDeleteBuyIn: (participant: Participant, buyIn: BuyIn) => void;
  onRemoveParticipant: (participant: Participant) => void;
}

export default function ParticipantCard({
  participant,
  chipConversionRate,
  isManager,
  canModifyBuyIns,
  canRemoveParticipant,
  onOpenAddBuyIn,
  onEditBuyIn,
  onDeleteBuyIn,
  onRemoveParticipant,
}: ParticipantCardProps) {
  const rate = Number(chipConversionRate) || 1;
  const totalChips = participant.buyIns.reduce((sum, b) => sum + b.chipValue, 0);
  const totalMoney = totalChips * rate;

  return (
    <article className="participant-card">
      <div className="card-top">
        <div className="player-identity">
          <div className="player-avatar">
            {getInitials(participant.username)}
          </div>
          <div>
            <h4 className="player-name">{participant.username}</h4>
            <span className="player-sub">Player #{participant.participantId}</span>
          </div>
        </div>

        {isManager && canRemoveParticipant && participant.buyIns.length === 0 && (
          <button
            type="button"
            className="btn-icon-danger"
            title={`Remove ${participant.username}`}
            onClick={() => onRemoveParticipant(participant)}
          >
            <TrashIcon size={15} />
          </button>
        )}
      </div>

      <div className="player-stats-grid">
        <div className="stat-tile">
          <span className="stat-label">Total Chips</span>
          <span className="stat-value chips-value">
            🪙 {formatChips(totalChips)}
          </span>
        </div>
        <div className="stat-tile">
          <span className="stat-label">Total Cash</span>
          <span className="stat-value cash-value">
            {formatCurrency(totalMoney)}
          </span>
        </div>
      </div>

      {/* Buy-Ins List */}
      <div className="buyins-container">
        <div className="buyins-header">
          <span className="buyins-title">
            Buy-Ins ({participant.buyIns.length}):
          </span>
          {isManager && canModifyBuyIns && (
            <button
              type="button"
              className="btn-quick-buyin"
              onClick={() => onOpenAddBuyIn(participant)}
            >
              <PlusIcon size={13} />
              <span>Add Buy-In</span>
            </button>
          )}
        </div>

        {participant.buyIns.length > 0 ? (
          <div className="buyins-pills-list">
            {participant.buyIns.map((buyIn, idx) => (
              <div key={buyIn.buyInId} className="buyin-pill">
                <span className="buyin-pill-label">#{idx + 1}:</span>
                <span className="buyin-pill-value font-mono">
                  {formatChips(buyIn.chipValue)} chips
                </span>
                {isManager && canModifyBuyIns && (
                  <div className="buyin-pill-actions">
                    <button
                      type="button"
                      className="pill-action-btn edit-btn"
                      title="Edit this buy-in"
                      onClick={() => onEditBuyIn(participant, buyIn)}
                    >
                      <EditIcon size={12} />
                    </button>
                    <button
                      type="button"
                      className="pill-action-btn delete-btn"
                      title="Delete this buy-in"
                      onClick={() => onDeleteBuyIn(participant, buyIn)}
                    >
                      <TrashIcon size={12} />
                    </button>
                  </div>
                )}
              </div>
            ))}
          </div>
        ) : (
          <div className="no-buyins-box">
            <span>No buy-ins yet. Every player must have at least one buy-in to start.</span>
          </div>
        )}
      </div>
    </article>
  );
}
