import axios from "./axiosInstance";
import type { BuyIn, Game, Participant, Settlement, SettlementPreview } from "../types/game";

export const getGames = () => axios.get<Game[]>("/games").then(r => r.data);

export const getGame = (id: string | number) => axios.get<Game>(`/games/${id}`).then(r => r.data);

export const createGame = (data: { name?: string; chipConversionRate: number | string }) =>
  axios.post<Game>("/games", data).then(r => r.data);

export const deleteGame = (id: string | number) => axios.delete(`/games/${id}`);

export const getParticipants = (id: string | number) =>
  axios.get<Participant[]>(`/games/${id}/participants`).then(r => r.data);

export const addParticipant = (gameId: string | number, data: { userId?: number; username?: string }) =>
  axios.post<Participant>(`/games/${gameId}/participants`, data).then(r => r.data);

export const removeParticipant = (gameId: string | number, participantId: number) =>
  axios.post(`/games/${gameId}/participants/${participantId}`);

export const addBuyIn = (gameId: string | number, participantId: number, chipValue: number) =>
  axios.post<BuyIn>(`/games/${gameId}/participants/${participantId}/`, { chipValue }).then(r => r.data);

export const updateBuyIn = (gameId: string | number, buyInId: number, chipValue: number) =>
  axios.patch<BuyIn>(`/games/${gameId}/buy-ins/${buyInId}`, { chipValue }).then(r => r.data);

export const deleteBuyIn = (gameId: string | number, buyInId: number) =>
  axios.delete(`/games/${gameId}/buy-ins/${buyInId}`);

export const startGame = (id: string | number) =>
  axios.post<Game>(`/games/${id}/start`).then(r => r.data);

export const updateRoundState = (id: string | number, roundActive: boolean) =>
  axios.patch<Game>(`/games/${id}/round-state`, { roundActive }).then(r => r.data);

export const endGame = (id: string | number) =>
  axios.post<Game>(`/games/${id}/end`).then(r => r.data);

export const saveFinalChips = (id: string | number, values: { participantId: number; chipValue: number }[]) =>
  axios.put<Game>(`/games/${id}/final-chip-values`, { finalChipValues: values }).then(r => r.data);

export const getPreview = (id: string | number) =>
  axios.get<SettlementPreview>(`/games/${id}/settlement-preview`).then(r => r.data);

export const finalizeSettlement = (id: string | number) =>
  axios.post<Settlement>(`/games/${id}/settlement/finalize`).then(r => r.data);

export const getSettlement = (id: string | number) =>
  axios.get<Settlement>(`/games/${id}/settlement`).then(r => r.data);

export const searchUsers = (query: string) =>
  axios.get<{ id: number; username: string }[]>("/users/search", { params: { q: query } }).then(r => r.data);
