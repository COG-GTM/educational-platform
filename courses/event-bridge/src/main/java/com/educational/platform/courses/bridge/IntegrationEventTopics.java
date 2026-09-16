package com.educational.platform.courses.bridge;

/**
 * Broker exchange and routing keys shared with the Python {@code courses-py} service.
 * Keep in sync with {@code courses-py/courses_py/infrastructure/messaging/topics.py}.
 */
public final class IntegrationEventTopics {

    public static final String EXCHANGE = "educational-platform.integration-events";

    public static final String COURSE_APPROVED_BY_ADMIN = "administration.course-approved-by-admin";
    public static final String STUDENT_ENROLLED_TO_COURSE = "course-enrollments.student-enrolled-to-course";
    public static final String USER_CREATED = "users.user-created";
    public static final String COURSE_RATING_RECALCULATED = "course-reviews.course-rating-recalculated";
    public static final String SEND_COURSE_TO_APPROVE = "courses.send-course-to-approve";

    /** Queue on which the monolith receives events published by {@code courses-py}. */
    public static final String MONOLITH_SEND_COURSE_TO_APPROVE_QUEUE = "java-monolith." + SEND_COURSE_TO_APPROVE;

    private IntegrationEventTopics() {
    }
}
