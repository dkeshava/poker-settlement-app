package com.dk.pokersettlement.services;


import com.dk.pokersettlement.dtos.BuyInResponse;
import com.dk.pokersettlement.dtos.CreateBuyInRequest;
import com.dk.pokersettlement.dtos.UpdateBuyInRequest;
import com.dk.pokersettlement.enums.GameState;
import com.dk.pokersettlement.exceptions.ResourceNotFoundException;
import com.dk.pokersettlement.models.BuyIn;
import com.dk.pokersettlement.models.Game;
import com.dk.pokersettlement.models.GameParticipant;
import com.dk.pokersettlement.repositories.BuyInRepo;
import com.dk.pokersettlement.repositories.GameParticipantRepo;
import com.dk.pokersettlement.repositories.GameRepo;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import org.springframework.security.access.AccessDeniedException;
@Service
@RequiredArgsConstructor
public class BuyInService {

    private final BuyInRepo buyInRepo;
    private final GameRepo gameRepo;
    private final GameParticipantRepo gameParticipantRepo;
    private final GameService gameService;
    private final CurrentUserService currentUserService;

    private Game getEditableGame(Long gameId) throws AccessDeniedException {

        // Game existence
        Game game = gameRepo.findById(gameId)
                .orElseThrow(() ->
                        new EntityNotFoundException("Game not found"));

        Long currentUserId =
                currentUserService.getCurrentUser().getId();

        // Manager authorization
        if (!gameService.isManager(currentUserId, gameId)) {
            throw new AccessDeniedException(
                    "Only the game manager can modify buy-ins");
        }

        // Lifecycle validation
        boolean editable =
                game.getGameStatus() == GameState.OPEN
                        || (game.getGameStatus() == GameState.RUNNING
                        && !game.isRoundActive());

        if (!editable) {
            throw new IllegalStateException(
                    "Buy-ins cannot be modified in the current game state");
        }

        return game;
    }

    // ADD BUY-IN
    @Transactional
    public BuyInResponse addBuyIn(
            Long gameId,
            Long participantId,
            CreateBuyInRequest request) throws AccessDeniedException {

        Game game = getEditableGame(gameId);

        GameParticipant participant =
                gameParticipantRepo.findByIdAndGame_Id(
                                participantId, gameId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Participant not found in this game"));

        BuyIn buyIn = new BuyIn();
        buyIn.setParticipant(participant);
        buyIn.setChipValue(request.chipValue());

        BuyIn saved = buyInRepo.save(buyIn);

        return toResponse(saved);
    }

    // UPDATE BUY-IN
    @Transactional
    public BuyInResponse updateBuyIn(
            Long gameId,
            Long buyInId,
            UpdateBuyInRequest request) throws AccessDeniedException {

        getEditableGame(gameId);

        BuyIn buyIn = buyInRepo
                .findByIdAndParticipant_Game_Id(
                        buyInId, gameId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Buy-in not found in this game"));

        buyIn.setChipValue(request.chipValue());

        BuyIn updated = buyInRepo.save(buyIn);

        return toResponse(updated);
    }

    // DELETE BUY-IN
    @Transactional
    public void deleteBuyIn(
            Long gameId,
            Long buyInId) throws AccessDeniedException {

        getEditableGame(gameId);

        BuyIn buyIn = buyInRepo
                .findByIdAndParticipant_Game_Id(
                        buyInId, gameId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Buy-in not found in this game"));

        buyInRepo.delete(buyIn);
    }

    private BuyInResponse toResponse(BuyIn buyIn) {

        return new BuyInResponse(
                buyIn.getId(),
                buyIn.getParticipant().getId(),
                buyIn.getChipValue()
        );
    }
}
