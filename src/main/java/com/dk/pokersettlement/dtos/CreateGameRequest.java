package com.dk.pokersettlement.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class CreateGameRequest {
    private String name;
    private BigDecimal chipConversionRate;

    public CreateGameRequest(BigDecimal chipConversionRate) {
        this.chipConversionRate = chipConversionRate;
    }
}
