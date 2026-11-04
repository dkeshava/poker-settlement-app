package com.dk.pokersettlement.controllers;

import com.dk.pokersettlement.dtos.AddParticipantRequest;
import com.dk.pokersettlement.dtos.ParticipantResponse;
import com.dk.pokersettlement.services.CurrentUserService;
import com.dk.pokersettlement.services.GameService;
import com.dk.pokersettlement.services.ParticipantService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import org.springframework.security.access.AccessDeniedException;
import java.util.List;

@RestController
@CrossOrigin
@RequestMapping("/games/{gameId}/participants")
public class ParticipantController {
    @Autowired
    GameService gameService;
    @Autowired
    ParticipantService participantService;
    @Autowired
    CurrentUserService currentUserService;

    @GetMapping
    public ResponseEntity<List<ParticipantResponse>> getParticipants(
            @PathVariable("gameId") Long gameId) throws AccessDeniedException {

        List<ParticipantResponse> participants = participantService.getParticipants(gameId);

        return ResponseEntity.ok(participants);
    }

    @PostMapping
    public ResponseEntity<ParticipantResponse> addParticipant(@PathVariable("gameId") Long gameId,@RequestBody AddParticipantRequest addParticipantRequest) throws AccessDeniedException {
        ParticipantResponse response =
                participantService.addParticipant(gameId, addParticipantRequest);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(response);
    }

    @PostMapping("/{participantId}")
    public ResponseEntity<ParticipantResponse> deleteParticipant(@PathVariable("gameId") Long gameId,@PathVariable Long participantId) throws AccessDeniedException {
        ParticipantResponse response =
                participantService.deleteParticipant(gameId, participantId);

        return ResponseEntity.status(HttpStatus.OK)
                .body(response);
    }
}
