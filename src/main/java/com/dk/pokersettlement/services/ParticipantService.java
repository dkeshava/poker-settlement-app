package com.dk.pokersettlement.services;

import com.dk.pokersettlement.dtos.AddParticipantRequest;
import com.dk.pokersettlement.dtos.ParticipantResponse;
import com.dk.pokersettlement.dtos.BuyInResponse;
import com.dk.pokersettlement.enums.GameState;
import com.dk.pokersettlement.models.Game;
import com.dk.pokersettlement.models.GameParticipant;
import com.dk.pokersettlement.models.User;
import com.dk.pokersettlement.repositories.BuyInRepo;
import com.dk.pokersettlement.repositories.GameParticipantRepo;
import com.dk.pokersettlement.repositories.GameRepo;
import com.dk.pokersettlement.repositories.UserRepo;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import org.springframework.security.access.AccessDeniedException;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ParticipantService {
    private final GameParticipantRepo gameParticipantRepo;
    private final GameService gameService;
    private final GameRepo gameRepo;
    private final UserRepo userRepo;
    private final CurrentUserService currentUserService;
    private final BuyInRepo buyInRepo;

    private ParticipantResponse toParticipantResponse(GameParticipant participant) {

        ParticipantResponse response = new ParticipantResponse();

        response.setParticipantId(participant.getId());
        response.setGameId(participant.getGame().getId());
        response.setUserId(participant.getUser().getId());
        response.setUsername(participant.getUser().getUsername());
        response.setFinalChipValue(participant.getFinalChipValue());
        response.setBuyIns(buyInRepo.findByParticipant_Id(participant.getId()).stream()
                .map(buyIn -> new BuyInResponse(buyIn.getId(), participant.getId(), buyIn.getChipValue()))
                .toList());

        return response;
    }

    public List<ParticipantResponse> getParticipants(Long gameId)
            throws AccessDeniedException {

        Long currentUserId = currentUserService.getCurrentUser().getId();

        if (!gameService.isManagerOrParticipant(currentUserId, gameId)) {
            throw new AccessDeniedException("You cannot access this game");
        }

        return gameParticipantRepo.findByGame_Id(gameId)
                .stream()
                .map(this::toParticipantResponse)
                .toList();
    }

    public ParticipantResponse addParticipant(
            Long gameId,
            AddParticipantRequest request) throws AccessDeniedException {

        Long currentUserId =
                currentUserService.getCurrentUser().getId();

        // 1. Verify the caller manages this game
        if (!gameService.isManager(currentUserId, gameId)) {
            throw new AccessDeniedException(
                    "Only the game manager can add participants");
        }

        // 2. Load the game
        Game game = gameRepo.findById(gameId)
                .orElseThrow(() ->
                        new EntityNotFoundException("Game not found"));

        // 3. Check whether participants can be added
        boolean canAddParticipant =
                game.getGameStatus() == GameState.OPEN
                        || (game.getGameStatus() == GameState.RUNNING
                        && !game.isRoundActive());

        if (!canAddParticipant) {
            throw new IllegalStateException(
                    "Participants cannot be added in the current game state");
        }

        // 4. Find the user to add
        User user;
        if (request.getUserId() != null) {
            user = userRepo.findById(request.getUserId())
                    .orElseThrow(() ->
                            new EntityNotFoundException("User not found with ID: " + request.getUserId()));
        } else if (request.getUsername() != null && !request.getUsername().trim().isEmpty()) {
            user = userRepo.findByUsernameIgnoreCase(request.getUsername().trim())
                    .or(() -> userRepo.findOptionalByUsername(request.getUsername().trim()))
                    .orElseThrow(() ->
                            new EntityNotFoundException("User not found with username: " + request.getUsername()));
        } else {
            throw new IllegalArgumentException("Either userId or username must be provided");
        }

        // 5. Prevent duplicate participation
        if (gameParticipantRepo.existsByGame_IdAndUser_Id(
                gameId, user.getId())) {
            throw new IllegalArgumentException(
                    "User is already a participant in this game");
        }

        // 6. Create the relationship
        GameParticipant participant = new GameParticipant();
        participant.setGame(game);
        participant.setUser(user);
        participant.setFinalChipValue(null);

        GameParticipant saved =
                gameParticipantRepo.save(participant);

        // 7. Return a DTO, not the entity
        return new ParticipantResponse(
                saved.getId(),
                gameId,
                user.getId(),
                user.getUsername(),
                saved.getFinalChipValue(),
                List.of()
        );
    }

    @Transactional
    public ParticipantResponse deleteParticipant(Long gameId, Long participantId) throws AccessDeniedException {
        User currentUser = currentUserService.getCurrentUser();
        Long currentUserId =
                currentUser.getId();

        // 1. Only the manager can delete participants
        if (!gameService.isManager(currentUserId, gameId)) {
            throw new AccessDeniedException(
                    "Only the game manager can delete participants");
        }

        // 2. Load the game
        Game game = gameRepo.findById(gameId)
                .orElseThrow(() ->
                        new EntityNotFoundException("Game not found"));

        // 3. Check lifecycle
        boolean canModifyParticipants =
                game.getGameStatus() == GameState.OPEN
                        || (game.getGameStatus() == GameState.RUNNING
                        && !game.isRoundActive());

        if (!canModifyParticipants) {
            throw new IllegalStateException(
                    "Cannot delete participants in the current game state");
        }

        // 4. Ensure this participant belongs to this game
        GameParticipant participant =
                gameParticipantRepo.findByIdAndGame_Id(
                                participantId, gameId)
                        .orElseThrow(() ->
                                new EntityNotFoundException(
                                        "Participant not found in this game"));

        // 5. Prevent deletion if buy-ins exist
        if (buyInRepo.existsByParticipant_Id(participantId)) {
            throw new IllegalStateException(
                    "Cannot delete a participant who has buy-ins");
        }

        // 6. Delete only after all checks pass
        gameParticipantRepo.delete(participant);

        return new ParticipantResponse(
                participant.getId(),
                gameId,
                participant.getUser().getId(),
                participant.getUser().getUsername(),
                participant.getFinalChipValue(),
                List.of()
        );
    }
}
