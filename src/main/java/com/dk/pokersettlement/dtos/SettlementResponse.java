package com.dk.pokersettlement.dtos;

import java.util.List;

public record SettlementResponse(
        Long gameId,
        List<SettlementTransfer> transfers,
        int transactionCount
) {
}
