package com.dk.pokersettlement.dtos;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record FinalChipValueRequest(
        @NotNull Long participantId,
        @NotNull @PositiveOrZero Long chipValue
) {}