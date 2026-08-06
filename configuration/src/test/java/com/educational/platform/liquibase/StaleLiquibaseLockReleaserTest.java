package com.educational.platform.liquibase;

import liquibase.integration.spring.SpringLiquibase;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Duration;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StaleLiquibaseLockReleaserTest {

    private JdbcDataSource dataSource;
    private Connection keepAlive;

    @BeforeEach
    void setUp() throws SQLException {
        dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1");
        dataSource.setUser("sa");
        keepAlive = dataSource.getConnection();
    }

    @AfterEach
    void tearDown() throws SQLException {
        try (Statement statement = keepAlive.createStatement()) {
            statement.execute("SHUTDOWN");
        } catch (SQLException ignored) {
            // database already shut down
        }
        keepAlive.close();
    }

    @Test
    void staleLock_isReleased() throws SQLException {
        createLockTable();
        insertLock(true, Duration.ofMinutes(40));

        process(new StaleLiquibaseLockReleaser(Duration.ofMinutes(5)));

        assertFalse(isLocked());
    }

    @Test
    void recentLock_isKept() throws SQLException {
        createLockTable();
        insertLock(true, Duration.ofSeconds(30));

        process(new StaleLiquibaseLockReleaser(Duration.ofMinutes(5)));

        assertTrue(isLocked());
    }

    @Test
    void unlockedRow_isLeftUntouched() throws SQLException {
        createLockTable();
        insertLock(false, null);

        process(new StaleLiquibaseLockReleaser(Duration.ofMinutes(5)));

        assertFalse(isLocked());
    }

    @Test
    void missingLockTable_isIgnored() {
        process(new StaleLiquibaseLockReleaser(Duration.ofMinutes(5)));
    }

    @Test
    void lockWithoutGrantTimestamp_isReleased() throws SQLException {
        createLockTable();
        insertLock(true, null);

        process(new StaleLiquibaseLockReleaser(Duration.ofMinutes(5)));

        assertFalse(isLocked());
    }

    @Test
    void otherBeans_arePassedThrough() {
        StaleLiquibaseLockReleaser releaser = new StaleLiquibaseLockReleaser(Duration.ofMinutes(5));
        Object bean = new Object();

        assertSame(bean, releaser.postProcessBeforeInitialization(bean, "someBean"));
    }

    @Test
    void liquibaseWithoutDataSource_isIgnored() {
        StaleLiquibaseLockReleaser releaser = new StaleLiquibaseLockReleaser(Duration.ofMinutes(5));
        SpringLiquibase liquibase = new SpringLiquibase();

        assertNull(liquibase.getDataSource());
        assertSame(liquibase, releaser.postProcessBeforeInitialization(liquibase, "liquibase"));
    }

    private void process(StaleLiquibaseLockReleaser releaser) {
        SpringLiquibase liquibase = new SpringLiquibase();
        liquibase.setDataSource(dataSource);
        releaser.postProcessBeforeInitialization(liquibase, "liquibase");
    }

    private void createLockTable() throws SQLException {
        execute("CREATE TABLE DATABASECHANGELOGLOCK (ID INT NOT NULL PRIMARY KEY, LOCKED BOOLEAN NOT NULL, LOCKGRANTED TIMESTAMP, LOCKEDBY VARCHAR(255))");
    }

    private void insertLock(boolean locked, Duration age) throws SQLException {
        String lockGranted = age == null
                ? "NULL"
                : "TIMESTAMPADD(SECOND, -" + age.toSeconds() + ", CURRENT_TIMESTAMP)";
        execute("INSERT INTO DATABASECHANGELOGLOCK (ID, LOCKED, LOCKGRANTED, LOCKEDBY) VALUES (1, " + locked + ", " + lockGranted + ", 'crashed-pod')");
    }

    private boolean isLocked() throws SQLException {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("SELECT LOCKED FROM DATABASECHANGELOGLOCK WHERE ID = 1")) {
            assertTrue(resultSet.next());
            return resultSet.getBoolean("LOCKED");
        }
    }

    private void execute(String sql) throws SQLException {
        try (Statement statement = keepAlive.createStatement()) {
            statement.execute(sql);
        }
    }
}
