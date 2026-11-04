import { useEffect, useState, type FormEvent } from "react";
import { Link, useNavigate } from "react-router-dom";
import { createGame, getGames } from "../api/gameApi";
import Navbar from "../components/Navbar";
import StatusBadge from "../components/StatusBadge";
import type { Game } from "../types/game";
import { errorMessage } from "../utils/errors";

export default function Dashboard() {
  const [games, setGames] = useState<Game[]>([]); const [loading, setLoading] = useState(true); const [error, setError] = useState("");
  const [open, setOpen] = useState(false); const [rate, setRate] = useState(""); const [saving, setSaving] = useState(false); const navigate = useNavigate();
  const load = async () => { setLoading(true); try { setGames(await getGames()); } catch (e) { setError(errorMessage(e)); } finally { setLoading(false); } };
  useEffect(() => { void load(); }, []);
  const submit = async (e: FormEvent) => { e.preventDefault(); if (Number(rate) <= 0) return setError("Conversion rate must be greater than zero."); setSaving(true); try { const game = await createGame(rate); navigate(`/games/${game.id}`); } catch (err) { setError(errorMessage(err)); } finally { setSaving(false); } };
  const renderList = (title: string, list: Game[], description: string) => <section className="section"><h2>{title}</h2><p>{description}</p>{list.length ? <div className="game-grid">{list.map(g => <Link className="game-card" key={g.id} to={`/games/${g.id}`}><div><strong>Game #{g.id}</strong><StatusBadge status={g.gameStatus} /></div><p>Manager: {g.createdByUsername}{g.manager ? " (you)" : ""}</p><small>₹{g.chipConversionRate} per chip · {g.roundActive ? "Round active" : "Between rounds"}</small></Link>)}</div> : <div className="empty-state">No games here yet.</div>}</section>;
  return <><Navbar /><main className="page"><div className="hero-row"><div><p className="eyebrow">POKER SETTLEMENT</p><h1>Your game ledger</h1><p>Track buy-ins, record final chips, and settle up clearly.</p></div><button className="button" onClick={() => setOpen(true)}>+ Create game</button></div>{error && <div className="alert error">{error} <button onClick={() => void load()}>Retry</button></div>}{loading ? <p className="loading">Loading games…</p> : <>{renderList("Managed by you", games.filter(g => g.manager), "Games you can configure and settle.")}{renderList("You’re playing", games.filter(g => !g.manager), "Games you can view as a participant.")}</>}</main>{open && <div className="modal-backdrop"><form className="modal" onSubmit={submit}><h2>Create a game</h2><p>Set the monetary value of one chip.</p><label>Chip conversion rate<input type="number" min="0.01" step="any" autoFocus value={rate} onChange={e => setRate(e.target.value)} required /></label><div className="actions"><button type="button" className="button ghost" onClick={() => setOpen(false)}>Cancel</button><button className="button" disabled={saving}>{saving ? "Creating…" : "Create game"}</button></div></form></div>}</>;
}
