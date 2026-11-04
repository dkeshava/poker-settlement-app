package com.dk.pokersettlement.dtos;

import java.math.BigDecimal;

public record ParticipantNetBalanceResponse(
        Long participantId,
        String username,
        Long totalBuyInChips,
        Long finalChipValue,
        Long netChipValue,
        BigDecimal netAmount
) {
}