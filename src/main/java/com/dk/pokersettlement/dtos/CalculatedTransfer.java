package com.dk.pokersettlement.dtos;

import java.math.BigDecimal;

public record CalculatedTransfer(
        Long fromParticipantId,
        Long toParticipantId,
        BigDecimal amount
) {}