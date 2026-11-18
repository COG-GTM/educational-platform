"""Integration event payload schemas shared with the Java monolith.

Field names are the camelCase Java record component names (``courseId``, ``username``, ``email``, ``rating``) so
that the JSON produced by the Java bridge (``courses/event-bridge``) and by this service is identical.
Each event maps to one broker topic (``TOPIC``); see ``courses_py/infrastructure/messaging/topics.py``.
"""

from __future__ import annotations

from typing import ClassVar
from uuid import UUID

from pydantic import BaseModel, ConfigDict
from pydantic.alias_generators import to_camel

from courses_py.infrastructure.messaging import topics


class IntegrationEvent(BaseModel):
    model_config = ConfigDict(alias_generator=to_camel, populate_by_name=True, frozen=True, extra="ignore")

    TOPIC: ClassVar[str]

    def to_payload(self) -> dict[str, object]:
        return self.model_dump(mode="json", by_alias=True)


class CourseApprovedByAdminIntegrationEvent(IntegrationEvent):
    """administration -> courses"""

    TOPIC = topics.COURSE_APPROVED_BY_ADMIN

    course_id: UUID


class StudentEnrolledToCourseIntegrationEvent(IntegrationEvent):
    """course-enrollments -> courses"""

    TOPIC = topics.STUDENT_ENROLLED_TO_COURSE

    course_id: UUID
    username: str


class UserCreatedIntegrationEvent(IntegrationEvent):
    """users -> courses"""

    TOPIC = topics.USER_CREATED

    username: str
    email: str | None = None


class CourseRatingRecalculatedIntegrationEvent(IntegrationEvent):
    """course-reviews -> courses"""

    TOPIC = topics.COURSE_RATING_RECALCULATED

    course_id: UUID
    rating: float


class SendCourseToApproveIntegrationEvent(IntegrationEvent):
    """courses -> administration"""

    TOPIC = topics.SEND_COURSE_TO_APPROVE

    course_id: UUID


INBOUND_EVENTS: tuple[type[IntegrationEvent], ...] = (
    CourseApprovedByAdminIntegrationEvent,
    StudentEnrolledToCourseIntegrationEvent,
    UserCreatedIntegrationEvent,
    CourseRatingRecalculatedIntegrationEvent,
)
