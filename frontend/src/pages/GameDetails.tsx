import { useCallback, useEffect, useMemo, useState } from "react";
import { Link, useParams, useNavigate } from "react-router-dom";
import * as api from "../api/gameApi";
import Navbar from "../components/Navbar";
import StatusBadge from "../components/StatusBadge";
import ConfirmDialog from "../components/ConfirmDialog";
import UserAutocomplete from "../components/UserAutocomplete";
import BuyInModal from "../components/BuyInModal";
import ParticipantCard from "../components/ParticipantCard";
import FinalChipEntry from "../components/FinalChipEntry";
import SettlementSection from "../components/SettlementSection";
import {
  PlayIcon,
  FlagIcon,
  TrashIcon,
  UsersIcon,
  ShieldIcon,
  CheckCircleIcon,
  AlertCircleIcon,
  ClockIcon,
} from "../components/Icons";
import type { Game, Participant, BuyIn, SettlementPreview, Settlement } from "../types/game";
import { errorMessage } from "../utils/errors";
import { formatCurrency, formatChips } from "../utils/formatters";

export default function GameDetails() {
  const { gameId = "" } = useParams();
  const navigate = useNavigate();

  const [game, setGame] = useState<Game>();
  const [participants, setParticipants] = useState<Participant[]>([]);
  const [preview, setPreview] = useState<SettlementPreview | undefined>();
  const [settlement, setSettlement] = useState<Settlement | undefined>();

  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  const [successBanner, setSuccessBanner] = useState("");

  // Buy-in modal state
  const [activeBuyInModal, setActiveBuyInModal] = useState<{
    participant: Participant;
    buyIn?: BuyIn;
  } | null>(null);

  // Confirmation dialog state
  const [confirmation, setConfirmation] = useState<{
    title: string;
    message: string;
    confirmLabel?: string;
    confirmVariant?: "primary" | "danger" | "warning";
    action: () => Promise<unknown>;
  } | null>(null);

  const reload = useCallback(async () => {
    setLoading(true);
    setError("");
    try {
      const [fetchedGame, fetchedParticipants] = await Promise.all([
        api.getGame(gameId),
        api.getParticipants(gameId),
      ]);

      setGame(fetchedGame);
      setParticipants(fetchedParticipants);

      if (fetchedGame.gameStatus === "ENDED") {
        setSettlement(undefined);
        // Only fetch preview if all participants have final chips entered
        const allHaveFinals =
          fetchedParticipants.length >= 2 &&
          fetchedParticipants.every(
            (p) => p.finalChipValue !== null && p.finalChipValue !== undefined
          );
        if (allHaveFinals) {
          try {
            const prev = await api.getPreview(gameId);
            setPreview(prev);
          } catch {
            setPreview(undefined);
          }
        } else {
          setPreview(undefined);
        }
      } else if (fetchedGame.gameStatus === "SETTLED") {
        // Crucial fix: Clear preview completely when game is SETTLED
        setPreview(undefined);
        const settled = await api.getSettlement(gameId);
        setSettlement(settled);
      } else {
        setPreview(undefined);
        setSettlement(undefined);
      }
    } catch (err) {
      setError(errorMessage(err));
    } finally {
      setLoading(false);
    }
  }, [gameId]);

  useEffect(() => {
    void reload();
  }, [reload]);

  const executeAction = async (action: () => Promise<unknown>, successMsg?: string) => {
    setBusy(true);
    setError("");
    try {
      await action();
      if (successMsg) {
        setSuccessBanner(successMsg);
        setTimeout(() => setSuccessBanner(""), 4000);
      }
      await reload();
    } catch (err) {
      setError(errorMessage(err));
    } finally {
      setBusy(false);
    }
  };

  const isManager = Boolean(game?.manager);
  const canModifyBuyIns = Boolean(
    isManager &&
      (game?.gameStatus === "OPEN" ||
        (game?.gameStatus === "RUNNING" && !game.roundActive))
  );
  const canRemoveParticipant = Boolean(
    isManager &&
      (game?.gameStatus === "OPEN" ||
        (game?.gameStatus === "RUNNING" && !game.roundActive))
  );

  // Pot and chips calculations
  const totalBuyInChips = useMemo(
    () =>
      participants.reduce(
        (sum, p) => sum + p.buyIns.reduce((bSum, b) => bSum + b.chipValue, 0),
        0
      ),
    [participants]
  );

  const rate = Number(game?.chipConversionRate) || 1;
  const totalPotMoney = totalBuyInChips * rate;

  // Participant actions
  const handleAddParticipant = (data: { userId?: number; username?: string }) => {
    void executeAction(
      () => api.addParticipant(gameId, data),
      `Player added successfully to the session!`
    );
  };

  const handleRemoveParticipant = (p: Participant) => {
    setConfirmation({
      title: `Remove ${p.username}?`,
      message: `Are you sure you want to remove ${p.username} from this poker session? This is only allowed when no buy-ins have been recorded for them.`,
      confirmLabel: "Remove Player",
      confirmVariant: "danger",
      action: () => api.removeParticipant(gameId, p.participantId),
    });
  };

  // Buy-in actions
  const handleSaveBuyIn = (chipValue: number) => {
    if (!activeBuyInModal) return;
    const { participant, buyIn } = activeBuyInModal;

    if (buyIn) {
      // Edit buy-in
      void executeAction(
        () => api.updateBuyIn(gameId, buyIn.buyInId, chipValue),
        `Buy-in updated to ${chipValue} chips for ${participant.username}.`
      );
    } else {
      // Add buy-in
      void executeAction(
        () => api.addBuyIn(gameId, participant.participantId, chipValue),
        `Recorded buy-in of ${chipValue} chips for ${participant.username}.`
      );
    }
    setActiveBuyInModal(null);
  };

  const handleDeleteBuyIn = (p: Participant, buyIn: BuyIn) => {
    setConfirmation({
      title: "Delete Buy-In?",
      message: `Delete the buy-in of ${buyIn.chipValue} chips for ${p.username}? This will remove it from the session ledger.`,
      confirmLabel: "Delete Buy-In",
      confirmVariant: "danger",
      action: () => api.deleteBuyIn(gameId, buyIn.buyInId),
    });
  };

  // Lifecycle transitions
  const handleStartGame = () => {
    if (participants.length < 2) {
      setError("At least 2 players are required to start the poker session.");
      return;
    }
    const missingBuyIn = participants.find((p) => p.buyIns.length === 0);
    if (missingBuyIn) {
      setError(
        `Every player must have at least one buy-in. Please record a buy-in for ${missingBuyIn.username}.`
      );
      return;
    }

    setConfirmation({
      title: "Start Poker Session?",
      message:
        "Starting the session locks initial buy-ins and starts Round 1. Once a round concludes, you can end the round to record re-buys or seat new players.",
      confirmLabel: "Start Game",
      confirmVariant: "primary",
      action: () => api.startGame(gameId),
    });
  };

  const handleToggleRound = () => {
    if (!game) return;
    const nextState = !game.roundActive;
    void executeAction(
      () => api.updateRoundState(gameId, nextState),
      nextState
        ? "Next round started! Table buy-ins and seating are locked during active play."
        : "Round ended! You can now record re-buys, seat new players, or end the session."
    );
  };

  const handleEndGame = () => {
    setConfirmation({
      title: "End Entire Poker Session?",
      message:
        "Ending the session concludes all play for the night. You will enter each player's final chip count to compute net balances and generate the minimum settlement transfers.",
      confirmLabel: "End Session",
      confirmVariant: "danger",
      action: () => api.endGame(gameId),
    });
  };

  const handleDeleteGame = () => {
    setConfirmation({
      title: "Delete This Game?",
      message:
        "Are you sure you want to delete this game session? All players and buy-ins will be permanently deleted. This cannot be undone.",
      confirmLabel: "Delete Game",
      confirmVariant: "danger",
      action: async () => {
        await api.deleteGame(gameId);
        navigate("/dashboard");
      },
    });
  };

  // Final chips saving
  const handleSaveFinalChips = (values: { participantId: number; chipValue: number }[]) => {
    void executeAction(
      () => api.saveFinalChips(gameId, values),
      "Final chip counts saved! Settlement preview calculated."
    );
  };

  // Finalize settlement (Fixed bug: updates state cleanly without getting stuck)
  const handleFinalizeSettlement = () => {
    setConfirmation({
      title: "Finalize & Lock Settlement?",
      message:
        "This will lock all ledger transactions permanently. The settlement will become completely read-only and cannot be changed.",
      confirmLabel: "Finalize Settlement",
      confirmVariant: "danger",
      action: async () => {
        const settledData = await api.finalizeSettlement(gameId);
        // Clear preview immediately to avoid stuck UI bug
        setPreview(undefined);
        setSettlement(settledData);
        setGame((prev) => (prev ? { ...prev, gameStatus: "SETTLED" } : undefined));
      },
    });
  };

  if (loading && !game) {
    return (
      <div className="app-layout">
        <Navbar />
        <main className="page-container">
          <div className="loading-state">
            <div className="spinner" />
            <p>Loading poker session #{gameId}…</p>
          </div>
        </main>
      </div>
    );
  }

  if (!game) {
    return (
      <div className="app-layout">
        <Navbar />
        <main className="page-container">
          <div className="alert alert-error">
            <span>{error || "Poker game session not found."}</span>
          </div>
          <Link to="/dashboard" className="btn btn-primary">
            ← Back to Dashboard
          </Link>
        </main>
      </div>
    );
  }

  return (
    <div className="app-layout">
      <Navbar />

      <main className="page-container">
        {/* Navigation Breadcrumb */}
        <div className="breadcrumb-row">
          <Link to="/dashboard" className="breadcrumb-link">
            ← Back to Dashboard
          </Link>
        </div>

        {/* Game Title & Header Banner */}
        <div className="game-header-card">
          <div className="game-header-info">
            <div className="game-meta-eyebrow">
              <span className="game-id-tag">SESSION #{game.id}</span>
              <span className="rate-badge">🪙 1 Chip = {formatCurrency(game.chipConversionRate)}</span>
              {game.manager && (
                <span className="manager-badge">
                  <ShieldIcon size={12} /> You are Game Manager
                </span>
              )}
            </div>

            <h1 className="game-main-title">
              {game.name || `${game.createdByUsername}’s Poker Session`}
            </h1>

            <p className="game-sub-info">
              Host: <strong>@{game.createdByUsername}</strong> • Table Status:{" "}
              <strong>{game.gameStatus}</strong>
              {game.gameStatus === "RUNNING" && (
                <span> ({game.roundActive ? "Round in Progress" : "Between Rounds"})</span>
              )}
            </p>
          </div>

          <div className="game-header-status-side">
            <StatusBadge status={game.gameStatus} roundActive={game.roundActive} />

            {/* Quick stats in header */}
            <div className="header-chips-pot">
              <div className="header-pot-item">
                <span className="pot-label">Total Table Chips</span>
                <span className="pot-value font-mono">🪙 {formatChips(totalBuyInChips)}</span>
              </div>
              <div className="header-pot-item">
                <span className="pot-label">Total Pot Value</span>
                <span className="pot-value font-mono text-emerald">{formatCurrency(totalPotMoney)}</span>
              </div>
            </div>
          </div>
        </div>

        {/* Alerts & Notifications */}
        {error && (
          <div className="alert alert-error">
            <AlertCircleIcon size={18} />
            <span>{error}</span>
          </div>
        )}

        {successBanner && (
          <div className="alert alert-success">
            <CheckCircleIcon size={18} />
            <span>{successBanner}</span>
          </div>
        )}

        {/* Step Guide / Lifecycle Banner */}
        <div className="lifecycle-panel">
          <div className="lifecycle-instructions">
            <h3 className="lifecycle-title">
              {game.gameStatus === "OPEN" && "1. Registration & Initial Buy-Ins"}
              {game.gameStatus === "RUNNING" && (game.roundActive ? "2. Active Round Playing" : "2. Between Rounds (Re-buys Allowed)")}
              {game.gameStatus === "ENDED" && "3. Enter Final Chip Counts"}
              {game.gameStatus === "SETTLED" && "4. Settlement Finalized & Complete"}
            </h3>
            <p className="lifecycle-desc">
              {game.gameStatus === "OPEN" &&
                "Add players by username or ID and record each player's initial buy-in. When everyone is seated, click 'Start Game'."}
              {game.gameStatus === "RUNNING" &&
                (game.roundActive
                  ? "Gameplay is in progress. Buy-in additions and new player seating are locked during an active round. When the hand concludes, click 'End Current Round' above to allow re-buys."
                  : "Round concluded (between hands). You can now record re-buys for existing players, seat new players, start the next round, or end the poker session when finished.")}
              {game.gameStatus === "ENDED" &&
                "The game has ended. Count all chips on the table and enter each player's final chips below. When balanced, review the preview and finalize!"}
              {game.gameStatus === "SETTLED" &&
                "Settlement is officially finalized! Review the minimum peer-to-peer transfers below and copy them to your group chat."}
            </p>
          </div>

          {/* Manager Action Toolbar */}
          {isManager && (
            <div className="lifecycle-toolbar">
              {game.gameStatus === "OPEN" && (
                <>
                  <button
                    type="button"
                    className="btn btn-primary"
                    disabled={busy || participants.length < 2}
                    onClick={handleStartGame}
                  >
                    <PlayIcon size={16} />
                    <span>Start Game</span>
                  </button>

                  <button
                    type="button"
                    className="btn btn-ghost text-rose"
                    disabled={busy}
                    onClick={handleDeleteGame}
                  >
                    <TrashIcon size={16} />
                    <span>Delete Session</span>
                  </button>
                </>
              )}

              {game.gameStatus === "RUNNING" && (
                <>
                  <button
                    type="button"
                    className={`btn ${game.roundActive ? "btn-warning" : "btn-primary"}`}
                    disabled={busy}
                    onClick={handleToggleRound}
                  >
                    {game.roundActive ? <CheckCircleIcon size={16} /> : <PlayIcon size={16} />}
                    <span>{game.roundActive ? "End Current Round" : "Start Next Round"}</span>
                  </button>

                  {!game.roundActive && (
                    <button
                      type="button"
                      className="btn btn-danger"
                      disabled={busy}
                      onClick={handleEndGame}
                    >
                      <FlagIcon size={16} />
                      <span>End Poker Session</span>
                    </button>
                  )}
                </>
              )}
            </div>
          )}
        </div>

        {/* Section: Players & Buy-Ins */}
        <section className="dashboard-section participants-section">
          <div className="section-header">
            <div>
              <h2 className="section-title">
                Players & Table Buy-Ins ({participants.length})
              </h2>
              <p className="section-sub">
                {canModifyBuyIns
                  ? "Add players to the table and record buy-ins or re-buys."
                  : "All recorded buy-ins are displayed below (modifications locked during active round)."}
              </p>
            </div>
          </div>

          {/* Informative banner when round is active */}
          {game.gameStatus === "RUNNING" && game.roundActive && (
            <div className="alert alert-info round-lock-banner">
              <ClockIcon size={16} />
              <span>
                A round is currently in progress. Table buy-ins and seating are locked during active play. Click <strong>End Current Round</strong> above once the hand finishes to record re-buys or seat new players.
              </span>
            </div>
          )}

          {/* Add Participant Autocomplete Search (Manager only when allowed) */}
          {canModifyBuyIns && (
            <div className="add-player-box">
              <span className="add-player-title">Add Player to Session:</span>
              <UserAutocomplete
                onSelect={handleAddParticipant}
                busy={busy}
                disabled={!canModifyBuyIns}
              />
            </div>
          )}

          {/* Participants Cards Grid */}
          {participants.length > 0 ? (
            <div className="participants-grid">
              {participants.map((p) => (
                <ParticipantCard
                  key={p.participantId}
                  participant={p}
                  chipConversionRate={game.chipConversionRate}
                  isManager={isManager}
                  canModifyBuyIns={canModifyBuyIns}
                  canRemoveParticipant={canRemoveParticipant}
                  onOpenAddBuyIn={(player) => setActiveBuyInModal({ participant: player })}
                  onEditBuyIn={(player, b) => setActiveBuyInModal({ participant: player, buyIn: b })}
                  onDeleteBuyIn={handleDeleteBuyIn}
                  onRemoveParticipant={handleRemoveParticipant}
                />
              ))}
            </div>
          ) : (
            <div className="empty-players-box">
              <UsersIcon size={36} className="text-gray" />
              <h3>No Players at the Table Yet</h3>
              <p>
                Use the search box above to add friends by their username or numeric ID. At least 2 players with buy-ins are required to start.
              </p>
            </div>
          )}
        </section>

        {/* Section: Final Chip Counts (when ENDED or SETTLED) */}
        {game.gameStatus === "ENDED" && (
          <FinalChipEntry
            participants={participants}
            chipConversionRate={game.chipConversionRate}
            isManager={isManager}
            busy={busy}
            onSave={handleSaveFinalChips}
          />
        )}

        {/* Section: Settlement & Transfers (when preview available or settled) */}
        {(preview || settlement) && (
          <SettlementSection
            preview={preview}
            settlement={settlement}
            participants={participants}
            chipConversionRate={game.chipConversionRate}
            isSettled={game.gameStatus === "SETTLED"}
            isManager={isManager}
            busy={busy}
            onFinalize={handleFinalizeSettlement}
          />
        )}
      </main>

      {/* Buy-In Modal */}
      {activeBuyInModal && (
        <BuyInModal
          participantName={activeBuyInModal.participant.username}
          chipConversionRate={game.chipConversionRate}
          initialValue={activeBuyInModal.buyIn?.chipValue}
          isEdit={Boolean(activeBuyInModal.buyIn)}
          busy={busy}
          onSave={handleSaveBuyIn}
          onClose={() => setActiveBuyInModal(null)}
        />
      )}

      {/* Confirmation Dialog */}
      {confirmation && (
        <ConfirmDialog
          title={confirmation.title}
          message={confirmation.message}
          confirmLabel={confirmation.confirmLabel}
          confirmVariant={confirmation.confirmVariant}
          busy={busy}
          onCancel={() => setConfirmation(null)}
          onConfirm={() => {
            const action = confirmation.action;
            setConfirmation(null);
            void executeAction(action);
          }}
        />
      )}
    </div>
  );
}
