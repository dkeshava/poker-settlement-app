package com.dk.pokersettlement.services;

import com.dk.pokersettlement.components.MinimumTransactionsCalculator;
import com.dk.pokersettlement.dtos.CalculatedTransfer;
import com.dk.pokersettlement.dtos.SettlementPreviewResponse;
import com.dk.pokersettlement.dtos.SettlementResponse;
import com.dk.pokersettlement.dtos.SettlementTransfer;
import com.dk.pokersettlement.enums.GameState;
import com.dk.pokersettlement.models.*;
import com.dk.pokersettlement.repositories.BuyInRepo;
import com.dk.pokersettlement.repositories.GameParticipantRepo;
import com.dk.pokersettlement.repositories.GameRepo;
import com.dk.pokersettlement.repositories.SettlementTransactionRepo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.nio.file.AccessDeniedException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SettlementServiceTest {

    @Mock
    private BuyInRepo buyInRepo;

    @Spy
    private MinimumTransactionsCalculator calculator =
            new MinimumTransactionsCalculator();

    @Mock
    private GameRepo gameRepo;

    @Mock
    private GameParticipantRepo gameParticipantRepo;

    @Mock
    private GameService gameService;

    @Mock
    private CurrentUserService currentUserService;

    @Mock
    private SettlementTransactionRepo settlementTransactionRepo;

    @InjectMocks
    private SettlementService settlementService;

    private Game game;
    private User manager;
    private User userA;
    private User userB;

    private GameParticipant participantA;
    private GameParticipant participantB;

    private final Long GAME_ID = 1L;
    private final Long MANAGER_ID = 100L;
    private final Long USER_A_ID = 101L;
    private final Long USER_B_ID = 102L;

    @BeforeEach
    void setUp() throws AccessDeniedException {

        manager = new User();
        manager.setId(MANAGER_ID);

        userA = new User();
        userA.setId(USER_A_ID);
        userA.setUsername("Alice");

        userB = new User();
        userB.setId(USER_B_ID);
        userB.setUsername("Bob");

        game = new Game();
        game.setId(GAME_ID);
        game.setCreatedBy(manager);
        game.setGameStatus(GameState.ENDED);
        game.setChipConversionRate(new BigDecimal("1"));

        participantA = new GameParticipant();
        participantA.setId(1L);
        participantA.setGame(game);
        participantA.setUser(userA);
        participantA.setFinalChipValue(50L);

        participantB = new GameParticipant();
        participantB.setId(2L);
        participantB.setGame(game);
        participantB.setUser(userB);
        participantB.setFinalChipValue(150L);

        User currentUser = new User();
        currentUser.setId(MANAGER_ID);

        lenient().when(currentUserService.getCurrentUser())
                .thenReturn(currentUser);
    }

    private BuyIn createBuyIn(
            Long id,
            GameParticipant participant,
            Long chipValue) {

        BuyIn buyIn = new BuyIn();
        buyIn.setId(id);
        buyIn.setParticipant(participant);
        buyIn.setChipValue(chipValue);

        return buyIn;
    }

    @Test
    void getSettlementPreview_shouldReturnValidPreview() throws AccessDeniedException {

        when(gameRepo.findById(GAME_ID))
                .thenReturn(Optional.of(game));

        when(gameService.isManagerOrParticipant(MANAGER_ID, GAME_ID))
                .thenReturn(true);

        when(gameParticipantRepo.findByGame_Id(GAME_ID))
                .thenReturn(new ArrayList<>(
                        List.of(participantA, participantB)
                ));

        when(buyInRepo.findByParticipantIdIn(anyList()))
                .thenReturn(List.of(
                        createBuyIn(1L, participantA, 100L),
                        createBuyIn(2L, participantB, 100L)
                ));

        SettlementPreviewResponse response =
                settlementService.getSettlementPreview(GAME_ID);

        assertEquals(GAME_ID, response.gameId());
        assertEquals(2, response.netBalances().size());
        assertEquals(1, response.transactionCount());

        assertEquals(
                new BigDecimal("-50"),
                response.netBalances().get(0).netAmount()
        );

        assertEquals(
                new BigDecimal("50"),
                response.netBalances().get(1).netAmount()
        );

        SettlementTransfer transfer = response.transfers().get(0);

        assertEquals(1L, transfer.fromParticipantId());
        assertEquals("Alice", transfer.fromUsername());

        assertEquals(2L, transfer.toParticipantId());
        assertEquals("Bob", transfer.toUsername());

        assertEquals(new BigDecimal("50"), transfer.amount());
    }

    @Test
    void getSettlementPreview_shouldRejectUnequalChipTotals() {

        when(gameRepo.findById(GAME_ID))
                .thenReturn(Optional.of(game));

        when(gameService.isManagerOrParticipant(MANAGER_ID, GAME_ID))
                .thenReturn(true);

        when(gameParticipantRepo.findByGame_Id(GAME_ID))
                .thenReturn(new ArrayList<>(
                        List.of(participantA, participantB)
                ));

        when(buyInRepo.findByParticipantIdIn(anyList()))
                .thenReturn(List.of(
                        createBuyIn(1L, participantA, 100L),
                        createBuyIn(2L, participantB, 100L)
                ));

        // Final chips: 50 + 120 = 170
        // Buy-ins: 100 + 100 = 200

        participantB.setFinalChipValue(120L);

        assertThrows(
                RuntimeException.class,
                () -> settlementService.getSettlementPreview(GAME_ID)
        );
    }

    @Test
    void finalizeSettlement_shouldSaveTransactionsAndSetGameSettled() throws AccessDeniedException {

        when(gameRepo.findByIdForUpdate(GAME_ID))
                .thenReturn(Optional.of(game));

        when(gameService.isManager(MANAGER_ID, GAME_ID))
                .thenReturn(true);

        when(gameParticipantRepo.findByGame_Id(GAME_ID))
                .thenReturn(new ArrayList<>(
                        List.of(participantA, participantB)
                ));

        when(buyInRepo.findByParticipantIdIn(anyList()))
                .thenReturn(List.of(
                        createBuyIn(1L, participantA, 100L),
                        createBuyIn(2L, participantB, 100L)
                ));


        SettlementResponse response =
                settlementService.finalizeSettlement(GAME_ID);

        assertEquals(GameState.SETTLED, game.getGameStatus());

        assertEquals(1, response.transactionCount());

        verify(settlementTransactionRepo).saveAll(anyList());

        verify(gameRepo).save(game);
    }

    @Test
    void finalizeSettlement_shouldRejectNonManager() throws AccessDeniedException {

        User participant = new User();
        participant.setId(USER_A_ID);

        when(gameRepo.findByIdForUpdate(GAME_ID))
                .thenReturn(Optional.of(game));

        when(currentUserService.getCurrentUser())
                .thenReturn(participant);

        when(gameService.isManager(USER_A_ID, GAME_ID))
                .thenReturn(false);

        assertThrows(
                RuntimeException.class,
                () -> settlementService.finalizeSettlement(GAME_ID)
        );

        verify(settlementTransactionRepo, never()).saveAll(anyList());

        verify(gameRepo, never()).save(any(Game.class));
    }

    @Test
    void getSettlement_shouldReturnStoredTransactions() throws AccessDeniedException {

        game.setGameStatus(GameState.SETTLED);

        SettlementTransaction transaction =
                new SettlementTransaction(
                        1L,
                        game,
                        participantA,
                        participantB,
                        new BigDecimal("50")
                );

        when(gameRepo.findById(GAME_ID))
                .thenReturn(Optional.of(game));

        when(gameService.isManagerOrParticipant(MANAGER_ID, GAME_ID))
                .thenReturn(true);

        when(settlementTransactionRepo.findByGameId(GAME_ID))
                .thenReturn(List.of(transaction));

        SettlementResponse response =
                settlementService.getSettlement(GAME_ID);

        assertEquals(1, response.transactionCount());

        assertEquals(
                new BigDecimal("50"),
                response.transfers().get(0).amount()
        );

        assertEquals("Alice", response.transfers().get(0).fromUsername());
        assertEquals("Bob", response.transfers().get(0).toUsername());

        // Important: retrieval must not recalculate.
        verifyNoInteractions(buyInRepo);
    }

    @Test
    void getSettlementPreview_whenBreakEven_returnsNoTransfers() throws Exception {
        participantA.setFinalChipValue(100L);
        participantB.setFinalChipValue(100L);

        when(gameRepo.findById(GAME_ID))
                .thenReturn(Optional.of(game));

        when(gameService.isManagerOrParticipant(MANAGER_ID, GAME_ID))
                .thenReturn(true);

        when(gameParticipantRepo.findByGame_Id(GAME_ID))
                .thenReturn(new ArrayList<>(List.of(participantA, participantB)));

        when(buyInRepo.findByParticipantIdIn(anyList()))
                .thenReturn(List.of(
                        createBuyIn(1L, participantA, 100L),
                        createBuyIn(2L, participantB, 100L)
                ));

        SettlementPreviewResponse response =
                settlementService.getSettlementPreview(GAME_ID);

        assertEquals(2, response.netBalances().size());
        assertEquals(0, response.transactionCount());
        assertTrue(response.transfers().isEmpty());

        assertEquals(
                new BigDecimal("0"),
                response.netBalances().get(0).netAmount()
        );

        assertEquals(
                new BigDecimal("0"),
                response.netBalances().get(1).netAmount()
        );
    }

    @Test
    void finalizeSettlement_whenBreakEven_marksGameSettledWithNoTransactions()
            throws Exception {

        participantA.setFinalChipValue(100L);
        participantB.setFinalChipValue(100L);

        when(gameRepo.findByIdForUpdate(GAME_ID))
                .thenReturn(Optional.of(game));

        when(gameService.isManager(MANAGER_ID, GAME_ID))
                .thenReturn(true);

        when(gameParticipantRepo.findByGame_Id(GAME_ID))
                .thenReturn(new ArrayList<>(List.of(participantA, participantB)));

        when(buyInRepo.findByParticipantIdIn(anyList()))
                .thenReturn(List.of(
                        createBuyIn(1L, participantA, 100L),
                        createBuyIn(2L, participantB, 100L)
                ));

        SettlementResponse response =
                settlementService.finalizeSettlement(GAME_ID);

        assertEquals(GameState.SETTLED, game.getGameStatus());
        assertEquals(0, response.transactionCount());
        assertTrue(response.transfers().isEmpty());

        verify(settlementTransactionRepo).saveAll(anyList());
        verify(gameRepo).save(game);
    }

    @Test
    void getSettlement_whenBreakEven_returnsEmptyStoredSettlement()
            throws Exception {

        game.setGameStatus(GameState.SETTLED);

        when(gameRepo.findById(GAME_ID))
                .thenReturn(Optional.of(game));

        when(gameService.isManagerOrParticipant(MANAGER_ID, GAME_ID))
                .thenReturn(true);

        when(settlementTransactionRepo.findByGameId(GAME_ID))
                .thenReturn(List.of());

        SettlementResponse response =
                settlementService.getSettlement(GAME_ID);

        assertEquals(GAME_ID, response.gameId());
        assertEquals(0, response.transactionCount());
        assertTrue(response.transfers().isEmpty());

        verifyNoInteractions(buyInRepo);
    }

    @Test
    void getSettlementPreview_whenParticipantHasNoBuyIn_throwsException()
            throws Exception {

        when(gameRepo.findById(GAME_ID))
                .thenReturn(Optional.of(game));

        when(gameService.isManagerOrParticipant(MANAGER_ID, GAME_ID))
                .thenReturn(true);

        when(gameParticipantRepo.findByGame_Id(GAME_ID))
                .thenReturn(new ArrayList<>(List.of(participantA, participantB)));

        // Only Alice has a buy-in.
        when(buyInRepo.findByParticipantIdIn(anyList()))
                .thenReturn(List.of(
                        createBuyIn(1L, participantA, 100L)
                ));

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> settlementService.getSettlementPreview(GAME_ID)
        );

        assertTrue(exception.getMessage().contains("buy-in"));
    }

    @Test
    void getSettlementPreview_whenFinalChipValueMissing_throwsException()
            throws Exception {

        participantA.setFinalChipValue(200L);
        participantB.setFinalChipValue(null);

        when(gameRepo.findById(GAME_ID))
                .thenReturn(Optional.of(game));

        when(gameService.isManagerOrParticipant(MANAGER_ID, GAME_ID))
                .thenReturn(true);

        when(gameParticipantRepo.findByGame_Id(GAME_ID))
                .thenReturn(new ArrayList<>(List.of(participantA, participantB)));

        when(buyInRepo.findByParticipantIdIn(anyList()))
                .thenReturn(List.of(
                        createBuyIn(1L, participantA, 100L),
                        createBuyIn(2L, participantB, 100L)
                ));

        assertThrows(
                RuntimeException.class,
                () -> settlementService.getSettlementPreview(GAME_ID)
        );
    }

    @Test
    void getSettlementPreview_whenFinalChipValueIsNegative_throwsException()
            throws Exception {

        participantA.setFinalChipValue(-1L);
        participantB.setFinalChipValue(201L);

        when(gameRepo.findById(GAME_ID))
                .thenReturn(Optional.of(game));

        when(gameService.isManagerOrParticipant(MANAGER_ID, GAME_ID))
                .thenReturn(true);

        when(gameParticipantRepo.findByGame_Id(GAME_ID))
                .thenReturn(new ArrayList<>(List.of(participantA, participantB)));

        when(buyInRepo.findByParticipantIdIn(anyList()))
                .thenReturn(List.of(
                        createBuyIn(1L, participantA, 100L),
                        createBuyIn(2L, participantB, 100L)
                ));

        assertThrows(
                RuntimeException.class,
                () -> settlementService.getSettlementPreview(GAME_ID)
        );
    }

    @Test
    void getSettlementPreview_whenConversionRateIsZero_throwsException()
            throws Exception {

        game.setChipConversionRate(BigDecimal.ZERO);

        when(gameRepo.findById(GAME_ID))
                .thenReturn(Optional.of(game));

        when(gameService.isManagerOrParticipant(MANAGER_ID, GAME_ID))
                .thenReturn(true);

        assertThrows(
                RuntimeException.class,
                () -> settlementService.getSettlementPreview(GAME_ID)
        );
    }

    @Test
    void getSettlementPreview_whenGameIsRunning_throwsException()
            throws Exception {

        game.setGameStatus(GameState.RUNNING);

        when(gameRepo.findById(GAME_ID))
                .thenReturn(Optional.of(game));

        when(gameService.isManagerOrParticipant(MANAGER_ID, GAME_ID))
                .thenReturn(true);

        assertThrows(
                RuntimeException.class,
                () -> settlementService.getSettlementPreview(GAME_ID)
        );

        verifyNoInteractions(buyInRepo);
    }

    @Test
    void finalizeSettlement_whenGameAlreadySettled_throwsException()
            throws Exception {

        game.setGameStatus(GameState.SETTLED);

        when(gameRepo.findByIdForUpdate(GAME_ID))
                .thenReturn(Optional.of(game));

        when(gameService.isManager(MANAGER_ID, GAME_ID))
                .thenReturn(true);

        assertThrows(
                RuntimeException.class,
                () -> settlementService.finalizeSettlement(GAME_ID)
        );

        verifyNoInteractions(buyInRepo);
        verifyNoInteractions(settlementTransactionRepo);
        verify(gameRepo, never()).save(any(Game.class));
    }

    @Test
    void getSettlement_whenUserIsNotParticipantOrManager_throwsException()
            throws Exception {

        when(gameRepo.findById(GAME_ID))
                .thenReturn(Optional.of(game));

        when(gameService.isManagerOrParticipant(MANAGER_ID, GAME_ID))
                .thenReturn(false);

        assertThrows(
                RuntimeException.class,
                () -> settlementService.getSettlementPreview(GAME_ID)
        );

        verifyNoInteractions(buyInRepo);
    }

    @Test
    void calculate_whenAllBalancesAreZero_returnsEmptyList() {

        List<BigDecimal> balances = List.of(
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO
        );

        List<Long> participantIds = List.of(1L, 2L, 3L);

        List<CalculatedTransfer> result =
                calculator.calculate(balances, participantIds);

        assertTrue(result.isEmpty());
    }

    @Test
    void calculate_whenOneDebtorAndOneCreditor_returnsOneTransfer() {

        List<BigDecimal> balances = List.of(
                new BigDecimal("-50"),
                new BigDecimal("50")
        );

        List<Long> participantIds = List.of(1L, 2L);

        List<CalculatedTransfer> result =
                calculator.calculate(balances, participantIds);

        assertEquals(1, result.size());

        CalculatedTransfer transfer = result.get(0);

        assertEquals(1L, transfer.fromParticipantId());
        assertEquals(2L, transfer.toParticipantId());
        assertEquals(new BigDecimal("50"), transfer.amount());
    }

    @Test
    void calculate_whenBalancesAreRepeated_returnsMinimumTransactions() {

        List<BigDecimal> balances = List.of(
                new BigDecimal("-5"),
                new BigDecimal("-5"),
                new BigDecimal("6"),
                new BigDecimal("4")
        );

        List<Long> participantIds = List.of(1L, 2L, 3L, 4L);

        List<CalculatedTransfer> result =
                calculator.calculate(balances, participantIds);

        assertEquals(3, result.size());
    }
}
