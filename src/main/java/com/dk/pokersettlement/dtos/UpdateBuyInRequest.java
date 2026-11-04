package com.dk.pokersettlement.dtos;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record UpdateBuyInRequest(
        @NotNull
        @Positive
        Long chipValue
) {
}
