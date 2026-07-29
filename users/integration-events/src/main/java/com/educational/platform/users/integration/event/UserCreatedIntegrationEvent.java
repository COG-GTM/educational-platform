package com.educational.platform.users.integration.event;

import java.util.UUID;

/**
 * Represents user created integration event, should be published after user creation.
 * The uuid is the stable cross-module identifier for the user; username and email are kept for display/login purposes.
 */
public record UserCreatedIntegrationEvent(UUID uuid, String username, String email) {

}
