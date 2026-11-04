package com.dk.pokersettlement.dtos;

import java.math.BigDecimal;

public record SettlementTransfer(
        Long fromParticipantId,
        String fromUsername,
        Long toParticipantId,
        String toUsername,
        BigDecimal amount
) {
}