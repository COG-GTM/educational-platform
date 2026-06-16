package com.educational.platform.users;

import com.educational.platform.users.login.SignInCommand;
import com.educational.platform.users.registration.UserRegistrationCommand;
import jakarta.persistence.Transient;
import jakarta.persistence.Version;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Structural coverage for the {@code @Version} optimistic-locking field added to {@link User}.
 *
 * <p>Complements the behavioural {@code jpa} tests: those prove the version increments and rejects
 * stale writes, while this fast, context-free test pins the source-level governance decision the PR
 * is built on - {@code @Version} is mapped on the {@code @Entity} (typed {@link Integer}, deliberately
 * decoupled from the {@code BIGINT} column), is never leaked onto a {@code *Command} request payload,
 * and never surfaces on the {@link UserDTO} read projection.
 */
class UserVersionMappingTest {

    private static final String USERS_PACKAGE = "com.educational.platform.users";

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
    void entityUser_versionField_isAPersistentInstanceField() throws NoSuchFieldException {
        // a static or @Transient field would still satisfy the "single Integer @Version field" check above
        // yet silently disable optimistic locking - Hibernate only manages a persistent instance field.
        // Pin that the mapping is genuinely active, not merely present.
        final Field version = User.class.getDeclaredField("version");

        assertThat(Modifier.isStatic(version.getModifiers()))
                .as("@Version must be an instance field, not static")
                .isFalse();
        assertThat(version.isAnnotationPresent(Transient.class))
                .as("@Version must be persistent, not @Transient")
                .isFalse();
    }

    @Test
    void entityUser_doesNotExposeVersionAccessorOrMutator() {
        // the PR's design rests on User having no field mutators - the version is owned by JPA and the
        // only domain write path is the constructor. Enforce that the optimistic-locking version can be
        // neither read nor set through the public API (no getVersion/setVersion leaks it onto callers).
        assertThat(User.class.getDeclaredMethods())
                .extracting(Method::getName)
                .as("User must not expose any accessor or mutator for the version field")
                .doesNotContain("getVersion", "setVersion", "isVersion", "version");
    }

    @Test
    void everyCommandClassInUsersModule_doesNotCarryVersion() throws ClassNotFoundException {
        // governance: @Version belongs to the aggregate's entity only - it must never appear on a
        // command, so a request payload can neither read nor set the optimistic-locking version.
        // Discover every *Command class in the module (rather than naming a fixed pair) so a command
        // added later is automatically held to the same rule.
        final List<Class<?>> commandClasses = commandClassesInUsersModule();

        // guard against a vacuous pass: the scan must actually see the known commands
        assertThat(commandClasses)
                .as("the users module exposes discoverable *Command classes")
                .contains(UserRegistrationCommand.class, SignInCommand.class);

        assertThat(commandClasses).allSatisfy(command -> {
            assertThat(command.getDeclaredFields())
                    .as("%s must not declare a @Version-annotated field", command.getSimpleName())
                    .noneMatch(field -> field.isAnnotationPresent(Version.class));
            assertThat(command.getDeclaredFields())
                    .extracting(Field::getName)
                    .as("%s must not declare a version field", command.getSimpleName())
                    .doesNotContain("version");
        });
    }

    @Test
    void userReadModel_doesNotExposeVersion() {
        // the optimistic-locking version is an internal persistence concern: it must stay off the read
        // projection so a UserDTO can neither carry nor leak the entity's @Version to API consumers
        assertThat(UserDTO.class.getDeclaredFields())
                .as("UserDTO must not declare a @Version-annotated field")
                .noneMatch(field -> field.isAnnotationPresent(Version.class));
        assertThat(UserDTO.class.getDeclaredFields())
                .extracting(Field::getName)
                .as("UserDTO must not expose a version field")
                .doesNotContain("version");
    }

    private static List<Class<?>> commandClassesInUsersModule() throws ClassNotFoundException {
        final ClassPathScanningCandidateComponentProvider scanner =
                new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter((metadataReader, metadataReaderFactory) -> true);

        final List<Class<?>> commandClasses = new ArrayList<>();
        for (final BeanDefinition definition : scanner.findCandidateComponents(USERS_PACKAGE)) {
            final Class<?> candidate = Class.forName(definition.getBeanClassName());
            if (candidate.getSimpleName().endsWith("Command")) {
                commandClasses.add(candidate);
            }
        }
        return commandClasses;
    }
}
