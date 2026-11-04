package com.dk.pokersettlement.component;

import com.dk.pokersettlement.components.MinimumTransactionsCalculator;
import com.dk.pokersettlement.dtos.CalculatedTransfer;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class MinimumTransactionsCalculatorTest {

    private final MinimumTransactionsCalculator calculator =
            new MinimumTransactionsCalculator();

    @Test
    void shouldSettleOneDebtorAndOneCreditor() {

        List<BigDecimal> balances = List.of(
                new BigDecimal("-10"),
                new BigDecimal("10")
        );

        List<Long> participantIds = List.of(101L, 102L);

        List<CalculatedTransfer> transfers =
                calculator.calculate(balances, participantIds);

        assertEquals(1, transfers.size());

        CalculatedTransfer transfer = transfers.get(0);

        assertEquals(101L, transfer.fromParticipantId());
        assertEquals(102L, transfer.toParticipantId());
        assertEquals(0, transfer.amount().compareTo(
                new BigDecimal("10")));
    }

    @Test
    void shouldSettleOneDebtorAndMultipleCreditors() {

        List<BigDecimal> balances = List.of(
                new BigDecimal("-10"),
                new BigDecimal("6"),
                new BigDecimal("4")
        );

        List<Long> participantIds = List.of(101L, 102L, 103L);

        List<CalculatedTransfer> transfers =
                calculator.calculate(balances, participantIds);

        assertEquals(2, transfers.size());

        assertValidSettlement(balances, participantIds, transfers);
    }

    @Test
    void shouldFindMinimumTransactionsForMultipleDebtors() {

        List<BigDecimal> balances = List.of(
                new BigDecimal("-5"),
                new BigDecimal("-5"),
                new BigDecimal("6"),
                new BigDecimal("4")
        );

        List<Long> participantIds = List.of(
                101L, 102L, 103L, 104L
        );

        List<CalculatedTransfer> transfers =
                calculator.calculate(balances, participantIds);

        assertEquals(3, transfers.size());

        assertValidSettlement(balances, participantIds, transfers);
    }

    private void assertValidSettlement(
            List<BigDecimal> originalBalances,
            List<Long> participantIds,
            List<CalculatedTransfer> transfers) {

        Map<Long, BigDecimal> resultingBalances =
                new HashMap<>();

        for (int i = 0; i < participantIds.size(); i++) {
            resultingBalances.put(
                    participantIds.get(i),
                    originalBalances.get(i)
            );
        }

        for (CalculatedTransfer transfer : transfers) {

            assertTrue(transfer.amount().signum() > 0);

            assertNotEquals(
                    transfer.fromParticipantId(),
                    transfer.toParticipantId()
            );

            resultingBalances.compute(
                    transfer.fromParticipantId(),
                    (id, balance) -> balance.add(transfer.amount())
            );

            resultingBalances.compute(
                    transfer.toParticipantId(),
                    (id, balance) -> balance.subtract(transfer.amount())
            );
        }

        for (BigDecimal balance : resultingBalances.values()) {
            assertEquals(0, balance.compareTo(BigDecimal.ZERO));
        }
    }
}
