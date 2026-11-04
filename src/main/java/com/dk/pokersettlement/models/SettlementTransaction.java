package com.dk.pokersettlement.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Data
@Table(name = "settlement_transactions")
public class SettlementTransaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(optional = false)
    @JoinColumn(name = "game_id", nullable = false)
    private Game game;
    @ManyToOne(optional = false)
    @JoinColumn(name = "from_participant_id",nullable = false)
    private GameParticipant fromParticipant;
    @ManyToOne(optional = false)
    @JoinColumn(name = "to_participant_id",nullable = false)
    private GameParticipant toParticipant;
    @Column(nullable = false)
    private BigDecimal amount;
}
