package com.dk.pokersettlement.models;

import com.dk.pokersettlement.enums.GameState;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Data
@Table(name = "games")
public class Game {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;


    @ManyToOne(optional = false)
    @JoinColumn(name = "created_by_id", nullable = false)
    private User createdBy;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private GameState gameStatus;
    @Column(nullable = false)
    private BigDecimal chipConversionRate;
    @Column(nullable = false)
    private boolean roundActive;
}
