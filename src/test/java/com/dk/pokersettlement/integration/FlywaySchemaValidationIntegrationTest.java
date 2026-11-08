package com.dk.pokersettlement.integration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.flywaydb.core.Flyway;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:flyway_test;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.flyway.enabled=true",
        "spring.flyway.baseline-on-migrate=true",
        "spring.flyway.baseline-version=0",
        "spring.flyway.locations=classpath:db/migration",
        "spring.jpa.hibernate.ddl-auto=validate"
})
class FlywaySchemaValidationIntegrationTest {

    @Autowired(required = false)
    private Flyway flyway;

    @Test
    @DisplayName("Flyway executes V1 migration and Hibernate validates schema successfully")
    void testFlywayMigrationAndHibernateValidation() {
        assertNotNull(flyway, "Flyway bean should be initialized by spring-boot-starter-flyway");
        var info = flyway.info().current();
        assertNotNull(info, "Flyway migration should have executed");
        assertTrue(info.getVersion().getVersion().equals("1"), "Current schema version should be 1");
    }
}
