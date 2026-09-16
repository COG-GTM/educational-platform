"""Wire-level contract with the Java ``courses/event-bridge``.

The byte strings below are the exact bodies pinned by ``OutboundIntegrationEventBridgeTest`` /
``IntegrationEventJsonTest`` on the Java side; here they travel the full Python consumer path
(``RabbitMQMessageBroker`` -> ``decode_payload`` -> ``IntegrationEventHandlers`` -> database). The reverse direction
checks that the body Python publishes is exactly the literal ``InboundIntegrationEventBridgeTest`` accepts.
"""

from __future__ import annotations

from uuid import UUID

import pytest
from sqlalchemy.orm import Session, sessionmaker

from courses_py.application.course.create import CreateCourseCommand
from courses_py.domain.course import Course
from courses_py.domain.enums import ApprovalStatus
from courses_py.infrastructure.messaging import topics
from courses_py.infrastructure.messaging.publisher import AfterCommitIntegrationEventPublisher, publish_pending_events
from courses_py.infrastructure.messaging.rabbitmq import RabbitMQMessageBroker
from courses_py.infrastructure.persistence.repositories import SqlAlchemyCourseRepository, SqlAlchemyTeacherRepository
from courses_py.integration_events.consumer import register_consumers
from courses_py.integration_events.events import SendCourseToApproveIntegrationEvent
from courses_py.tests.conftest import count, insert_teacher
from courses_py.tests.infrastructure.test_rabbitmq import URL, FakeChannel, FakeConnectionFactory

COURSE_ID = UUID("123e4567-e89b-12d3-a456-426655440001")

# IntegrationEventJson.courseId / courseIdAndUsername / usernameAndEmail / courseIdAndRating output
JAVA_COURSE_APPROVED = b'{"courseId":"123e4567-e89b-12d3-a456-426655440001"}'
JAVA_STUDENT_ENROLLED = b'{"courseId":"123e4567-e89b-12d3-a456-426655440001","username":"student"}'
JAVA_USER_CREATED = b'{"username":"teacher","email":"teacher@example.com"}'
JAVA_USER_CREATED_NULL_EMAIL = b'{"username":"teacher","email":null}'
JAVA_RATING_RECALCULATED = b'{"courseId":"123e4567-e89b-12d3-a456-426655440001","rating":4.5}'


@pytest.fixture
def draft_course(session_factory: sessionmaker[Session]) -> UUID:
    with session_factory() as session:
        teacher = insert_teacher(session)
        assert teacher.id is not None
        course = Course(CreateCourseCommand(name="name", description="description"), teacher.id)
        course.uuid = COURSE_ID
        session.add(course)
        session.commit()
    return COURSE_ID


@pytest.fixture
def factory() -> FakeConnectionFactory:
    return FakeConnectionFactory()


@pytest.fixture
def channel(factory: FakeConnectionFactory, session_factory: sessionmaker[Session]) -> FakeChannel:
    """The consumer process: ``courses-py-consumer`` subscribed to the four inbound topics."""
    register_consumers(RabbitMQMessageBroker(URL, connection_factory=factory), session_factory)
    return factory.channel


def _course(session_factory: sessionmaker[Session], uuid: UUID) -> Course:
    with session_factory() as session:
        course = SqlAlchemyCourseRepository(session).find_by_uuid(uuid)
        assert course is not None
        return course


def test_javaCourseApprovedByAdminBody_courseApprovedAndAcked(
    channel: FakeChannel, session_factory: sessionmaker[Session], draft_course: UUID
) -> None:
    channel.deliver("courses-py." + topics.COURSE_APPROVED_BY_ADMIN, JAVA_COURSE_APPROVED, delivery_tag=7)

    assert _course(session_factory, draft_course).approval_status == ApprovalStatus.APPROVED
    assert channel.acked == [7]
    assert channel.nacked == []


def test_javaStudentEnrolledBody_numberOfStudentsIncreased(
    channel: FakeChannel, session_factory: sessionmaker[Session], draft_course: UUID
) -> None:
    channel.deliver("courses-py." + topics.STUDENT_ENROLLED_TO_COURSE, JAVA_STUDENT_ENROLLED)

    assert _course(session_factory, draft_course).number_of_students.number == 1
    assert channel.acked == [1]


@pytest.mark.parametrize("body", [JAVA_USER_CREATED, JAVA_USER_CREATED_NULL_EMAIL], ids=["email", "null email"])
def test_javaUserCreatedBody_teacherCreated(
    channel: FakeChannel, session_factory: sessionmaker[Session], body: bytes
) -> None:
    channel.deliver("courses-py." + topics.USER_CREATED, body)

    with session_factory() as session:
        assert SqlAlchemyTeacherRepository(session).find_by_username("teacher") is not None
    assert channel.acked == [1]


def test_javaRatingRecalculatedBody_ratingUpdated(
    channel: FakeChannel, session_factory: sessionmaker[Session], draft_course: UUID
) -> None:
    channel.deliver("courses-py." + topics.COURSE_RATING_RECALCULATED, JAVA_RATING_RECALCULATED)

    assert _course(session_factory, draft_course).rating.rating == 4.5
    assert channel.acked == [1]


def test_javaIntegerRatingFormatting_parsedAsFloat(
    channel: FakeChannel, session_factory: sessionmaker[Session], draft_course: UUID
) -> None:
    # Java's Double.toString renders 4 as "4.0" and keeps 17 significant digits
    channel.deliver(
        "courses-py." + topics.COURSE_RATING_RECALCULATED,
        b'{"courseId":"123e4567-e89b-12d3-a456-426655440001","rating":3.3333333333333335}',
    )

    assert _course(session_factory, draft_course).rating.rating == 3.3333333333333335


def test_javaEscapedUsername_unescapedBeforeStoring(
    channel: FakeChannel, session_factory: sessionmaker[Session]
) -> None:
    # IntegrationEventJson.string("a\\b\n\r\t\u0001") and a non-ASCII username sent as raw UTF-8
    channel.deliver("courses-py." + topics.USER_CREATED, b'{"username":"a\\\\b\\n\\r\\t\\u0001","email":null}')
    channel.deliver("courses-py." + topics.USER_CREATED, '{"username":"ünïcødé","email":"e@x.io"}'.encode())

    with session_factory() as session:
        repository = SqlAlchemyTeacherRepository(session)
        assert repository.find_by_username("a\\b\n\r\t\x01") is not None
        assert repository.find_by_username("ünïcødé") is not None
    assert channel.acked == [1, 1]


def test_javaNullUsername_rejectedWithoutRequeue(channel: FakeChannel, session_factory: sessionmaker[Session]) -> None:
    # IntegrationEventJson.usernameAndEmail(null, "") is syntactically valid JSON but not a valid event
    channel.deliver("courses-py." + topics.USER_CREATED, b'{"username":null,"email":""}', delivery_tag=3)

    assert channel.nacked == [(3, False)]
    assert channel.acked == []
    with session_factory() as session:
        assert count(session, "teacher") == 0


def test_javaBodyForUnknownCourse_requeuedOnceThenRejected(
    channel: FakeChannel, session_factory: sessionmaker[Session]
) -> None:
    queue = "courses-py." + topics.COURSE_APPROVED_BY_ADMIN

    channel.deliver(queue, JAVA_COURSE_APPROVED, delivery_tag=1)
    channel.deliver(queue, JAVA_COURSE_APPROVED, delivery_tag=2, redelivered=True)

    assert channel.nacked == [(1, True), (2, False)]
    assert channel.acked == []


def test_sendCourseToApprove_publishedBodyIsTheLiteralJavaInboundBridgeAccepts(
    factory: FakeConnectionFactory, session_factory: sessionmaker[Session]
) -> None:
    broker = RabbitMQMessageBroker(URL, connection_factory=factory)

    with session_factory() as session:
        AfterCommitIntegrationEventPublisher(session).publish(SendCourseToApproveIntegrationEvent(course_id=COURSE_ID))
        publish_pending_events(session, broker)

    (message,) = factory.channel.published
    assert message["exchange"] == "educational-platform.integration-events"
    assert message["routing_key"] == "courses.send-course-to-approve"
    # InboundIntegrationEventBridgeTest.onSendCourseToApprove_pythonPayload_sendCourseToApproveEventRepublished
    assert message["body"] == b'{"courseId": "123e4567-e89b-12d3-a456-426655440001"}'
    assert message["properties"].content_type == "application/json"
