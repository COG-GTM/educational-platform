package com.educational.platform.liquibase;

import liquibase.integration.spring.SpringLiquibase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.BeanPostProcessor;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;

/**
 * Releases a stale Liquibase changelog lock before {@link SpringLiquibase} runs.
 *
 * <p>If an instance is killed mid-migration (e.g. OOM-killed during a rollout), the
 * {@code DATABASECHANGELOGLOCK} row stays locked and every new instance fails to start
 * with "Could not acquire change log lock". This post-processor inspects the lock row
 * and clears it when it has been held longer than a configurable timeout, on the
 * assumption that the original holder is no longer alive.
 */
public class StaleLiquibaseLockReleaser implements BeanPostProcessor {

    private static final Logger log = LoggerFactory.getLogger(StaleLiquibaseLockReleaser.class);

    private final Duration staleLockTimeout;

    public StaleLiquibaseLockReleaser(Duration staleLockTimeout) {
        this.staleLockTimeout = staleLockTimeout;
    }

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) {
        if (bean instanceof SpringLiquibase liquibase) {
            releaseStaleLock(liquibase.getDataSource());
        }
        return bean;
    }

    private void releaseStaleLock(DataSource dataSource) {
        if (dataSource == null) {
            return;
        }
        try (Connection connection = dataSource.getConnection()) {
            String lockTable = findLockTable(connection);
            if (lockTable == null) {
                return;
            }
            try (PreparedStatement select = connection.prepareStatement(
                    "SELECT LOCKED, LOCKGRANTED, LOCKEDBY FROM " + lockTable + " WHERE ID = 1")) {
                try (ResultSet resultSet = select.executeQuery()) {
                    if (!resultSet.next() || !resultSet.getBoolean("LOCKED")) {
                        return;
                    }
                    Timestamp lockGranted = resultSet.getTimestamp("LOCKGRANTED");
                    String lockedBy = resultSet.getString("LOCKEDBY");
                    if (lockGranted != null && lockGranted.toInstant().isAfter(Instant.now().minus(staleLockTimeout))) {
                        log.info("Liquibase changelog lock held by '{}' since {} is within the stale lock timeout of {}; leaving it in place",
                                lockedBy, lockGranted, staleLockTimeout);
                        return;
                    }
                    log.warn("Releasing stale Liquibase changelog lock held by '{}' since {} (stale lock timeout: {})",
                            lockedBy, lockGranted, staleLockTimeout);
                }
            }
            try (Statement update = connection.createStatement()) {
                update.executeUpdate(
                        "UPDATE " + lockTable + " SET LOCKED = FALSE, LOCKGRANTED = NULL, LOCKEDBY = NULL WHERE ID = 1");
            }
        } catch (SQLException e) {
            log.warn("Failed to check for a stale Liquibase changelog lock; Liquibase will attempt to acquire the lock as usual", e);
        }
    }

    private String findLockTable(Connection connection) throws SQLException {
        for (String candidate : new String[]{"DATABASECHANGELOGLOCK", "databasechangeloglock"}) {
            try (ResultSet tables = connection.getMetaData().getTables(null, null, candidate, new String[]{"TABLE"})) {
                if (tables.next()) {
                    return candidate;
                }
            }
        }
        return null;
    }
}
