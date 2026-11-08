package com.dk.pokersettlement.integration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.flywaydb.core.Flyway;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Simulates Neon where tables already exist from previous ddl-auto=update,
 * but no flyway_schema_history table exists yet.
 */
@SpringBootTest
@ContextConfiguration(initializers = FlywayBaselineExistingDatabaseIntegrationTest.DatabasePrePopulator.class)
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:existing_db_test;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.flyway.enabled=true",
        "spring.flyway.baseline-on-migrate=true",
        "spring.flyway.baseline-version=0",
        "spring.flyway.locations=classpath:db/migration",
        "spring.jpa.hibernate.ddl-auto=validate"
})
class FlywayBaselineExistingDatabaseIntegrationTest {

    static class DatabasePrePopulator implements ApplicationContextInitializer<ConfigurableApplicationContext> {
        @Override
        public void initialize(ConfigurableApplicationContext applicationContext) {
            try (Connection conn = DriverManager.getConnection("jdbc:h2:mem:existing_db_test;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH;DB_CLOSE_DELAY=-1", "sa", "");
                 Statement stmt = conn.createStatement()) {
                // Pre-create tables as if created by Hibernate previously (non-empty schema without flyway_schema_history)
                stmt.execute("CREATE TABLE users (id BIGSERIAL PRIMARY KEY, username VARCHAR(255) NOT NULL UNIQUE, password VARCHAR(255) NOT NULL)");
                stmt.execute("CREATE TABLE games (id BIGSERIAL PRIMARY KEY, name VARCHAR(255), created_by_id BIGINT NOT NULL, game_status VARCHAR(50) NOT NULL, chip_conversion_rate NUMERIC(19, 4) NOT NULL, round_active BOOLEAN NOT NULL DEFAULT FALSE, version BIGINT NOT NULL DEFAULT 0)");
                stmt.execute("CREATE TABLE game_participants (id BIGSERIAL PRIMARY KEY, game_id BIGINT NOT NULL, user_id BIGINT NOT NULL, final_chip_value BIGINT)");
                stmt.execute("CREATE TABLE buy_ins (id BIGSERIAL PRIMARY KEY, participant_id BIGINT NOT NULL, chip_value BIGINT NOT NULL)");
                stmt.execute("CREATE TABLE settlement_transactions (id BIGSERIAL PRIMARY KEY, game_id BIGINT NOT NULL, from_participant_id BIGINT NOT NULL, to_participant_id BIGINT NOT NULL, amount NUMERIC(19, 4) NOT NULL)");
            } catch (Exception e) {
                throw new RuntimeException("Failed to pre-populate existing database", e);
            }
        }
    }

    @Autowired
    private Flyway flyway;

    @Test
    @DisplayName("Flyway baselines non-empty database and executes V1 migration with ddl-auto=validate")
    void testFlywayBaselinesNonEmptyDatabase() {
        assertNotNull(flyway, "Flyway bean should exist");
        var info = flyway.info().current();
        assertNotNull(info, "Flyway migration should have executed");
        assertTrue(info.getVersion().getVersion().equals("1"), "Current version should be 1");
    }
}
