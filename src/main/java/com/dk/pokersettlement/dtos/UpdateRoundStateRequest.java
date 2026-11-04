package com.dk.pokersettlement.dtos;

import jakarta.validation.constraints.NotNull;

public record UpdateRoundStateRequest(
        @NotNull Boolean roundActive
) {}