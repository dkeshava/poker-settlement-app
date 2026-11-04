package com.dk.pokersettlement.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class AddParticipantRequest {
    private Long userId;
    private String username;

    public AddParticipantRequest(Long userId) {
        this.userId = userId;
    }
}
