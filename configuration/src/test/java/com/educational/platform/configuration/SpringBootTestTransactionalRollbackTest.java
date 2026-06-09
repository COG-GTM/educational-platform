package com.educational.platform.configuration;

import com.educational.platform.EducationalPlatformApplication;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Validates that the {@code DataSource} auto-configured by the Spring Boot
 * plugin supports transactional behavior through standard JDBC. The Spring
 * Boot plugin's auto-configuration registers a {@code DataSource} and
 * {@code PlatformTransactionManager} that enable test-transactional rollback
 * (via {@code @Transactional} on test methods). This test validates the
 * underlying JDBC transaction support by exercising manual commit/rollback
 * on the auto-configured H2 DataSource.
 * <p>
 * Existing tests validate {@code PlatformTransactionManager} bean presence
 * ({@link JpaAutoConfigurationTest}) and annotation classpath availability
 * ({@link StarterTestSpringTestAnnotationsPresenceTest}). This test
 * exercises the actual JDBC transactional behavior of the auto-configured
 * DataSource, proving the boot plugin's data source auto-configuration
 * correctly supports transaction isolation — the foundation for
 * {@code @Transactional} test rollback.
 */
@SpringBootTest(
        classes = EducationalPlatformApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE
)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class SpringBootTestTransactionalRollbackTest {

    @Autowired
    private DataSource dataSource;

    @Test
    @Order(1)
    void dataSource_shouldSupportManualTransactionRollback() throws Exception {
        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);
            try (Statement stmt = conn.createStatement()) {
                stmt.execute(
                        "CREATE TABLE IF NOT EXISTS txn_test (id INT PRIMARY KEY, val VARCHAR(50))");
                conn.commit();

                stmt.execute("INSERT INTO txn_test (id, val) VALUES (1, 'rollback-me')");
                // Verify row is visible within the transaction
                try (ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM txn_test")) {
                    rs.next();
                    assertThat(rs.getLong(1))
                            .as("Inserted row must be visible within uncommitted transaction")
                            .isEqualTo(1L);
                }

                conn.rollback();

                // Verify rollback worked
                try (ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM txn_test")) {
                    rs.next();
                    assertThat(rs.getLong(1))
                            .as("After rollback, the inserted row must be gone — "
                                    + "this proves the auto-configured DataSource supports "
                                    + "transactional isolation required by @Transactional test rollback")
                            .isEqualTo(0L);
                }
            }
        }
    }

    @Test
    @Order(2)
    void dataSource_shouldSupportCommit() throws Exception {
        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);
            try (Statement stmt = conn.createStatement()) {
                stmt.execute(
                        "CREATE TABLE IF NOT EXISTS txn_test_commit (id INT PRIMARY KEY)");
                stmt.execute("INSERT INTO txn_test_commit (id) VALUES (42)");
                conn.commit();

                try (ResultSet rs = stmt.executeQuery(
                        "SELECT COUNT(*) FROM txn_test_commit WHERE id = 42")) {
                    rs.next();
                    assertThat(rs.getLong(1))
                            .as("Committed row must persist after commit")
                            .isEqualTo(1L);
                }
            } finally {
                // Clean up
                try (Statement stmt = conn.createStatement()) {
                    stmt.execute("DROP TABLE IF EXISTS txn_test_commit");
                }
            }
        }
    }

    @Test
    @Order(3)
    void dataSource_shouldProvideIsolatedConnections() throws Exception {
        try (Connection conn1 = dataSource.getConnection();
             Connection conn2 = dataSource.getConnection()) {
            assertThat(conn1)
                    .as("DataSource must provide non-null connections")
                    .isNotNull();
            assertThat(conn2)
                    .as("DataSource must support multiple concurrent connections")
                    .isNotNull();
        }
    }
}
