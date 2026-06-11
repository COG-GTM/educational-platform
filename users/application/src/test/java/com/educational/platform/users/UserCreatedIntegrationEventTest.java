package com.educational.platform.users;

import com.educational.platform.users.integration.event.UserCreatedIntegrationEvent;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class UserCreatedIntegrationEventTest {

    @Test
    void userCreatedIntegrationEvent_exposesUsernameAndEmail() {
        // when
        final UserCreatedIntegrationEvent event = new UserCreatedIntegrationEvent("username", "email@test.com");

        // then
        assertThat(event.username()).isEqualTo("username");
        assertThat(event.email()).isEqualTo("email@test.com");
    }

    @Test
    void userCreatedIntegrationEvent_equalInstances() {
        assertThat(new UserCreatedIntegrationEvent("user", "email@test.com"))
                .isEqualTo(new UserCreatedIntegrationEvent("user", "email@test.com"));
    }

    @Test
    void userCreatedIntegrationEvent_differentUsername_notEqual() {
        assertThat(new UserCreatedIntegrationEvent("user1", "email@test.com"))
                .isNotEqualTo(new UserCreatedIntegrationEvent("user2", "email@test.com"));
    }

    @Test
    void userCreatedIntegrationEvent_differentEmail_notEqual() {
        assertThat(new UserCreatedIntegrationEvent("user", "email1@test.com"))
                .isNotEqualTo(new UserCreatedIntegrationEvent("user", "email2@test.com"));
    }
}
