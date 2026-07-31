package com.educational.platform.users.integration.event;

import java.util.UUID;

/**
 * Represents user created integration event, should be published after user creation.
 * The {@code uuid} is the stable cross-module identifier for the user; {@code username}
 * and {@code email} are carried for display/login purposes only.
 */
public record UserCreatedIntegrationEvent(UUID uuid, String username, String email) {

}
