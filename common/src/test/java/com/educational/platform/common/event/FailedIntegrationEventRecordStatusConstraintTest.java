package com.educational.platform.common.event;

import jakarta.persistence.Column;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that the {@link FailedIntegrationEventRecord.Status} enum values
 * are compatible with the Liquibase schema constraint {@code VARCHAR(20)} on
 * the {@code status} column.
 * <p>
 * Since the entity uses {@code @Enumerated(EnumType.STRING)}, Hibernate persists
 * the enum's {@code name()} as a string. If any enum constant name exceeds 20
 * characters, runtime persistence will fail with a DB truncation error that is
 * invisible at compile time.
 */
class FailedIntegrationEventRecordStatusConstraintTest {

    private static final int LIQUIBASE_STATUS_COLUMN_MAX_LENGTH = 20;

    @Test
    void allStatusEnumNames_fitWithinDatabaseColumnWidth() {
        for (FailedIntegrationEventRecord.Status status : FailedIntegrationEventRecord.Status.values()) {
            assertThat(status.name().length())
                    .as("Status '%s' name length (%d) must be <= %d to fit VARCHAR(%d) in Liquibase schema",
                            status.name(), status.name().length(),
                            LIQUIBASE_STATUS_COLUMN_MAX_LENGTH, LIQUIBASE_STATUS_COLUMN_MAX_LENGTH)
                    .isLessThanOrEqualTo(LIQUIBASE_STATUS_COLUMN_MAX_LENGTH);
        }
    }

    @Test
    void statusColumn_defaultJpaLengthCoversAllEnumValues() throws NoSuchFieldException {
        Field statusField = FailedIntegrationEventRecord.class.getDeclaredField("status");
        Column column = statusField.getAnnotation(Column.class);
        assertThat(column).isNotNull();

        // JPA default @Column.length is 255 if not specified — but the actual Liquibase column is VARCHAR(20).
        // Verify the longest enum name still fits within the DB constraint.
        int longestEnumName = 0;
        for (FailedIntegrationEventRecord.Status status : FailedIntegrationEventRecord.Status.values()) {
            longestEnumName = Math.max(longestEnumName, status.name().length());
        }
        assertThat(longestEnumName)
                .as("Longest Status enum name must fit within VARCHAR(%d)", LIQUIBASE_STATUS_COLUMN_MAX_LENGTH)
                .isLessThanOrEqualTo(LIQUIBASE_STATUS_COLUMN_MAX_LENGTH);
    }

    @Test
    void statusEnum_failedNameLength_isWithinLimit() {
        assertThat(FailedIntegrationEventRecord.Status.FAILED.name())
                .hasSize(6); // "FAILED" = 6 chars, well within 20
    }

    @Test
    void statusEnum_resolvedNameLength_isWithinLimit() {
        assertThat(FailedIntegrationEventRecord.Status.RESOLVED.name())
                .hasSize(8); // "RESOLVED" = 8 chars, well within 20
    }

    @Test
    void statusEnum_valuesAreAllUpperCase() {
        for (FailedIntegrationEventRecord.Status status : FailedIntegrationEventRecord.Status.values()) {
            assertThat(status.name())
                    .as("Status '%s' should be UPPER_CASE convention for DB consistency", status.name())
                    .isEqualTo(status.name().toUpperCase());
        }
    }

    @Test
    void statusEnum_hasExactlyTwoValues() {
        assertThat(FailedIntegrationEventRecord.Status.values())
                .as("Only FAILED and RESOLVED should exist — adding new values requires "
                        + "verifying they fit within VARCHAR(%d)", LIQUIBASE_STATUS_COLUMN_MAX_LENGTH)
                .hasSize(2);
    }

    @Test
    void statusEnum_ordinals_areStable() {
        // Ordinals should not be used for persistence (we use EnumType.STRING),
        // but documenting them prevents accidental reordering from breaking switch/case code.
        assertThat(FailedIntegrationEventRecord.Status.FAILED.ordinal()).isZero();
        assertThat(FailedIntegrationEventRecord.Status.RESOLVED.ordinal()).isEqualTo(1);
    }

    @Test
    void statusEnum_valueOfRoundTrip() {
        for (FailedIntegrationEventRecord.Status status : FailedIntegrationEventRecord.Status.values()) {
            assertThat(FailedIntegrationEventRecord.Status.valueOf(status.name()))
                    .as("valueOf(name()) must round-trip for DB serialization/deserialization")
                    .isSameAs(status);
        }
    }

    @Test
    void statusEnum_namesDoNotContainSpaces() {
        for (FailedIntegrationEventRecord.Status status : FailedIntegrationEventRecord.Status.values()) {
            assertThat(status.name())
                    .as("Status '%s' must not contain spaces for clean DB storage", status.name())
                    .doesNotContain(" ");
        }
    }

    @Test
    void statusEnum_namesAreNonEmpty() {
        for (FailedIntegrationEventRecord.Status status : FailedIntegrationEventRecord.Status.values()) {
            assertThat(status.name())
                    .as("Status enum constants must have non-empty names")
                    .isNotEmpty();
        }
    }
}
