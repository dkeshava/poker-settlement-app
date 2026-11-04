package com.dk.pokersettlement.services;

import com.dk.pokersettlement.dtos.*;
import com.dk.pokersettlement.enums.GameState;
import com.dk.pokersettlement.exceptions.ResourceNotFoundException;
import com.dk.pokersettlement.models.BuyIn;
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
class BuyInServiceTest {

    @Mock private BuyInRepo buyInRepo;
    @Mock private GameRepo gameRepo;
    @Mock private GameParticipantRepo gameParticipantRepo;
    @Mock private GameService gameService;
    @Mock private CurrentUserService currentUserService;

    @InjectMocks
    private BuyInService buyInService;

    private Game game;
    private User manager;
    private User outsider;
    private GameParticipant participant;
    private BuyIn buyIn;

    private final Long GAME_ID = 1L;
    private final Long MANAGER_ID = 10L;
    private final Long PARTICIPANT_ID = 100L;
    private final Long BUYIN_ID = 200L;

    @BeforeEach
    void setUp() {
        manager = new User();
        manager.setId(MANAGER_ID);
        manager.setUsername("manager");

        outsider = new User();
        outsider.setId(30L);

        game = new Game();
        game.setId(GAME_ID);
        game.setCreatedBy(manager);
        game.setGameStatus(GameState.OPEN);
        game.setRoundActive(false);

        participant = new GameParticipant();
        participant.setId(PARTICIPANT_ID);
        participant.setGame(game);
        participant.setUser(manager);

        buyIn = new BuyIn();
        buyIn.setId(BUYIN_ID);
        buyIn.setParticipant(participant);
        buyIn.setChipValue(100L);
    }

    private void mockManager() {
        when(gameRepo.findById(GAME_ID)).thenReturn(Optional.of(game));
        when(currentUserService.getCurrentUser()).thenReturn(manager);
        when(gameService.isManager(MANAGER_ID, GAME_ID)).thenReturn(true);
    }

    // 1. Add buy-in successfully
    @Test
    void addBuyIn_shouldSaveBuyIn() {
        mockManager();

        CreateBuyInRequest request = new CreateBuyInRequest(100L);

        when(gameParticipantRepo.findByIdAndGame_Id(
                PARTICIPANT_ID, GAME_ID))
                .thenReturn(Optional.of(participant));

        when(buyInRepo.save(any(BuyIn.class)))
                .thenAnswer(invocation -> {
                    BuyIn saved = invocation.getArgument(0);
                    saved.setId(BUYIN_ID);
                    return saved;
                });

        BuyInResponse response =
                buyInService.addBuyIn(GAME_ID, PARTICIPANT_ID, request);

        assertEquals(BUYIN_ID, response.buyInId());
        assertEquals(PARTICIPANT_ID, response.participantId());
        assertEquals(100L, response.chipValue());

        verify(buyInRepo).save(any(BuyIn.class));
    }

    // 2. Multiple buy-ins are allowed
    @Test
    void addBuyIn_shouldAllowMultipleBuyIns() {
        mockManager();

        CreateBuyInRequest request = new CreateBuyInRequest(50L);

        when(gameParticipantRepo.findByIdAndGame_Id(
                PARTICIPANT_ID, GAME_ID))
                .thenReturn(Optional.of(participant));

        when(buyInRepo.save(any(BuyIn.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        buyInService.addBuyIn(GAME_ID, PARTICIPANT_ID, request);
        buyInService.addBuyIn(GAME_ID, PARTICIPANT_ID, request);

        verify(buyInRepo, times(2)).save(any(BuyIn.class));
    }

    // 3. Non-manager cannot add buy-in
    @Test
    void addBuyIn_shouldRejectNonManager() {
        when(gameRepo.findById(GAME_ID)).thenReturn(Optional.of(game));
        when(currentUserService.getCurrentUser()).thenReturn(outsider);
        when(gameService.isManager(30L, GAME_ID)).thenReturn(false);

        assertThrows(
                AccessDeniedException.class,
                () -> buyInService.addBuyIn(
                        GAME_ID, PARTICIPANT_ID,
                        new CreateBuyInRequest(100L))
        );

        verify(buyInRepo, never()).save(any());
    }

    // 4. Participant must belong to the game
    @Test
    void addBuyIn_shouldRejectParticipantFromAnotherGame() {
        mockManager();

        when(gameParticipantRepo.findByIdAndGame_Id(
                PARTICIPANT_ID, GAME_ID))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> buyInService.addBuyIn(
                        GAME_ID, PARTICIPANT_ID,
                        new CreateBuyInRequest(100L))
        );

        verify(buyInRepo, never()).save(any());
    }

    // 5. Cannot add buy-in during active round
    @Test
    void addBuyIn_shouldRejectDuringActiveRound() {
        game.setGameStatus(GameState.RUNNING);
        game.setRoundActive(true);

        mockManager();

        assertThrows(
                IllegalStateException.class,
                () -> buyInService.addBuyIn(
                        GAME_ID, PARTICIPANT_ID,
                        new CreateBuyInRequest(100L))
        );

        verify(gameParticipantRepo, never())
                .findByIdAndGame_Id(anyLong(), anyLong());
    }

    // 6. Cannot add buy-in after game ends
    @Test
    void addBuyIn_shouldRejectAfterGameEnds() {
        game.setGameStatus(GameState.ENDED);
        mockManager();

        assertThrows(
                IllegalStateException.class,
                () -> buyInService.addBuyIn(
                        GAME_ID, PARTICIPANT_ID,
                        new CreateBuyInRequest(100L))
        );
    }

    // 7. Update buy-in successfully
    @Test
    void updateBuyIn_shouldUpdateChipValue() {
        mockManager();

        when(buyInRepo.findByIdAndParticipant_Game_Id(
                BUYIN_ID, GAME_ID))
                .thenReturn(Optional.of(buyIn));

        when(buyInRepo.save(any(BuyIn.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        UpdateBuyInRequest request = new UpdateBuyInRequest(200L);

        BuyInResponse response =
                buyInService.updateBuyIn(GAME_ID, BUYIN_ID, request);

        assertEquals(200L, response.chipValue());
        assertEquals(200L, buyIn.getChipValue());

        verify(buyInRepo).save(buyIn);
    }

    // 8. Cannot update buy-in belonging to another game
    @Test
    void updateBuyIn_shouldRejectBuyInFromAnotherGame() {
        mockManager();

        when(buyInRepo.findByIdAndParticipant_Game_Id(
                BUYIN_ID, GAME_ID))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> buyInService.updateBuyIn(
                        GAME_ID, BUYIN_ID,
                        new UpdateBuyInRequest(200L))
        );

        verify(buyInRepo, never()).save(any());
    }

    // 9. Delete buy-in successfully
    @Test
    void deleteBuyIn_shouldDeleteBuyIn() {
        mockManager();

        when(buyInRepo.findByIdAndParticipant_Game_Id(
                BUYIN_ID, GAME_ID))
                .thenReturn(Optional.of(buyIn));

        buyInService.deleteBuyIn(GAME_ID, BUYIN_ID);

        verify(buyInRepo).delete(buyIn);
    }

    // 10. Cannot delete buy-in after game ends
    @Test
    void deleteBuyIn_shouldRejectAfterGameEnds() {
        game.setGameStatus(GameState.ENDED);
        mockManager();

        assertThrows(
                IllegalStateException.class,
                () -> buyInService.deleteBuyIn(GAME_ID, BUYIN_ID)
        );

        verify(buyInRepo, never())
                .findByIdAndParticipant_Game_Id(anyLong(), anyLong());

        verify(buyInRepo, never()).delete(any());
    }

    // 11. Cannot update buy-in during active round
    @Test
    void updateBuyIn_shouldRejectDuringActiveRound() {
        game.setGameStatus(GameState.RUNNING);
        game.setRoundActive(true);
        mockManager();

        assertThrows(
                IllegalStateException.class,
                () -> buyInService.updateBuyIn(
                        GAME_ID, BUYIN_ID,
                        new UpdateBuyInRequest(200L))
        );

        verify(buyInRepo, never())
                .findByIdAndParticipant_Game_Id(anyLong(), anyLong());
    }

    // 12. Game does not exist
    @Test
    void addBuyIn_shouldRejectMissingGame() {
        when(gameRepo.findById(GAME_ID)).thenReturn(Optional.empty());

        assertThrows(
                EntityNotFoundException.class,
                () -> buyInService.addBuyIn(
                        GAME_ID, PARTICIPANT_ID,
                        new CreateBuyInRequest(100L))
        );

        verify(buyInRepo, never()).save(any());
    }
}

