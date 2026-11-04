package com.dk.pokersettlement.services;
import com.dk.pokersettlement.dtos.AddParticipantRequest;
import com.dk.pokersettlement.dtos.CreateGameRequest;
import com.dk.pokersettlement.dtos.ParticipantResponse;
import com.dk.pokersettlement.enums.GameState;
import com.dk.pokersettlement.exceptions.ResourceNotFoundException;
import com.dk.pokersettlement.models.Game;
import com.dk.pokersettlement.models.GameParticipant;
import com.dk.pokersettlement.models.User;
import com.dk.pokersettlement.repositories.BuyInRepo;
import com.dk.pokersettlement.repositories.GameParticipantRepo;
import com.dk.pokersettlement.repositories.GameRepo;
import com.dk.pokersettlement.repositories.UserRepo;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ParticipantServiceTest {

    @Mock private GameParticipantRepo gameParticipantRepo;
    @Mock private GameService gameService;
    @Mock private GameRepo gameRepo;
    @Mock private UserRepo userRepo;
    @Mock private CurrentUserService currentUserService;
    @Mock private BuyInRepo buyInRepo;

    @InjectMocks
    private ParticipantService participantService;

    private Game game;
    private User manager;
    private User participantUser;
    private User outsider;
    private GameParticipant participant;

    private final Long GAME_ID = 1L;
    private final Long MANAGER_ID = 10L;
    private final Long USER_ID = 20L;

    @BeforeEach
    void setUp() {
        manager = new User();
        manager.setId(MANAGER_ID);
        manager.setUsername("manager");

        participantUser = new User();
        participantUser.setId(USER_ID);
        participantUser.setUsername("alice");

        outsider = new User();
        outsider.setId(30L);

        game = new Game();
        game.setId(GAME_ID);
        game.setCreatedBy(manager);
        game.setGameStatus(GameState.OPEN);
        game.setRoundActive(false);

        participant = new GameParticipant();
        participant.setId(100L);
        participant.setGame(game);
        participant.setUser(participantUser);
    }

    private void mockManager() {
        when(currentUserService.getCurrentUser()).thenReturn(manager);
        when(gameService.isManager(MANAGER_ID, GAME_ID)).thenReturn(true);
    }

    // 1. Get participants as manager
    @Test
    void getParticipants_shouldReturnParticipantsForManager() {
        when(currentUserService.getCurrentUser()).thenReturn(manager);
        when(gameService.isManagerOrParticipant(MANAGER_ID, GAME_ID))
                .thenReturn(true);
        when(gameParticipantRepo.findByGame_Id(GAME_ID))
                .thenReturn(List.of(participant));

        List<ParticipantResponse> result =
                participantService.getParticipants(GAME_ID);

        assertEquals(1, result.size());
        assertEquals(USER_ID, result.get(0).getUserId());
        assertEquals("alice", result.get(0).getUsername());
    }

    // 2. Get participants as participant
    @Test
    void getParticipants_shouldAllowParticipant() {
        when(currentUserService.getCurrentUser()).thenReturn(participantUser);
        when(gameService.isManagerOrParticipant(USER_ID, GAME_ID))
                .thenReturn(true);
        when(gameParticipantRepo.findByGame_Id(GAME_ID))
                .thenReturn(List.of(participant));

        assertEquals(1, participantService.getParticipants(GAME_ID).size());
    }

    // 3. Outsider cannot get participants
    @Test
    void getParticipants_shouldRejectOutsider() {
        when(currentUserService.getCurrentUser()).thenReturn(outsider);
        when(gameService.isManagerOrParticipant(30L, GAME_ID))
                .thenReturn(false);

        assertThrows(AccessDeniedException.class,
                () -> participantService.getParticipants(GAME_ID));

        verify(gameParticipantRepo, never()).findByGame_Id(GAME_ID);
    }

    // 4. Add participant successfully
    @Test
    void addParticipant_shouldSaveParticipant() {
        mockManager();

        AddParticipantRequest request = new AddParticipantRequest();
        request.setUserId(USER_ID);

        when(gameRepo.findById(GAME_ID)).thenReturn(Optional.of(game));
        when(userRepo.findById(USER_ID)).thenReturn(Optional.of(participantUser));
        when(gameParticipantRepo.existsByGame_IdAndUser_Id(GAME_ID, USER_ID))
                .thenReturn(false);
        when(gameParticipantRepo.save(any(GameParticipant.class)))
                .thenAnswer(invocation -> {
                    GameParticipant saved = invocation.getArgument(0);
                    saved.setId(100L);
                    return saved;
                });

        ParticipantResponse result =
                participantService.addParticipant(GAME_ID, request);

        assertEquals(100L, result.getParticipantId());
        assertEquals(GAME_ID, result.getGameId());
        assertEquals(USER_ID, result.getUserId());
        assertEquals("alice", result.getUsername());

        verify(gameParticipantRepo).save(any(GameParticipant.class));
    }

    // 5. Non-manager cannot add participant
    @Test
    void addParticipant_shouldRejectNonManager() {
        when(currentUserService.getCurrentUser()).thenReturn(outsider);
        when(gameService.isManager(30L, GAME_ID)).thenReturn(false);

        assertThrows(AccessDeniedException.class,
                () -> participantService.addParticipant(
                        GAME_ID, new AddParticipantRequest()));

        verify(gameRepo, never()).findById(GAME_ID);
    }

    // 6. Duplicate participant
    @Test
    void addParticipant_shouldRejectDuplicate() {
        mockManager();

        AddParticipantRequest request = new AddParticipantRequest();
        request.setUserId(USER_ID);

        when(gameRepo.findById(GAME_ID)).thenReturn(Optional.of(game));
        when(userRepo.findById(USER_ID)).thenReturn(Optional.of(participantUser));
        when(gameParticipantRepo.existsByGame_IdAndUser_Id(GAME_ID, USER_ID))
                .thenReturn(true);

        assertThrows(IllegalArgumentException.class,
                () -> participantService.addParticipant(GAME_ID, request));

        verify(gameParticipantRepo, never()).save(any());
    }

    // 7. Cannot add participant during active round
    @Test
    void addParticipant_shouldRejectDuringActiveRound() {
        mockManager();
        game.setGameStatus(GameState.RUNNING);
        game.setRoundActive(true);

        when(gameRepo.findById(GAME_ID)).thenReturn(Optional.of(game));

        assertThrows(IllegalStateException.class,
                () -> participantService.addParticipant(
                        GAME_ID, new AddParticipantRequest()));

        verify(userRepo, never()).findById(anyLong());
    }

    // 8. Cannot add participant after game ends
    @Test
    void addParticipant_shouldRejectAfterGameEnds() {
        mockManager();
        game.setGameStatus(GameState.ENDED);

        when(gameRepo.findById(GAME_ID)).thenReturn(Optional.of(game));

        assertThrows(IllegalStateException.class,
                () -> participantService.addParticipant(
                        GAME_ID, new AddParticipantRequest()));
    }

    // 9. Delete participant without buy-ins
    @Test
    void deleteParticipant_shouldDeleteParticipantWithoutBuyIns() {
        mockManager();

        when(gameRepo.findById(GAME_ID)).thenReturn(Optional.of(game));
        when(gameParticipantRepo.findByIdAndGame_Id(100L, GAME_ID))
                .thenReturn(Optional.of(participant));
        when(buyInRepo.existsByParticipant_Id(100L)).thenReturn(false);

        ParticipantResponse result =
                participantService.deleteParticipant(GAME_ID, 100L);

        assertEquals(100L, result.getParticipantId());
        verify(gameParticipantRepo).delete(participant);
    }

    // 10. Cannot delete participant with buy-ins
    @Test
    void deleteParticipant_shouldRejectWhenBuyInsExist() {
        mockManager();

        when(gameRepo.findById(GAME_ID)).thenReturn(Optional.of(game));
        when(gameParticipantRepo.findByIdAndGame_Id(100L, GAME_ID))
                .thenReturn(Optional.of(participant));
        when(buyInRepo.existsByParticipant_Id(100L)).thenReturn(true);

        assertThrows(IllegalStateException.class,
                () -> participantService.deleteParticipant(GAME_ID, 100L));

        verify(gameParticipantRepo, never()).delete(any());
    }

    // 11. Non-manager cannot delete
    @Test
    void deleteParticipant_shouldRejectNonManager() {
        when(currentUserService.getCurrentUser()).thenReturn(outsider);
        when(gameService.isManager(30L, GAME_ID)).thenReturn(false);

        assertThrows(AccessDeniedException.class,
                () -> participantService.deleteParticipant(GAME_ID, 100L));

        verify(gameRepo, never()).findById(GAME_ID);
    }

    // 12. Cannot delete during active round
    @Test
    void deleteParticipant_shouldRejectDuringActiveRound() {
        mockManager();
        game.setGameStatus(GameState.RUNNING);
        game.setRoundActive(true);

        when(gameRepo.findById(GAME_ID)).thenReturn(Optional.of(game));

        assertThrows(IllegalStateException.class,
                () -> participantService.deleteParticipant(GAME_ID, 100L));

        verify(gameParticipantRepo, never()).delete(any());
    }

    // 13. Participant ID must belong to requested game
    @Test
    void deleteParticipant_shouldRejectParticipantFromAnotherGame() {
        mockManager();

        when(gameRepo.findById(GAME_ID)).thenReturn(Optional.of(game));
        when(gameParticipantRepo.findByIdAndGame_Id(100L, GAME_ID))
                .thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class,
                () -> participantService.deleteParticipant(GAME_ID, 100L));

        verify(gameParticipantRepo, never()).delete(any());
    }

    // 14. Cannot add participant when game does not exist
    @Test
    void addParticipant_shouldRejectMissingGame() {
        mockManager();
        when(gameRepo.findById(GAME_ID)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class,
                () -> participantService.addParticipant(
                        GAME_ID, new AddParticipantRequest()));
    }
}
