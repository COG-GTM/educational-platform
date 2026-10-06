package com.educational.platform.course.enrollments;

import com.educational.platform.common.exception.UnprocessableEntityException;

import java.util.UUID;

/**
 * Thrown when learning progress is changed on an archived enrollment. The enrollment has to be restored first.
 */
public class CourseEnrollmentArchivedException extends UnprocessableEntityException {

    public CourseEnrollmentArchivedException(UUID uuid) {
        super(String.format("Course enrollment with uuid: %s is archived, restore it to continue learning", uuid));
    }
}
