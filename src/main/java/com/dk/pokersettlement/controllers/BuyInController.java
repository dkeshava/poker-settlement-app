package com.dk.pokersettlement.controllers;

import com.dk.pokersettlement.dtos.BuyInResponse;
import com.dk.pokersettlement.dtos.CreateBuyInRequest;
import com.dk.pokersettlement.dtos.UpdateBuyInRequest;
import com.dk.pokersettlement.services.BuyInService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import org.springframework.security.access.AccessDeniedException;

@RestController
@CrossOrigin
@RequestMapping("/games/{gameId}")
public class BuyInController {

    private final BuyInService buyInService;

    public BuyInController(BuyInService buyInService) {
        this.buyInService = buyInService;
    }

    // ADD BUY-IN
    @PostMapping("/participants/{participantId}/")
    public ResponseEntity<BuyInResponse> addBuyIn(
            @PathVariable Long gameId,
            @PathVariable Long participantId,
            @Valid @RequestBody CreateBuyInRequest request) throws AccessDeniedException {

        BuyInResponse response =
                buyInService.addBuyIn(
                        gameId, participantId, request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(response);
    }

    // UPDATE BUY-IN
    @PatchMapping("/buy-ins/{buyInId}")
    public ResponseEntity<BuyInResponse> updateBuyIn(
            @PathVariable Long gameId,
            @PathVariable Long buyInId,
            @Valid @RequestBody UpdateBuyInRequest request) throws AccessDeniedException {

        BuyInResponse response =
                buyInService.updateBuyIn(
                        gameId, buyInId, request);

        return ResponseEntity.ok(response);
    }

    // DELETE BUY-IN
    @DeleteMapping("/buy-ins/{buyInId}")
    public ResponseEntity<Void> deleteBuyIn(
            @PathVariable Long gameId,
            @PathVariable Long buyInId) throws AccessDeniedException {

        buyInService.deleteBuyIn(gameId, buyInId);

        return ResponseEntity.noContent().build();
    }
}
