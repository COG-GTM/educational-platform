package com.educational.platform.users.jpa;

import com.educational.platform.users.RoleDTO;
import com.educational.platform.users.User;
import com.educational.platform.users.UserRepository;
import com.educational.platform.users.registration.UserRegistrationCommand;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.OptimisticLockException;
import jakarta.persistence.PersistenceContext;
import liquibase.Contexts;
import liquibase.LabelExpression;
import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * End-to-end coverage of the {@code @Version} optimistic locking on {@link User} running against the
 * <em>production schema configuration</em>: the {@code custom_user} table is created by the real Liquibase
 * changelog ({@code db/users.yml}, whose {@code add-version-column-to-custom_user} changeSet adds
 * {@code version BIGINT NOT NULL}) and Hibernate runs with {@code ddl-auto=none}, so the entity is mapped onto
 * the migration-owned schema exactly as in production.
 *
 * <p>This closes the one seam the rest of the suite leaves open:
 * <ul>
 *   <li>{@link UserOptimisticLockingTest} proves the locking <em>behaviour</em>, but against a
 *   <strong>Hibernate-generated</strong> schema (the default {@code @DataJpaTest} {@code ddl-auto=create-drop}),
 *   where the version column is an {@code INTEGER} derived from the entity field - never the migration's
 *   {@code BIGINT} column.</li>
 *   <li>{@link com.educational.platform.users.db.UserVersionColumnMigrationTest} proves the migration produces a
 *   {@code BIGINT NOT NULL} column, but with <strong>no entity</strong> in play (raw JDBC).</li>
 *   <li>{@link com.educational.platform.users.UserVersionMappingTest} pins the {@code @Version -> "version"}
 *   column mapping by <strong>reflection</strong>, never at runtime.</li>
 * </ul>
 * None of them prove the Integer-typed {@code @Version} field actually initialises, increments and detects
 * stale writes when bound to the genuinely migrated {@code BIGINT} column under {@code ddl-auto=none} - which is
 * the only configuration that ships. A renamed/retyped column, or the entity drifting from the migration, would
 * pass every test above yet break here, where optimistic locking is exercised on the real production schema.
 */
@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=none")
class UserOptimisticLockingOnMigratedSchemaTest {

    private static final String USERNAME = "username";

    @Autowired
    private UserRepository repository;

    @Autowired
    private DataSource dataSource;

    @PersistenceContext
    private EntityManager entityManager;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @BeforeEach
    void applyProductionMigration() throws Exception {
        // build the schema the way production does - via the Liquibase changelog, not Hibernate. With
        // ddl-auto=none Hibernate never touches the schema, so without this the table would not exist.
        // Re-running per test is idempotent: createTable is MARK_RAN (table already present) and the version
        // changeSet is recorded once, so subsequent tests reuse the same migrated schema.
        try (Connection connection = dataSource.getConnection()) {
            final Database database = DatabaseFactory.getInstance()
                    .findCorrectDatabaseImplementation(new JdbcConnection(connection));
            try (Liquibase liquibase = new Liquibase("db/users.yml", new ClassLoaderResourceAccessor(), database)) {
                liquibase.update(new Contexts(), new LabelExpression());
            }
        }
    }

    @Test
    void schemaUnderTest_isTheMigratedBigIntColumn_notAHibernateGeneratedOne() throws SQLException {
        // precondition that gives every other test here its meaning: the JPA entity is running against the
        // migration-owned BIGINT column (ddl-auto=none + Liquibase), not the INTEGER column Hibernate would
        // derive from the Integer field had it generated the schema. This is the runtime counterpart to the
        // entity-less migration test and the reflection-only column-mapping test.
        try (Connection connection = dataSource.getConnection();
             ResultSet columns = connection.getMetaData().getColumns(null, null, "CUSTOM_USER", "VERSION")) {
            assertThat(columns.next()).as("version column exists on the migrated custom_user").isTrue();
            assertThat(columns.getInt("DATA_TYPE")).as("version is the migration's BIGINT, not a generated INTEGER")
                    .isEqualTo(Types.BIGINT);
            assertThat(columns.getString("IS_NULLABLE")).isEqualTo("NO");
        }
    }

    @Test
    void persist_onMigratedSchema_initializesVersionToZero() {
        // when a registered aggregate is persisted onto the migrated BIGINT column
        final User saved = repository.saveAndFlush(newUser());

        // then the Integer @Version maps cleanly onto the BIGINT column and JPA initialises it to 0 - the
        // entity<->migration agreement holds at runtime, not just by reflection
        assertThat(saved).hasFieldOrPropertyWithValue("version", 0);
        assertThat(((Number) versionOf(USERNAME)).longValue()).isZero();
    }

    @Test
    void write_onMigratedSchema_incrementsVersionAndReadsBackAsInteger() {
        // given a user persisted on the migrated schema
        repository.saveAndFlush(newUser());
        entityManager.clear();
        final User loaded = repository.findByUsername(USERNAME).orElseThrow();

        // when the row is written (forced increment exercises the @Version column)
        entityManager.lock(loaded, LockModeType.PESSIMISTIC_FORCE_INCREMENT);
        repository.flush();

        // then the version advances on the BIGINT column and materialises back onto the Integer-typed field -
        // proving the intentional Integer<->BIGINT decoupling round-trips on the production schema
        assertThat(loaded).hasFieldOrPropertyWithValue("version", 1);
        assertThat(loaded).extracting("version").isInstanceOf(Integer.class);
        assertThat(((Number) versionOf(USERNAME)).longValue()).isEqualTo(1L);
    }

    @Test
    void staleWrite_onMigratedSchema_throwsOptimisticLockException() {
        // given a user loaded at version 0 from the migrated schema
        repository.saveAndFlush(newUser());
        entityManager.clear();
        final User stale = repository.findByUsername(USERNAME).orElseThrow();

        // and a concurrent transaction that advanced the row's version
        entityManager.createNativeQuery("UPDATE custom_user SET version = version + 1 WHERE username = :username")
                .setParameter("username", USERNAME)
                .executeUpdate();

        // when the stale instance is written back through the raw EntityManager path
        // then optimistic locking fires on the BIGINT column just as it does on the Hibernate-generated one
        assertThatExceptionOfType(OptimisticLockException.class)
                .isThrownBy(() -> {
                    entityManager.lock(stale, LockModeType.PESSIMISTIC_FORCE_INCREMENT);
                    repository.flush();
                });
    }

    @Test
    void staleMerge_onMigratedSchema_throwsObjectOptimisticLockingFailureException() {
        // given a user detached at version 0 (the state a request holds between read and write)
        repository.saveAndFlush(newUser());
        entityManager.clear();
        final User stale = repository.findByUsername(USERNAME).orElseThrow();
        entityManager.detach(stale);

        // and a concurrent transaction that advanced the row's version
        entityManager.createNativeQuery("UPDATE custom_user SET version = version + 1 WHERE username = :username")
                .setParameter("username", USERNAME)
                .executeUpdate();

        // when the stale instance is written back through the Spring Data merge path against the migrated schema
        // then the conflict surfaces as Spring's translated exception, not a silent overwrite - the other
        // production write path (repository.save) is guarded on the real BIGINT column too
        assertThatExceptionOfType(ObjectOptimisticLockingFailureException.class)
                .isThrownBy(() -> repository.saveAndFlush(stale));
    }

    private Object versionOf(final String username) {
        return entityManager
                .createNativeQuery("SELECT version FROM custom_user WHERE username = :username")
                .setParameter("username", username)
                .getSingleResult();
    }

    private User newUser() {
        final UserRegistrationCommand command = UserRegistrationCommand.builder()
                .username(USERNAME)
                .email("email@gmail.com")
                .password("password")
                .role(RoleDTO.ROLE_STUDENT)
                .build();
        return new User(command, passwordEncoder);
    }
}
