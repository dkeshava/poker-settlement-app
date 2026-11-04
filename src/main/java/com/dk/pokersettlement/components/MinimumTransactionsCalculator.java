package com.dk.pokersettlement.components;

import com.dk.pokersettlement.dtos.CalculatedTransfer;
import com.dk.pokersettlement.dtos.CalculatedTransfer;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Component
public class MinimumTransactionsCalculator {

    public List<CalculatedTransfer> calculate(
            List<BigDecimal> netBalances,
            List<Long> participantIds) {

        List<BigDecimal> balances = new ArrayList<>(netBalances);

        List<CalculatedTransfer> currentTransfers =
                new ArrayList<>();

        List<CalculatedTransfer> bestTransfers =
                new ArrayList<>();

        backtrack(
                balances,
                participantIds,
                currentTransfers,
                bestTransfers
        );

        return bestTransfers;
    }

    private void backtrack(
            List<BigDecimal> balances,
            List<Long> participantIds,
            List<CalculatedTransfer> currentTransfers,
            List<CalculatedTransfer> bestTransfers) {

        // Find the first participant who is not settled.
        int currentIndex = 0;

        while (currentIndex < balances.size()
                && balances.get(currentIndex).signum() == 0) {
            currentIndex++;
        }

        // Everyone is settled.
        if (currentIndex == balances.size()) {
            if (bestTransfers.isEmpty()
                    || currentTransfers.size() < bestTransfers.size()) {

                bestTransfers.clear();
                bestTransfers.addAll(currentTransfers);
            }

            return;
        }

        // Pruning: this branch cannot improve the best solution.
        if (!bestTransfers.isEmpty()
                && currentTransfers.size() >= bestTransfers.size()) {
            return;
        }

        BigDecimal currentBalance = balances.get(currentIndex);

        for (int nextIndex = currentIndex + 1;
             nextIndex < balances.size();
             nextIndex++) {

            BigDecimal nextBalance = balances.get(nextIndex);

            // Only settle participants with opposite signs.
            if (currentBalance.signum() * nextBalance.signum() >= 0) {
                continue;
            }

            // Skip equivalent choices.
            boolean duplicateBalance = false;

            for (int previousIndex = currentIndex + 1;
                 previousIndex < nextIndex;
                 previousIndex++) {

                if (balances.get(previousIndex)
                        .compareTo(nextBalance) == 0) {

                    duplicateBalance = true;
                    break;
                }
            }

            if (duplicateBalance) {
                continue;
            }

            BigDecimal amount = currentBalance.abs()
                    .min(nextBalance.abs());

            Long fromParticipantId;
            Long toParticipantId;

            if (currentBalance.signum() < 0) {
                // Current participant owes money to next participant.
                fromParticipantId = participantIds.get(currentIndex);
                toParticipantId = participantIds.get(nextIndex);

                balances.set(
                        currentIndex,
                        currentBalance.add(amount)
                );

                balances.set(
                        nextIndex,
                        nextBalance.subtract(amount)
                );

            } else {
                // Next participant owes money to current participant.
                fromParticipantId = participantIds.get(nextIndex);
                toParticipantId = participantIds.get(currentIndex);

                balances.set(
                        currentIndex,
                        currentBalance.subtract(amount)
                );

                balances.set(
                        nextIndex,
                        nextBalance.add(amount)
                );
            }

            CalculatedTransfer transfer =
                    new CalculatedTransfer(
                            fromParticipantId,
                            toParticipantId,
                            amount
                    );

            // Choose.
            currentTransfers.add(transfer);

            // Explore.
            backtrack(
                    balances,
                    participantIds,
                    currentTransfers,
                    bestTransfers
            );

            // Unchoose.
            currentTransfers.remove(
                    currentTransfers.size() - 1
            );

            // Restore the original balances.
            balances.set(currentIndex, currentBalance);
            balances.set(nextIndex, nextBalance);

            // If current participant was completely settled,
            // no need to try other equivalent counterpart choices.
            if (currentBalance.abs()
                    .compareTo(nextBalance.abs()) == 0) {
                break;
            }
        }
    }
}
