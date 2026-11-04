package com.dk.pokersettlement.dtos;

import com.dk.pokersettlement.enums.GameState;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class ParticipantResponse {
    private Long participantId;
    private Long gameId;
    private Long userId;
    private String username;
    private Long finalChipValue;
    private List<BuyInResponse> buyIns;
}
