package com.educational.platform.users.integration.event;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UserCreatedIntegrationEventTest {

    @Test
    void constructor_validArguments_eventCreated() {
        // given
        final String username = "john_doe";
        final String email = "john@example.com";

        // when
        final UserCreatedIntegrationEvent sut = new UserCreatedIntegrationEvent(username, email);

        // then
        assertThat(sut.username()).isEqualTo("john_doe");
        assertThat(sut.email()).isEqualTo("john@example.com");
    }

    @Test
    void equality_sameValues_equal() {
        // given / when
        final UserCreatedIntegrationEvent event1 = new UserCreatedIntegrationEvent("user", "user@example.com");
        final UserCreatedIntegrationEvent event2 = new UserCreatedIntegrationEvent("user", "user@example.com");

        // then
        assertThat(event1).isEqualTo(event2);
        assertThat(event1.hashCode()).isEqualTo(event2.hashCode());
    }

    @Test
    void equality_differentUsername_notEqual() {
        // given / when
        final UserCreatedIntegrationEvent event1 = new UserCreatedIntegrationEvent("user1", "user@example.com");
        final UserCreatedIntegrationEvent event2 = new UserCreatedIntegrationEvent("user2", "user@example.com");

        // then
        assertThat(event1).isNotEqualTo(event2);
    }

    @Test
    void equality_differentEmail_notEqual() {
        // given / when
        final UserCreatedIntegrationEvent event1 = new UserCreatedIntegrationEvent("user", "a@example.com");
        final UserCreatedIntegrationEvent event2 = new UserCreatedIntegrationEvent("user", "b@example.com");

        // then
        assertThat(event1).isNotEqualTo(event2);
    }
}
