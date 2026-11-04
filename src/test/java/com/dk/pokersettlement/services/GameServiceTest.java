package com.dk.pokersettlement.services;


import com.dk.pokersettlement.dtos.CreateGameRequest;
import com.dk.pokersettlement.enums.GameState;
import com.dk.pokersettlement.exceptions.ResourceNotFoundException;
import com.dk.pokersettlement.models.Game;
import com.dk.pokersettlement.models.User;
import com.dk.pokersettlement.repositories.BuyInRepo;
import com.dk.pokersettlement.repositories.GameParticipantRepo;
import com.dk.pokersettlement.repositories.GameRepo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GameServiceTest {

    @Mock
    private GameRepo gameRepo;

    @Mock
    private CurrentUserService currentUserService;

    @Mock
    private GameParticipantRepo gameParticipantRepo;

    @Mock
    private BuyInRepo buyInRepo;

    @InjectMocks
    private GameService gameService;

    private User manager;
    private User participant;
    private User outsider;
    private Game game;

    @BeforeEach
    void setUp() {
        manager = new User();
        manager.setId(1L);
        manager.setUsername("manager");
        manager.setPassword("password");

        participant = new User();
        participant.setId(2L);
        participant.setUsername("participant");
        participant.setPassword("password");

        outsider = new User();
        outsider.setId(3L);
        outsider.setUsername("outsider");
        outsider.setPassword("password");

        game = new Game();
        game.setId(100L);
        game.setCreatedBy(manager);
        game.setGameStatus(GameState.OPEN);
        game.setRoundActive(false);
        game.setChipConversionRate(new BigDecimal("0.10"));
    }

    // 1. Create game successfully
    @Test
    void createGame_shouldSaveGameWithCorrectDetails() {

        CreateGameRequest request = new CreateGameRequest();
        request.setChipConversionRate(new BigDecimal("0.10"));

        when(currentUserService.getCurrentUser()).thenReturn(manager);
        when(gameRepo.save(any(Game.class))).thenAnswer(invocation ->
                invocation.getArgument(0));

        Game result = gameService.createGame(request);

        assertNotNull(result);
        assertEquals(manager, result.getCreatedBy());
        assertEquals(GameState.OPEN, result.getGameStatus());
        assertFalse(result.isRoundActive());
        assertEquals(0, result.getChipConversionRate()
                .compareTo(new BigDecimal("0.10")));

        verify(gameRepo).save(any(Game.class));
    }

    // 2. Manager can access game
    @Test
    void getGame_shouldAllowManager() {
        when(currentUserService.getCurrentUser()).thenReturn(manager);

        when(gameRepo.findById(100L)).thenReturn(Optional.of(game));

        Game result = gameService.getGame(100L);

        assertEquals(game, result);
        verify(gameParticipantRepo, never())
                .existsByGame_IdAndUser_Id(anyLong(), anyLong());
    }

    // 3. Participant can access game
    @Test
    void getGame_shouldAllowParticipant() {

        when(gameRepo.findById(100L)).thenReturn(Optional.of(game));
        when(currentUserService.getCurrentUser()).thenReturn(participant);
        when(gameParticipantRepo.existsByGame_IdAndUser_Id(100L, 2L))
                .thenReturn(true);

        Game result = gameService.getGame(100L);

        assertEquals(game, result);
    }

    // 4. Outsider cannot access game
    @Test
    void getGame_shouldThrowAccessDeniedForOutsider() {

        when(gameRepo.findById(100L)).thenReturn(Optional.of(game));
        when(currentUserService.getCurrentUser()).thenReturn(outsider);
        when(gameParticipantRepo.existsByGame_IdAndUser_Id(100L, 3L))
                .thenReturn(false);

        assertThrows(
                AccessDeniedException.class,
                () -> gameService.getGame(100L)
        );
    }

    // 5. Game does not exist
    @Test
    void getGame_shouldThrowWhenGameDoesNotExist() {

        when(gameRepo.findById(100L)).thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> gameService.getGame(100L)
        );
    }

    // 6. Manager check should return true
    @Test
    void isManager_shouldReturnTrueForCreator() {

        when(gameRepo.findById(100L)).thenReturn(Optional.of(game));

        assertTrue(gameService.isManager(1L, 100L));
    }

    // 7. Manager check should return false for non-manager
    @Test
    void isManager_shouldReturnFalseForNonManager() {

        when(gameRepo.findById(100L)).thenReturn(Optional.of(game));

        assertFalse(gameService.isManager(2L, 100L));
    }

    // 8. Manager or participant check should return true for manager
    @Test
    void isManagerOrParticipant_shouldReturnTrueForManager() {

        when(gameRepo.findById(100L)).thenReturn(Optional.of(game));

        assertTrue(gameService.isManagerOrParticipant(1L, 100L));

        verify(gameParticipantRepo, never())
                .existsByGame_IdAndUser_Id(anyLong(), anyLong());
    }

    // 9. Manager or participant check should return true for participant
    @Test
    void isManagerOrParticipant_shouldReturnTrueForParticipant() {

        when(gameRepo.findById(100L)).thenReturn(Optional.of(game));
        when(gameParticipantRepo.existsByGame_IdAndUser_Id(100L, 2L))
                .thenReturn(true);

        assertTrue(gameService.isManagerOrParticipant(2L, 100L));
    }

    // 10. Manager or participant check should return false for outsider
    @Test
    void isManagerOrParticipant_shouldReturnFalseForOutsider() {

        when(gameRepo.findById(100L)).thenReturn(Optional.of(game));
        when(gameParticipantRepo.existsByGame_IdAndUser_Id(100L, 3L))
                .thenReturn(false);

        assertFalse(gameService.isManagerOrParticipant(3L, 100L));
    }
}
