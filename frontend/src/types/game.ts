export type GameStatus = "OPEN" | "RUNNING" | "ENDED" | "SETTLED";

export interface Game {
  id: number;
  name?: string;
  createdById: number;
  createdByUsername: string;
  gameStatus: GameStatus;
  chipConversionRate: number | string;
  roundActive: boolean;
  manager: boolean;
  participant: boolean;
}

export interface UserSummary {
  id: number;
  username: string;
}

export interface Participant {
  participantId: number;
  gameId: number;
  userId: number;
  username: string;
  finalChipValue: number | null;
  buyIns: BuyIn[];
}

export interface BuyIn {
  buyInId: number;
  participantId: number;
  chipValue: number;
}

export interface SettlementTransfer {
  fromParticipantId: number;
  fromUsername: string;
  toParticipantId: number;
  toUsername: string;
  amount: number | string;
}

export interface NetBalance {
  participantId: number;
  username: string;
  totalBuyInChips: number;
  finalChipValue: number;
  netChipValue: number;
  netAmount: number | string;
}

export interface SettlementPreview {
  gameId: number;
  chipConversionRate: number | string;
  netBalances: NetBalance[];
  transfers: SettlementTransfer[];
  transactionCount: number;
}

export interface Settlement {
  gameId: number;
  transfers: SettlementTransfer[];
  transactionCount: number;
}
