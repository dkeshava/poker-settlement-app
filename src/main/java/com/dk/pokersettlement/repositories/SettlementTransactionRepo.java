package com.dk.pokersettlement.repositories;

import com.dk.pokersettlement.models.SettlementTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SettlementTransactionRepo extends JpaRepository<SettlementTransaction,Long> {
    List<SettlementTransaction> findByGameId(Long gameId);
}
