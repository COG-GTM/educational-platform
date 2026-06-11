package com.educational.platform.users;

import com.educational.platform.users.integration.event.UserCreatedIntegrationEvent;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class UserIntegrationEventsTest {

    @Test
    void userCreatedIntegrationEvent_exposesUsernameAndEmail() {
        // when
        final UserCreatedIntegrationEvent event = new UserCreatedIntegrationEvent("username", "user@example.com");

        // then
        assertThat(event.username()).isEqualTo("username");
        assertThat(event.email()).isEqualTo("user@example.com");
    }

    @Test
    void userCreatedIntegrationEvent_equalInstances() {
        assertThat(new UserCreatedIntegrationEvent("username", "email@test.com"))
                .isEqualTo(new UserCreatedIntegrationEvent("username", "email@test.com"));
    }

    @Test
    void userCreatedIntegrationEvent_differentValues_notEqual() {
        assertThat(new UserCreatedIntegrationEvent("user1", "email@test.com"))
                .isNotEqualTo(new UserCreatedIntegrationEvent("user2", "email@test.com"));
    }
}
