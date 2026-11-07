package com.dk.pokersettlement.controllers;

import com.dk.pokersettlement.dtos.CreateGameRequest;
import com.dk.pokersettlement.dtos.GameResponse;
import com.dk.pokersettlement.dtos.UpdateFinalChipValuesRequest;
import com.dk.pokersettlement.dtos.UpdateRoundStateRequest;
import com.dk.pokersettlement.models.Game;
import com.dk.pokersettlement.repositories.GameParticipantRepo;
import com.dk.pokersettlement.repositories.GameRepo;
import com.dk.pokersettlement.services.CurrentUserService;
import com.dk.pokersettlement.services.GameService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import org.springframework.security.access.AccessDeniedException;
import java.util.List;

@Slf4j
@RestController
@CrossOrigin
@RequestMapping("/games")
public class GameController {

    private GameService gameService;
    private CurrentUserService currentUserService;
    private GameParticipantRepo gameParticipantRepo;
    private GameRepo gameRepo;

    public GameController(GameService gameService, CurrentUserService currentUserService,
                          GameParticipantRepo gameParticipantRepo, GameRepo gameRepo) {
        this.gameService = gameService;
        this.currentUserService = currentUserService;
        this.gameParticipantRepo = gameParticipantRepo;
        this.gameRepo = gameRepo;
    }

    private GameResponse toGameResponse(Game game) {
        GameResponse gameResponse = new GameResponse();

        gameResponse.setId(game.getId());
        gameResponse.setName(game.getName() != null && !game.getName().isBlank() ? game.getName() : "Poker Session #" + game.getId());
        gameResponse.setGameStatus(game.getGameStatus());
        gameResponse.setChipConversionRate(game.getChipConversionRate());
        gameResponse.setRoundActive(game.isRoundActive());
        gameResponse.setCreatedById(game.getCreatedBy().getId());
        gameResponse.setCreatedByUsername(game.getCreatedBy().getUsername());
        Long currentUserId = currentUserService.getCurrentUser().getId();
        gameResponse.setManager(game.getCreatedBy().getId().equals(currentUserId));
        gameResponse.setParticipant(gameParticipantRepo.existsByGame_IdAndUser_Id(game.getId(), currentUserId));

        return gameResponse;
    }

    @GetMapping
    public ResponseEntity<List<GameResponse>> getGames() {
        return ResponseEntity.ok(gameService.getAccessibleGames().stream()
                .map(this::toGameResponse)
                .toList());
    }

    @GetMapping("/{gameId}")
    public ResponseEntity<GameResponse> getGame(
            @PathVariable("gameId") Long gameId) {

        Game game = gameService.getGame(gameId);

        GameResponse gameResponse = toGameResponse(game);

        return ResponseEntity.ok(gameResponse);
    }

    //    @PostMapping("/")
    //    public ResponseEntity<Game> createGame(@Valid @RequestBody CreateGameRequest gameRequest) {
    //        Game game;
    //        try {
    //            game = gameService.createGame(gameRequest);
    //            return new ResponseEntity<>(game, HttpStatus.CREATED);
    //        }
    //        catch (Exception e) {
    //            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
    //        }
    //    }

    @PostMapping
    public ResponseEntity<GameResponse> createGame(
            @Valid @RequestBody CreateGameRequest gameRequest) {

        Game game = gameService.createGame(gameRequest);
        log.info("Created game id={} name='{}'", game.getId(), game.getName());

        GameResponse gameResponse = toGameResponse(game);

        return new ResponseEntity<>(gameResponse, HttpStatus.CREATED);
    }

    @DeleteMapping("/{gameId}")
    public ResponseEntity<Void> deleteGame(
            @PathVariable Long gameId) throws AccessDeniedException {

        gameService.deleteGame(gameId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{gameId}/start")
    public ResponseEntity<GameResponse> startGame(
            @PathVariable Long gameId) throws AccessDeniedException {

        Game game=gameService.startGame(gameId);
        return ResponseEntity.ok(toGameResponse(game));
    }

    @PatchMapping("/{gameId}/round-state")
    public ResponseEntity<GameResponse> updateRoundState(
            @PathVariable Long gameId,
            @Valid @RequestBody UpdateRoundStateRequest request) throws AccessDeniedException {

        Game game=gameService.updateRoundState(gameId, request);
        return ResponseEntity.ok(toGameResponse(game));
    }

    @PostMapping("/{gameId}/end")
    public ResponseEntity<GameResponse> endGame(
            @PathVariable Long gameId) throws AccessDeniedException {

        Game game=gameService.endGame(gameId);
        return ResponseEntity.ok(toGameResponse(game));
    }

    @PutMapping("/{gameId}/final-chip-values")
    public ResponseEntity<GameResponse> updateFinalChipValues(
            @PathVariable Long gameId,
            @Valid @RequestBody UpdateFinalChipValuesRequest request) throws AccessDeniedException {

        Game game=gameService.updateFinalChipValues(gameId, request);
        return ResponseEntity.ok(toGameResponse(game));
    }
}
