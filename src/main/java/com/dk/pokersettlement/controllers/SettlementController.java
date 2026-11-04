package com.dk.pokersettlement.controllers;

import com.dk.pokersettlement.dtos.SettlementPreviewResponse;
import com.dk.pokersettlement.dtos.SettlementResponse;
import com.dk.pokersettlement.services.SettlementService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.AccessDeniedException;
//import java.nio.file.AccessDeniedException;

@RestController
@RequestMapping("/games/{gameId}")
@RequiredArgsConstructor
public class SettlementController {

    private final SettlementService settlementService;

    @GetMapping("/settlement-preview")
    public ResponseEntity<SettlementPreviewResponse> getSettlementPreview(
            @PathVariable Long gameId) throws AccessDeniedException {

        return ResponseEntity.ok(
                settlementService.getSettlementPreview(gameId)
        );
    }

    @PostMapping("/settlement/finalize")
    public ResponseEntity<SettlementResponse> finalizeSettlement(
            @PathVariable Long gameId) throws AccessDeniedException {

        return ResponseEntity.ok(
                settlementService.finalizeSettlement(gameId)
        );
    }

    @GetMapping("/settlement")
    public ResponseEntity<SettlementResponse> getSettlement(
            @PathVariable Long gameId) throws AccessDeniedException {

        return ResponseEntity.ok(
                settlementService.getSettlement(gameId)
        );
    }
}