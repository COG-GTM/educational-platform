package com.educational.platform.courses.integration.event;

import java.util.List;
import java.util.UUID;

/**
 * Represents course published integration event, should be published after a course becomes available to students.
 * Carries the course snapshot other modules need to track learning progress.
 */
public record CoursePublishedIntegrationEvent(UUID courseId, String name, List<Lecture> lectures) {

    /**
     * Represents a lecture of the published course.
     */
    public record Lecture(UUID uuid, String title, Integer serialNumber) {

    }
}
