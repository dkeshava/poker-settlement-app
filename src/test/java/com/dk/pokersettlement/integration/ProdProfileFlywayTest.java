package com.dk.pokersettlement.integration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.flywaydb.core.Flyway;
import org.springframework.boot.flyway.autoconfigure.FlywayProperties;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(properties = {
    "SPRING_DATASOURCE_URL=jdbc:h2:mem:prod_test;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH;DB_CLOSE_DELAY=-1",
    "SPRING_DATASOURCE_USERNAME=sa",
    "SPRING_DATASOURCE_PASSWORD=",
    "JWT_SECRET=ThisIsAVeryLongProductionSecretKeyForTestingOnly12345!"
})
@ActiveProfiles("prod")
class ProdProfileFlywayTest {

    @Autowired
    private FlywayProperties flywayProperties;

    @Autowired
    private Flyway flyway;

    @Test
    @DisplayName("Verify FlywayProperties in prod profile")
    void testProdProfileFlywayProperties() {
        assertTrue(flywayProperties.isEnabled(), "Flyway should be enabled in prod");
        assertTrue(flywayProperties.isBaselineOnMigrate(), "baseline-on-migrate should be true in prod");
        assertTrue("0".equals(flywayProperties.getBaselineVersion()), "baseline-version should be 0 in prod");
        assertNotNull(flyway, "Flyway bean should exist in prod");
    }
}
