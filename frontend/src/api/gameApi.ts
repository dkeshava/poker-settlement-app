import axios from "./axiosInstance";
import type { BuyIn, Game, Participant, Settlement, SettlementPreview } from "../types/game";

export const getGames = () => axios.get<Game[]>("/games").then(r => r.data);
export const getGame = (id: string) => axios.get<Game>(`/games/${id}`).then(r => r.data);
export const createGame = (chipConversionRate: string) => axios.post<Game>("/games", { chipConversionRate }).then(r => r.data);
export const getParticipants = (id: string) => axios.get<Participant[]>(`/games/${id}/participants`).then(r => r.data);
export const addParticipant = (gameId: string, userId: number) => axios.post<Participant>(`/games/${gameId}/participants`, { userId }).then(r => r.data);
export const removeParticipant = (gameId: string, participantId: number) => axios.post(` /games/${gameId}/participants/${participantId}`.trim());
export const addBuyIn = (gameId: string, participantId: number, chipValue: number) => axios.post<BuyIn>(`/games/${gameId}/participants/${participantId}/`, { chipValue }).then(r => r.data);
export const updateBuyIn = (gameId: string, buyInId: number, chipValue: number) => axios.patch<BuyIn>(`/games/${gameId}/buy-ins/${buyInId}`, { chipValue }).then(r => r.data);
export const deleteBuyIn = (gameId: string, buyInId: number) => axios.delete(`/games/${gameId}/buy-ins/${buyInId}`);
export const startGame = (id: string) => axios.post<Game>(`/games/${id}/start`).then(r => r.data);
export const updateRoundState = (id: string, roundActive: boolean) => axios.patch<Game>(`/games/${id}/round-state`, { roundActive }).then(r => r.data);
export const endGame = (id: string) => axios.post<Game>(`/games/${id}/end`).then(r => r.data);
export const saveFinalChips = (id: string, values: { participantId: number; chipValue: number }[]) => axios.put<Game>(`/games/${id}/final-chip-values`, { finalChipValues: values }).then(r => r.data);
export const getPreview = (id: string) => axios.get<SettlementPreview>(`/games/${id}/settlement-preview`).then(r => r.data);
export const finalizeSettlement = (id: string) => axios.post<Settlement>(`/games/${id}/settlement/finalize`).then(r => r.data);
export const getSettlement = (id: string) => axios.get<Settlement>(`/games/${id}/settlement`).then(r => r.data);
