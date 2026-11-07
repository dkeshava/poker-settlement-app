package com.dk.pokersettlement.services;

import com.dk.pokersettlement.dtos.CreateGameRequest;
import com.dk.pokersettlement.dtos.FinalChipValueRequest;
import com.dk.pokersettlement.dtos.UpdateFinalChipValuesRequest;
import com.dk.pokersettlement.dtos.UpdateRoundStateRequest;
import com.dk.pokersettlement.enums.GameState;
import com.dk.pokersettlement.exceptions.ResourceNotFoundException;
import com.dk.pokersettlement.models.Game;
import com.dk.pokersettlement.models.GameParticipant;
import com.dk.pokersettlement.repositories.BuyInRepo;
import com.dk.pokersettlement.repositories.GameParticipantRepo;
import com.dk.pokersettlement.repositories.GameRepo;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import org.springframework.security.access.AccessDeniedException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Comparator;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
public class GameService {
    private GameRepo gameRepo;
    private CurrentUserService currentUserService;
    private GameParticipantRepo gameParticipantRepo;
    private BuyInRepo buyInRepo;
    public GameService(GameRepo gameRepo, CurrentUserService currentUserService, GameParticipantRepo gameParticipantRepo, BuyInRepo buyInRepo) {
        this.gameRepo = gameRepo;
        this.currentUserService = currentUserService;
        this.gameParticipantRepo = gameParticipantRepo;
        this.buyInRepo = buyInRepo;
    }

    public Game createGame(CreateGameRequest gameRequest) throws AccessDeniedException {
        Game game = new Game();
        game.setGameStatus(GameState.OPEN);
        game.setRoundActive(false);
        game.setCreatedBy(currentUserService.getCurrentUser());
        game.setChipConversionRate(gameRequest.getChipConversionRate());
        if (gameRequest.getName() != null && !gameRequest.getName().trim().isEmpty()) {
            game.setName(gameRequest.getName().trim());
        } else {
            game.setName("Poker Session");
        }

        return gameRepo.save(game);
    }

    @Transactional
    public void deleteGame(Long gameId) throws AccessDeniedException {
        Game game = getGameForManager(gameId);

        if (game.getGameStatus() == GameState.SETTLED) {
            throw new IllegalStateException("Settled games cannot be deleted");
        }

        List<GameParticipant> participants = gameParticipantRepo.findByGame_Id(gameId);
        if (!participants.isEmpty()) {
            List<Long> participantIds = participants.stream()
                    .map(GameParticipant::getId)
                    .toList();
            List<com.dk.pokersettlement.models.BuyIn> buyIns = buyInRepo.findByParticipantIdIn(participantIds);
            if (!buyIns.isEmpty()) {
                buyInRepo.deleteAll(buyIns);
            }
            gameParticipantRepo.deleteAll(participants);
        }

        gameRepo.delete(game);
    }

    public Game getGame(Long gameId) throws AccessDeniedException {
        Game game = gameRepo.findById(gameId)
                .orElseThrow(() -> new ResourceNotFoundException("Game not found"));

        Long currentUserId = currentUserService.getCurrentUser().getId();

        if (!isManagerOrParticipant(currentUserId,gameId)) {
            log.warn("Access denied: user {} cannot access game {}", currentUserId, gameId);
            throw new AccessDeniedException("You cannot access this game");
        }

        return game;
    }

    public List<Game> getAccessibleGames() {
        Long userId = currentUserService.getCurrentUser().getId();
        return gameRepo.findAccessibleByUserId(userId).stream()
                .sorted(Comparator.comparing(Game::getId).reversed())
                .toList();
    }

    public boolean isManagerOrParticipant(Long userId, Long gameId) {
        Game game = gameRepo.findById(gameId)
                .orElseThrow(() -> new EntityNotFoundException("Game not found"));

        boolean isManager =
                game.getCreatedBy().getId().equals(userId);

        return isManager
                || gameParticipantRepo.existsByGame_IdAndUser_Id(
                gameId, userId
        );
    }
    public boolean isManager(Long userId, Long gameId) {
        Game game = gameRepo.findById(gameId)
                .orElseThrow(() -> new EntityNotFoundException("Game not found"));

        return game.getCreatedBy().getId().equals(userId);
    }


    @Transactional
    public Game startGame(Long gameId) throws AccessDeniedException {

        Game game = getGameForManager(gameId);

        if (game.getGameStatus() != GameState.OPEN) {
            throw new IllegalStateException(
                    "Only an OPEN game can be started");
        }

        List<GameParticipant> participants =
                gameParticipantRepo.findByGame_Id(gameId);

        if (participants.size() < 2) {
            throw new IllegalStateException(
                    "At least 2 participants are required to start the game");
        }

        for (GameParticipant participant : participants) {

            boolean hasBuyIn =
                    buyInRepo.existsByParticipant_Id(participant.getId());

            if (!hasBuyIn) {
                throw new IllegalStateException(
                        "Every participant must have at least one buy-in");
            }
        }

        game.setGameStatus(GameState.RUNNING);
        game.setRoundActive(true);

        return gameRepo.save(game);
    }

    @Transactional
    public Game updateRoundState(
            Long gameId,
            @Valid UpdateRoundStateRequest request) throws AccessDeniedException {

        Game game = getGameForManager(gameId);

        if (game.getGameStatus() != GameState.RUNNING) {
            throw new IllegalStateException(
                    "Round state can only be changed while the game is RUNNING");
        }

        if (game.isRoundActive() == request.roundActive()) {
            throw new IllegalStateException(
                    "The game is already in the requested round state");
        }

        game.setRoundActive(request.roundActive());

        return gameRepo.save(game);
    }

    @Transactional
    public Game endGame(Long gameId) throws AccessDeniedException {

        Game game = getGameForManager(gameId);

        if (game.getGameStatus() != GameState.RUNNING) {
            throw new IllegalStateException(
                    "Only a RUNNING game can be ended");
        }

        if (game.isRoundActive()) {
            throw new IllegalStateException(
                    "The active round must be stopped before ending the game");
        }

        game.setGameStatus(GameState.ENDED);
        game.setRoundActive(false);

        return gameRepo.save(game);
    }

    @Transactional
    public Game updateFinalChipValues(
            Long gameId,
            UpdateFinalChipValuesRequest request) throws AccessDeniedException {

        Game game = getGameForManager(gameId);

        if (game.getGameStatus() != GameState.ENDED) {
            throw new IllegalStateException(
                    "Final chip values can only be updated after the game has ended");
        }

        List<GameParticipant> participants =
                gameParticipantRepo.findByGame_Id(gameId);

        Map<Long, GameParticipant> participantMap =
                participants.stream()
                        .collect(Collectors.toMap(
                                GameParticipant::getId,
                                Function.identity()
                        ));

        Map<Long, Long> requestedChipValues = new HashMap<>();

        for (FinalChipValueRequest chipValueRequest
                : request.finalChipValues()) {

            Long participantId = chipValueRequest.participantId();

            if (requestedChipValues.containsKey(participantId)) {
                throw new IllegalArgumentException(
                        "Duplicate participant ID: " + participantId);
            }

            if (!participantMap.containsKey(participantId)) {
                throw new IllegalArgumentException(
                        "Participant does not belong to this game: "
                                + participantId);
            }

            requestedChipValues.put(
                    participantId,
                    chipValueRequest.chipValue()
            );
        }

        if (requestedChipValues.size() != participants.size()) {
            throw new IllegalArgumentException(
                    "Final chip values must be provided for every participant");
        }

        for (Map.Entry<Long, Long> entry
                : requestedChipValues.entrySet()) {

            GameParticipant participant =
                    participantMap.get(entry.getKey());

            participant.setFinalChipValue(entry.getValue());
        }

        gameParticipantRepo.saveAll(participants);

        return game;
    }

    private Game getGameForManager(Long gameId) throws AccessDeniedException{

        Game game = gameRepo.findById(gameId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Game not found"));

        Long currentUserId =
                currentUserService.getCurrentUser().getId();

        if (!game.getCreatedBy().getId().equals(currentUserId)) {
            throw new AccessDeniedException(
                    "Only the game manager can perform this action");
        }

        return game;
    }
}
