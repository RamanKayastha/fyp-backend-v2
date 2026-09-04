package com.stitch.story.backend.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class SchemaPatchConfig {

    private final JdbcTemplate jdbcTemplate;

    @Bean
    ApplicationRunner widenEnumColumns() {
        return args -> {
            widen("`user`", "role");
            widen("activity_logs", "entity_type");
            widen("activity_logs", "action");
            widen("vendor_application", "status");
            addDoubleColumn("orders", "latitude");
            addDoubleColumn("orders", "longitude");
        };
    }

    private void addDoubleColumn(String table, String column) {
        try {
            jdbcTemplate.execute("ALTER TABLE " + table + " ADD COLUMN " + column + " DOUBLE NULL");
            log.info("Ensured {}.{} exists", table, column);
        } catch (Exception exception) {
            log.warn("Could not add {}.{}: {}", table, column, exception.getMessage());
        }
    }

    private void widen(String table, String column) {
        try {
            jdbcTemplate.execute("ALTER TABLE " + table + " MODIFY COLUMN " + column + " VARCHAR(32)");
            log.info("Ensured {}.{} is VARCHAR(32)", table, column);
        } catch (Exception exception) {
            log.warn("Could not widen {}.{}: {}", table, column, exception.getMessage());
        }
    }
}
