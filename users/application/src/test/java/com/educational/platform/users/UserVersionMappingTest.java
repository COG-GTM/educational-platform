package com.educational.platform.users;

import com.educational.platform.users.login.SignInCommand;
import com.educational.platform.users.registration.UserRegistrationCommand;
import jakarta.persistence.Version;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Structural coverage for the {@code @Version} optimistic-locking field added to {@link User}.
 *
 * <p>Complements the behavioural {@code jpa} tests: those prove the version increments and rejects
 * stale writes, while this fast, context-free test pins the source-level governance decision the PR
 * is built on - {@code @Version} is mapped on the {@code @Entity} (typed {@link Integer}, deliberately
 * decoupled from the {@code BIGINT} column) and is never leaked onto a {@code *Command} class.
 */
class UserVersionMappingTest {

    @Test
    void entityUser_declaresSingleIntegerVersionField() {
        // the entity exposes exactly one @Version field, named "version" and typed Integer
        // (the intentional decoupling from the BIGINT backing column)
        final List<Field> versionFields = List.of(User.class.getDeclaredFields()).stream()
                .filter(field -> field.isAnnotationPresent(Version.class))
                .toList();

        assertThat(versionFields).singleElement().satisfies(field -> {
            assertThat(field.getName()).isEqualTo("version");
            assertThat(field.getType()).isEqualTo(Integer.class);
        });
    }

    @Test
    void commandClasses_doNotCarryVersion() {
        // governance: @Version belongs to the aggregate's entity only - it must never appear on a
        // command DTO, so a request payload can neither read nor set the optimistic-locking version
        for (final Class<?> command : List.of(UserRegistrationCommand.class, SignInCommand.class)) {
            assertThat(command.getDeclaredFields())
                    .as("%s must not declare a @Version-annotated field", command.getSimpleName())
                    .noneMatch(field -> field.isAnnotationPresent(Version.class));
            assertThat(command.getDeclaredFields())
                    .extracting(Field::getName)
                    .as("%s must not declare a version field", command.getSimpleName())
                    .doesNotContain("version");
        }
    }
}
