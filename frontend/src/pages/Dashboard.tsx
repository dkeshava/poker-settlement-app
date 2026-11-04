import { useEffect, useState, type FormEvent } from "react";
import { Link, useNavigate } from "react-router-dom";
import { createGame, getGames, deleteGame } from "../api/gameApi";
import Navbar from "../components/Navbar";
import StatusBadge from "../components/StatusBadge";
import ConfirmDialog from "../components/ConfirmDialog";
import { SpadeIcon, PlusIcon, TrashIcon, ChipIcon, ShieldIcon, ArrowRightIcon } from "../components/Icons";
import type { Game } from "../types/game";
import { errorMessage } from "../utils/errors";
import { formatCurrency } from "../utils/formatters";

export default function Dashboard() {
  const [games, setGames] = useState<Game[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [activeTab, setActiveTab] = useState<"all" | "managed" | "playing">("all");

  // Create game modal state
  const [openModal, setOpenModal] = useState(false);
  const [gameName, setGameName] = useState("");
  const [conversionRate, setConversionRate] = useState("1");
  const [creating, setCreating] = useState(false);

  // Delete game confirmation state
  const [deletingGame, setDeletingGame] = useState<Game | null>(null);
  const [deleteBusy, setDeleteBusy] = useState(false);

  const navigate = useNavigate();

  const loadGames = async () => {
    setLoading(true);
    setError("");
    try {
      const data = await getGames();
      setGames(data);
    } catch (err) {
      setError(errorMessage(err));
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    void loadGames();
  }, []);

  const handleCreateGame = async (e: FormEvent) => {
    e.preventDefault();
    const rate = parseFloat(conversionRate);
    if (isNaN(rate) || rate <= 0) {
      setError("Chip conversion rate must be a positive number.");
      return;
    }

    setCreating(true);
    setError("");
    try {
      const newGame = await createGame({
        name: gameName.trim() || undefined,
        chipConversionRate: rate,
      });
      setOpenModal(false);
      setGameName("");
      setConversionRate("1");
      navigate(`/games/${newGame.id}`);
    } catch (err) {
      setError(errorMessage(err));
    } finally {
      setCreating(false);
    }
  };

  const handleDeleteGame = async () => {
    if (!deletingGame) return;
    setDeleteBusy(true);
    try {
      await deleteGame(deletingGame.id);
      setDeletingGame(null);
      await loadGames();
    } catch (err) {
      setError(errorMessage(err));
      setDeletingGame(null);
    } finally {
      setDeleteBusy(false);
    }
  };

  const managedGames = games.filter((g) => g.manager);
  const playingGames = games.filter((g) => !g.manager);

  const displayedGames =
    activeTab === "managed"
      ? managedGames
      : activeTab === "playing"
      ? playingGames
      : games;

  return (
    <div className="app-layout">
      <Navbar />

      <main className="page-container">
        {/* Hero Section */}
        <div className="dashboard-hero">
          <div className="hero-content">
            <div className="hero-eyebrow">
              <SpadeIcon size={14} className="text-emerald" />
              <span>POKER SETTLEMENT DASHBOARD</span>
            </div>
            <h1 className="hero-title">Live Poker Sessions</h1>
            <p className="hero-description">
              Manage buy-ins, track gameplay rounds, enter final chip counts, and generate mathematically optimal settlements.
            </p>
          </div>

          <div className="hero-actions">
            <button
              type="button"
              className="btn btn-primary btn-lg"
              onClick={() => {
                setGameName("");
                setConversionRate("1");
                setOpenModal(true);
              }}
            >
              <PlusIcon size={18} />
              <span>Create New Game</span>
            </button>
          </div>
        </div>

        {error && (
          <div className="alert alert-error">
            <span>{error}</span>
            <button
              type="button"
              className="btn-alert-action"
              onClick={() => void loadGames()}
            >
              Retry
            </button>
          </div>
        )}

        {/* Stats Row */}
        <div className="stats-row">
          <div className="stat-card">
            <span className="stat-card-label">Total Accessible Games</span>
            <span className="stat-card-value font-mono">{games.length}</span>
          </div>
          <div className="stat-card">
            <span className="stat-card-label">Managed by You</span>
            <span className="stat-card-value text-emerald font-mono">{managedGames.length}</span>
          </div>
          <div className="stat-card">
            <span className="stat-card-label">Active / In Progress</span>
            <span className="stat-card-value text-amber font-mono">
              {games.filter((g) => g.gameStatus === "OPEN" || g.gameStatus === "RUNNING").length}
            </span>
          </div>
        </div>

        {/* Filter Tabs */}
        <div className="tabs-header">
          <div className="tab-buttons">
            <button
              type="button"
              className={`tab-btn ${activeTab === "all" ? "active" : ""}`}
              onClick={() => setActiveTab("all")}
            >
              All Sessions ({games.length})
            </button>
            <button
              type="button"
              className={`tab-btn ${activeTab === "managed" ? "active" : ""}`}
              onClick={() => setActiveTab("managed")}
            >
              Managed by You ({managedGames.length})
            </button>
            <button
              type="button"
              className={`tab-btn ${activeTab === "playing" ? "active" : ""}`}
              onClick={() => setActiveTab("playing")}
            >
              Joined as Player ({playingGames.length})
            </button>
          </div>
        </div>

        {/* Games Grid */}
        {loading ? (
          <div className="loading-state">
            <div className="spinner" />
            <p>Loading poker sessions…</p>
          </div>
        ) : displayedGames.length > 0 ? (
          <div className="games-grid">
            {displayedGames.map((game) => (
              <div key={game.id} className="game-card-tile">
                <div className="game-tile-header">
                  <div>
                    <div className="game-id-badge">SESSION #{game.id}</div>
                    <h3 className="game-tile-title">{game.name || `Poker Session #${game.id}`}</h3>
                  </div>
                  <StatusBadge status={game.gameStatus} roundActive={game.roundActive} />
                </div>

                <div className="game-tile-meta">
                  <div className="meta-item">
                    <span className="meta-label">Conversion Rate</span>
                    <span className="meta-value font-mono">
                      {formatCurrency(game.chipConversionRate)} / chip
                    </span>
                  </div>
                  <div className="meta-item">
                    <span className="meta-label">Manager</span>
                    <span className="meta-value">
                      {game.manager ? (
                        <span className="badge-manager-pill">
                          <ShieldIcon size={12} /> You (Manager)
                        </span>
                      ) : (
                        `@${game.createdByUsername}`
                      )}
                    </span>
                  </div>
                </div>

                <div className="game-tile-actions">
                  <Link to={`/games/${game.id}`} className="btn btn-tile-open">
                    <span>Enter Game Table</span>
                    <ArrowRightIcon size={16} />
                  </Link>

                  {/* Manager Delete Button (only if manager and not settled) */}
                  {game.manager && game.gameStatus !== "SETTLED" && (
                    <button
                      type="button"
                      className="btn-icon-danger"
                      title="Delete this game session"
                      onClick={() => setDeletingGame(game)}
                    >
                      <TrashIcon size={16} />
                    </button>
                  )}
                </div>
              </div>
            ))}
          </div>
        ) : (
          <div className="empty-state-box">
            <div className="empty-icon-wrap">
              <SpadeIcon size={36} className="text-gray" />
            </div>
            <h3>No Poker Games Found</h3>
            <p>
              {activeTab === "managed"
                ? "You haven't created any games yet. Click '+ Create New Game' above to start one!"
                : activeTab === "playing"
                ? "You haven't joined any games yet. Ask your game manager to add you by your username!"
                : "No games are currently available. Create a new game to get started!"}
            </p>
            <button
              type="button"
              className="btn btn-primary"
              onClick={() => {
                setGameName("");
                setConversionRate("1");
                setOpenModal(true);
              }}
            >
              <PlusIcon size={16} />
              <span>Create Your First Game</span>
            </button>
          </div>
        )}
      </main>

      {/* Create Game Modal */}
      {openModal && (
        <div className="modal-backdrop" onClick={() => setOpenModal(false)}>
          <div
            className="modal create-game-modal"
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
                  <h2 className="modal-title">Create Poker Session</h2>
                  <p className="modal-sub">Set up a new table and invite players</p>
                </div>
              </div>
              <button
                type="button"
                className="btn-icon-close"
                onClick={() => setOpenModal(false)}
              >
                ✕
              </button>
            </div>

            <form onSubmit={handleCreateGame} className="modal-body">
              <div className="form-group">
                <label className="input-label" htmlFor="gameNameInput">
                  Game Session Name (Optional)
                </label>
                <input
                  id="gameNameInput"
                  type="text"
                  autoFocus
                  placeholder="e.g. Friday Night Cash Game, High Stakes #1"
                  className="input"
                  value={gameName}
                  onChange={(e) => setGameName(e.target.value)}
                  disabled={creating}
                />
                <span className="input-hint">
                  Give your poker session a memorable name.
                </span>
              </div>

              <div className="form-group">
                <label className="input-label" htmlFor="rateInput">
                  Chip Conversion Rate (₹ Cash per Chip)
                </label>
                <div className="rate-input-wrap">
                  <span className="rate-prefix">1 Chip = ₹</span>
                  <input
                    id="rateInput"
                    type="number"
                    min="0.01"
                    step="any"
                    className="input rate-input font-mono"
                    placeholder="1.00"
                    value={conversionRate}
                    onChange={(e) => setConversionRate(e.target.value)}
                    required
                    disabled={creating}
                  />
                </div>
                <span className="input-hint">
                  e.g. 1 means 1 chip = ₹1; 0.50 means 1 chip = ₹0.50 (50 paise); 5 means 1 chip = ₹5.
                </span>
              </div>

              {/* Rate Presets */}
              <div className="preset-chips-section">
                <span className="preset-title">Common Rates:</span>
                <div className="preset-buttons-grid">
                  {["0.50", "1", "2", "5", "10"].map((r) => (
                    <button
                      key={r}
                      type="button"
                      className={`btn-preset ${conversionRate === r ? "active" : ""}`}
                      onClick={() => setConversionRate(r)}
                      disabled={creating}
                    >
                      ₹{r}
                    </button>
                  ))}
                </div>
              </div>

              <div className="modal-actions">
                <button
                  type="button"
                  className="btn btn-ghost"
                  onClick={() => setOpenModal(false)}
                  disabled={creating}
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  className="btn btn-primary"
                  disabled={creating || !conversionRate || parseFloat(conversionRate) <= 0}
                >
                  <PlusIcon size={16} />
                  <span>{creating ? "Creating Table…" : "Create & Launch Table"}</span>
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Delete Game Confirmation Dialog */}
      {deletingGame && (
        <ConfirmDialog
          title="Delete Poker Session?"
          message={`Are you sure you want to delete "${deletingGame.name || `Session #${deletingGame.id}`}"? All registered participants and recorded buy-ins for this session will be permanently removed.`}
          confirmLabel="Delete Session"
          confirmVariant="danger"
          busy={deleteBusy}
          onConfirm={() => void handleDeleteGame()}
          onCancel={() => setDeletingGame(null)}
        />
      )}
    </div>
  );
}
