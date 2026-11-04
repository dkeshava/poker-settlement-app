package com.dk.pokersettlement.dtos;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record UpdateFinalChipValuesRequest(
        @NotEmpty
        List<@Valid FinalChipValueRequest> finalChipValues
) {}
