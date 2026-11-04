package com.dk.pokersettlement.repositories;

import com.dk.pokersettlement.models.BuyIn;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BuyInRepo extends JpaRepository<BuyIn,Long> {
    List<BuyIn> findByParticipant_Id(Long participantId);

    boolean existsByParticipant_Id(Long participantId);

    Optional<BuyIn> findByIdAndParticipant_Game_Id(
            Long buyInId,
            Long gameId
    );
    List<BuyIn> findByParticipantIdIn(List<Long> participantIds);
}
