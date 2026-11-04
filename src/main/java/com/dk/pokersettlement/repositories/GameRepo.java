package com.dk.pokersettlement.repositories;

import com.dk.pokersettlement.models.Game;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.List;

public interface GameRepo extends JpaRepository<Game, Long> {
    @Query("SELECT DISTINCT g FROM Game g LEFT JOIN GameParticipant p ON p.game = g " +
            "WHERE g.createdBy.id = :userId OR p.user.id = :userId")
    List<Game> findAccessibleByUserId(@Param("userId") Long userId);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT g FROM Game g WHERE g.id = :gameId")
    Optional<Game> findByIdForUpdate(
            @Param("gameId") Long gameId
    );
}
