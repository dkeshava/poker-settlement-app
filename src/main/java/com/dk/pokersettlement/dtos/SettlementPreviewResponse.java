package com.dk.pokersettlement.dtos;

import java.math.BigDecimal;
import java.util.List;

public record SettlementPreviewResponse(
        Long gameId,
        BigDecimal chipConversionRate,
        List<ParticipantNetBalanceResponse> netBalances,
        List<SettlementTransfer> transfers,
        int transactionCount
) {
}