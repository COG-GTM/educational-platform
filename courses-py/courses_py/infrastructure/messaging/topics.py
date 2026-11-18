"""Broker topic names (RabbitMQ routing keys) shared with the Java bridge.

Keep in sync with ``courses/event-bridge/.../IntegrationEventTopics.java``.
"""

EXCHANGE = "educational-platform.integration-events"

COURSE_APPROVED_BY_ADMIN = "administration.course-approved-by-admin"
STUDENT_ENROLLED_TO_COURSE = "course-enrollments.student-enrolled-to-course"
USER_CREATED = "users.user-created"
COURSE_RATING_RECALCULATED = "course-reviews.course-rating-recalculated"
SEND_COURSE_TO_APPROVE = "courses.send-course-to-approve"

CONSUMER_QUEUE_PREFIX = "courses-py."
