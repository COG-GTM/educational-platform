package com.educational.platform.users.integration.event;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class UserCreatedIntegrationEventTest {

    @Test
    void constructor_allFieldsSet_accessorsReturnCorrectValues() {
        // when
        final UserCreatedIntegrationEvent event = new UserCreatedIntegrationEvent("username", "email@example.com");

        // then
        assertThat(event.username()).isEqualTo("username");
        assertThat(event.email()).isEqualTo("email@example.com");
    }

    @Test
    void constructor_nullFields_accessorsReturnNull() {
        // when
        final UserCreatedIntegrationEvent event = new UserCreatedIntegrationEvent(null, null);

        // then
        assertThat(event.username()).isNull();
        assertThat(event.email()).isNull();
    }

    @Test
    void equality_sameValues_equal() {
        // given
        final UserCreatedIntegrationEvent event1 = new UserCreatedIntegrationEvent("user", "user@example.com");
        final UserCreatedIntegrationEvent event2 = new UserCreatedIntegrationEvent("user", "user@example.com");

        // then
        assertThat(event1).isEqualTo(event2);
    }

    @Test
    void equality_differentValues_notEqual() {
        // given
        final UserCreatedIntegrationEvent event1 = new UserCreatedIntegrationEvent("user1", "user1@example.com");
        final UserCreatedIntegrationEvent event2 = new UserCreatedIntegrationEvent("user2", "user2@example.com");

        // then
        assertThat(event1).isNotEqualTo(event2);
    }

    @Test
    void toString_containsFieldValues() {
        // given
        final UserCreatedIntegrationEvent event = new UserCreatedIntegrationEvent("testuser", "test@example.com");

        // when
        final String str = event.toString();

        // then
        assertThat(str).contains("testuser");
        assertThat(str).contains("test@example.com");
    }

    @Test
    void hashCode_sameValues_equal() {
        // given
        final UserCreatedIntegrationEvent event1 = new UserCreatedIntegrationEvent("user", "user@example.com");
        final UserCreatedIntegrationEvent event2 = new UserCreatedIntegrationEvent("user", "user@example.com");

        // then
        assertThat(event1.hashCode()).isEqualTo(event2.hashCode());
    }
}
