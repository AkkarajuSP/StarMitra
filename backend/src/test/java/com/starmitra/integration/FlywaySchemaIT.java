package com.starmitra.integration;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Flyway authority check — runs the real migration chain against the test
 * database and verifies the physical-schema invariants (90 tables, deferred
 * absent). Requires a reachable PostgreSQL (testcontainers in CI or local).
 */
@SpringBootTest
@ActiveProfiles("test")
class FlywaySchemaIT {

    @Autowired JdbcTemplate jdbc;

    @Test
    void migrationsProduceNinetyTableSchema() {
        Integer tables = jdbc.queryForObject(
                "select count(*) from pg_tables where schemaname='public' and tablename <> 'flyway_schema_history'",
                Integer.class);
        assertEquals(90, tables);
    }

    @Test
    void deferredTablesAbsent() {
        Integer present = jdbc.queryForObject(
                "select count(*) from pg_tables where schemaname='public' and tablename in " +
                "('password_credentials','moderation_appeals','assignment_scope')",
                Integer.class);
        assertEquals(0, present);
    }

    @Test
    void constraintCountsMatchPhysicalDesign() {
        // exclude flyway_schema_history bookkeeping table
        String excl = " and conrelid <> 'flyway_schema_history'::regclass";
        assertEquals(90, jdbc.queryForObject(
                "select count(*) from pg_constraint where contype='p' and connamespace='public'::regnamespace" + excl,
                Integer.class));
        assertTrue(jdbc.queryForObject(
                "select count(*) from pg_constraint where contype='f' and connamespace='public'::regnamespace" + excl,
                Integer.class) >= 70);
    }

    @Test
    void participantXorCheckAndSeedsExist() {
        Integer roles = jdbc.queryForObject("select count(*) from system_roles", Integer.class);
        assertEquals(4, roles);
        // XOR check present on competition_participants
        Integer ck = jdbc.queryForObject(
                "select count(*) from pg_constraint where conname='ck_cp_xor'", Integer.class);
        assertEquals(1, ck);
    }
}
