package com.dk.pokersettlement.services;

import com.dk.pokersettlement.components.MinimumTransactionsCalculator;
import com.dk.pokersettlement.dtos.*;
import com.dk.pokersettlement.enums.GameState;
import com.dk.pokersettlement.models.BuyIn;
import com.dk.pokersettlement.models.Game;
import com.dk.pokersettlement.models.GameParticipant;
import com.dk.pokersettlement.models.SettlementTransaction;
import com.dk.pokersettlement.repositories.BuyInRepo;
import com.dk.pokersettlement.repositories.GameParticipantRepo;
import com.dk.pokersettlement.repositories.GameRepo;
import com.dk.pokersettlement.repositories.SettlementTransactionRepo;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import org.springframework.security.access.AccessDeniedException;
import java.util.*;

@Service
@RequiredArgsConstructor
public class SettlementService {

    private final BuyInRepo buyInRepo;
    private final GameRepo gameRepo;
    private final GameParticipantRepo gameParticipantRepo;
    private final GameService gameService;
    private final CurrentUserService currentUserService;
    private final SettlementTransactionRepo settlementTransactionRepo;

    private final MinimumTransactionsCalculator calculator;

    private record SettlementCalculationResult(
            SettlementPreviewResponse preview,
            Map<Long, GameParticipant> participantMap
    ) {}
    private SettlementCalculationResult calculateSettlement(Game game){
        BigDecimal chipConversionRate = game.getChipConversionRate();

        if (chipConversionRate == null
                || chipConversionRate.signum() <= 0) {
            throw new RuntimeException("Invalid chip conversion rate");
        }
        List<GameParticipant> participants =
                gameParticipantRepo.findByGame_Id(game.getId());

        if (participants.size() < 2) {
            throw new RuntimeException("At least two participants are required");
        }

        // Ensure deterministic calculator input.
        participants = new ArrayList<>(participants);
        participants.sort(Comparator.comparing(GameParticipant::getId));

        List<Long> participantIds = participants.stream()
                .map(GameParticipant::getId)
                .toList();

        List<BuyIn> buyIns =
                buyInRepo.findByParticipantIdIn(participantIds);

        Map<Long, Long> totalBuyInMap = new HashMap<>();

        for (BuyIn buyIn : buyIns) {

            if (buyIn.getChipValue() == null || buyIn.getChipValue() <= 0) {
                throw new RuntimeException("Invalid buy-in value");
            }

            Long participantId = buyIn.getParticipant().getId();

            totalBuyInMap.merge(
                    participantId,
                    buyIn.getChipValue(),
                    Math::addExact
            );
        }

        long totalBuyInChips = 0;
        long totalFinalChips = 0;

        List<ParticipantNetBalanceResponse> netBalances = new ArrayList<>();
        List<BigDecimal> balances = new ArrayList<>();

        Map<Long, GameParticipant> participantMap = new HashMap<>();

        for (GameParticipant participant : participants) {

            Long participantId = participant.getId();

            // Every participant must have at least one buy-in.
            Long totalBuyIn = totalBuyInMap.get(participantId);

            if (totalBuyIn == null) {
                throw new RuntimeException(
                        "Every participant must have at least one buy-in"
                );
            }

            Long finalChips = participant.getFinalChipValue();

            if (finalChips == null || finalChips < 0) {
                throw new IllegalStateException(
                        "Final chip value missing or invalid for participant: "
                                + participantId
                );
            }

            totalBuyInChips = Math.addExact(totalBuyInChips, totalBuyIn);
            totalFinalChips = Math.addExact(totalFinalChips, finalChips);

            long netChipValue = Math.subtractExact(finalChips, totalBuyIn);

            BigDecimal netAmount = BigDecimal.valueOf(netChipValue)
                    .multiply(chipConversionRate);

            balances.add(netAmount);

            netBalances.add(new ParticipantNetBalanceResponse(
                    participantId,
                    participant.getUser().getUsername(),
                    totalBuyIn,
                    finalChips,
                    netChipValue,
                    netAmount
            ));

            participantMap.put(participantId, participant);
        }

        // Accounting validation.
        if (totalBuyInChips != totalFinalChips) {
            throw new IllegalStateException(
                    "Total final chips must equal total buy-in chips"
            );
        }

        // Calculate minimum transactions.
        List<CalculatedTransfer> calculatedTransfers =
                calculator.calculate(balances, participantIds);

        // Convert calculator output into API response DTOs.
        List<SettlementTransfer> transfers = calculatedTransfers.stream()
                .map(transfer -> {

                    GameParticipant fromParticipant =
                            participantMap.get(transfer.fromParticipantId());

                    GameParticipant toParticipant =
                            participantMap.get(transfer.toParticipantId());

                    return new SettlementTransfer(
                            fromParticipant.getId(),
                            fromParticipant.getUser().getUsername(),
                            toParticipant.getId(),
                            toParticipant.getUser().getUsername(),
                            transfer.amount()
                    );
                })
                .toList();

        SettlementPreviewResponse preview = new SettlementPreviewResponse(
                game.getId(),
                chipConversionRate,
                netBalances,
                transfers,
                transfers.size()
        );

        return new SettlementCalculationResult(
                preview,
                participantMap
        );
    }

    @Transactional
    public SettlementResponse finalizeSettlement(Long gameId) throws AccessDeniedException {

        Game game = gameRepo.findByIdForUpdate(gameId)
                .orElseThrow(() -> new RuntimeException("Game not found"));

        Long currentUserId = currentUserService.getCurrentUser().getId();

        if (!gameService.isManager(currentUserId, gameId)) {
            throw new AccessDeniedException("Only the manager can finalize settlement");
        }

        if (game.getGameStatus() != GameState.ENDED) {
            throw new RuntimeException("Game must be ENDED to finalize settlement");
        }

        // Generate and validate the latest settlement plan.
        SettlementCalculationResult result = calculateSettlement(game);

        SettlementPreviewResponse preview = result.preview();

        Map<Long, GameParticipant> participantMap = result.participantMap();

        List<SettlementTransaction> transactions = preview.transfers()
                .stream()
                .map(transfer -> new SettlementTransaction(
                        null,
                        game,
                        participantMap.get(transfer.fromParticipantId()),
                        participantMap.get(transfer.toParticipantId()),
                        transfer.amount()
                ))
                .toList();

        settlementTransactionRepo.saveAll(transactions);

        game.setGameStatus(GameState.SETTLED);
        gameRepo.save(game);

        return new SettlementResponse(
                gameId,
                preview.transfers(),
                preview.transactionCount()
        );
    }
    public SettlementPreviewResponse getSettlementPreview(Long gameId) throws AccessDeniedException {

        Game game = gameRepo.findById(gameId)
                .orElseThrow(() -> new RuntimeException("Game not found"));

        Long currentUserId = currentUserService.getCurrentUser().getId();

        if (!gameService.isManagerOrParticipant(currentUserId, gameId)) {
            throw new AccessDeniedException("Access denied");
        }

        if (game.getGameStatus() != GameState.ENDED) {
            throw new RuntimeException("Game must be ENDED to preview settlement");
        }

        return calculateSettlement(game).preview();
    }

    public SettlementResponse getSettlement(Long gameId) throws AccessDeniedException {

        Game game = gameRepo.findById(gameId)
                .orElseThrow(() -> new RuntimeException("Game not found"));

        Long currentUserId = currentUserService.getCurrentUser().getId();

        if (!gameService.isManagerOrParticipant(currentUserId, gameId)) {
            throw new RuntimeException("Access denied");
        }

        if (game.getGameStatus() != GameState.SETTLED) {
            throw new RuntimeException("Settlement has not been finalized");
        }

        List<SettlementTransaction> transactions =
                settlementTransactionRepo.findByGameId(gameId);

        List<SettlementTransfer> transfers = transactions.stream()
                .map(transaction -> new SettlementTransfer(
                        transaction.getFromParticipant().getId(),
                        transaction.getFromParticipant().getUser().getUsername(),
                        transaction.getToParticipant().getId(),
                        transaction.getToParticipant().getUser().getUsername(),
                        transaction.getAmount()
                ))
                .toList();

        return new SettlementResponse(
                gameId,
                transfers,
                transfers.size()
        );
    }
}
