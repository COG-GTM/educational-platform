package com.educational.platform.courses.bridge;

import java.util.Optional;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Minimal JSON (de)serialisation for the flat integration-event payloads exchanged with {@code courses-py}.
 * Payloads are JSON objects whose keys are the Java record component names ({@code courseId}, {@code username},
 * {@code email}, {@code rating}); the Python side reads/writes the same keys.
 * Kept dependency-free on purpose so the bridge stays trivially removable at cutover.
 */
final class IntegrationEventJson {

    private static final Pattern COURSE_ID = Pattern.compile("\"courseId\"\\s*:\\s*\"([0-9a-fA-F-]{36})\"");

    private IntegrationEventJson() {
    }

    static String courseId(UUID courseId) {
        return "{\"courseId\":\"" + courseId + "\"}";
    }

    static String courseIdAndUsername(UUID courseId, String username) {
        return "{\"courseId\":\"" + courseId + "\",\"username\":" + string(username) + "}";
    }

    static String usernameAndEmail(String username, String email) {
        return "{\"username\":" + string(username) + ",\"email\":" + string(email) + "}";
    }

    static String courseIdAndRating(UUID courseId, double rating) {
        return "{\"courseId\":\"" + courseId + "\",\"rating\":" + rating + "}";
    }

    static Optional<UUID> readCourseId(String json) {
        Matcher matcher = COURSE_ID.matcher(json);
        return matcher.find() ? Optional.of(UUID.fromString(matcher.group(1))) : Optional.empty();
    }

    private static String string(String value) {
        if (value == null) {
            return "null";
        }
        StringBuilder sb = new StringBuilder("\"");
        for (char c : value.toCharArray()) {
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
                }
            }
        }
        return sb.append('"').toString();
    }
}
