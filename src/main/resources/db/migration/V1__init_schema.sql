-- ===================================================================
-- Poker Settlement Management System - Baseline Schema
-- V1__init_schema.sql
-- ===================================================================

-- 1. Users table
CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL
);

-- 2. Games table
CREATE TABLE IF NOT EXISTS games (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255),
    created_by_id BIGINT NOT NULL,
    game_status VARCHAR(50) NOT NULL,
    chip_conversion_rate NUMERIC(19, 4) NOT NULL,
    round_active BOOLEAN NOT NULL DEFAULT FALSE,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_games_created_by FOREIGN KEY (created_by_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_games_created_by ON games(created_by_id);
CREATE INDEX IF NOT EXISTS idx_games_status ON games(game_status);

-- 3. Game Participants table
CREATE TABLE IF NOT EXISTS game_participants (
    id BIGSERIAL PRIMARY KEY,
    game_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    final_chip_value BIGINT,
    CONSTRAINT fk_participants_game FOREIGN KEY (game_id) REFERENCES games(id) ON DELETE CASCADE,
    CONSTRAINT fk_participants_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT uq_game_participant UNIQUE (game_id, user_id)
);

CREATE INDEX IF NOT EXISTS idx_participants_game ON game_participants(game_id);
CREATE INDEX IF NOT EXISTS idx_participants_user ON game_participants(user_id);

-- 4. Buy-ins table
CREATE TABLE IF NOT EXISTS buy_ins (
    id BIGSERIAL PRIMARY KEY,
    participant_id BIGINT NOT NULL,
    chip_value BIGINT NOT NULL,
    CONSTRAINT fk_buy_ins_participant FOREIGN KEY (participant_id) REFERENCES game_participants(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_buy_ins_participant ON buy_ins(participant_id);

-- 5. Settlement Transactions table
CREATE TABLE IF NOT EXISTS settlement_transactions (
    id BIGSERIAL PRIMARY KEY,
    game_id BIGINT NOT NULL,
    from_participant_id BIGINT NOT NULL,
    to_participant_id BIGINT NOT NULL,
    amount NUMERIC(19, 4) NOT NULL,
    CONSTRAINT fk_settlement_game FOREIGN KEY (game_id) REFERENCES games(id) ON DELETE CASCADE,
    CONSTRAINT fk_settlement_from FOREIGN KEY (from_participant_id) REFERENCES game_participants(id) ON DELETE CASCADE,
    CONSTRAINT fk_settlement_to FOREIGN KEY (to_participant_id) REFERENCES game_participants(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_settlement_game ON settlement_transactions(game_id);
