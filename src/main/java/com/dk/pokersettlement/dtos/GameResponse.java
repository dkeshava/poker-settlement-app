package com.dk.pokersettlement.dtos;

import com.dk.pokersettlement.enums.GameState;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class GameResponse {
    private Long id;
    private String name;
    private Long createdById;
    private String createdByUsername;
    private GameState gameStatus;
    private BigDecimal chipConversionRate;
    private boolean roundActive;
    private boolean manager;
    private boolean participant;
}
