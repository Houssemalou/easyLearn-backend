package com.free.easyLearn.config;

import org.flywaydb.core.Flyway;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;

@Configuration
public class FlywayConfig {

    @Bean
    public FlywayMigrationStrategy flywayMigrationStrategy(LocalContainerEntityManagerFactoryBean entityManagerFactory) {
        return flyway -> {
            // Assurez-vous que JPA a initialisé le schéma avant d'exécuter Flyway
            assert entityManagerFactory.getObject() != null;
            entityManagerFactory.getObject().createEntityManager().close();
            flyway.migrate();
        };
    }
}
