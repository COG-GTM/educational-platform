package com.educational.platform.course.enrollments.course.create;

import java.util.List;
import java.util.UUID;

/**
 * Represents Create Course Command. Mirrors the published course snapshot (name and lectures) into the enrollments module.
 */
public record CreateCourseCommand(UUID uuid, String name, List<CreateLectureCommand> lectures) {

    public CreateCourseCommand(UUID uuid) {
        this(uuid, null, List.of());
    }

    /**
     * Represents a lecture of the course.
     */
    public record CreateLectureCommand(UUID uuid, String title, Integer serialNumber) {

    }
}
