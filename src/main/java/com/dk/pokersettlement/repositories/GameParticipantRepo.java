package com.dk.pokersettlement.repositories;

import com.dk.pokersettlement.models.GameParticipant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface GameParticipantRepo extends JpaRepository<GameParticipant,Long> {
    boolean existsByGame_IdAndUser_Id(Long gameId, Long userId);

    // GameParticipantRepo
    Optional<GameParticipant> findByIdAndGame_Id(
            Long participantId, Long gameId);

    List<GameParticipant> findByGame_Id(Long gameId);

    List<GameParticipant> findByUser_Id(Long userId);
}
