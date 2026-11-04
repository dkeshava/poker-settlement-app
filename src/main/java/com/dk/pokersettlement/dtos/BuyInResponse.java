package com.dk.pokersettlement.dtos;

public record BuyInResponse(
        Long buyInId,
        Long participantId,
        Long chipValue
) {}
