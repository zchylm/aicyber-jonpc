package com.aicyber.backend.config;

import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

@Configuration
public class DatabaseMigrationConfig {

    @Bean
    Flyway flyway(
            DataSource dataSource,
            @Value("${spring.flyway.enabled:true}") boolean enabled,
            @Value("${spring.flyway.locations:classpath:db/migration}") String locations
    ) {
        Flyway flyway = Flyway.configure()
                .dataSource(dataSource)
                .locations(locations)
                .load();
        if (enabled) {
            flyway.migrate();
        }
        return flyway;
    }
}
