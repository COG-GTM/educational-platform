"""Port of ``CourseApiTest.java`` (plus error-shape and read-endpoint coverage) using FastAPI's ``TestClient``."""

from __future__ import annotations

from uuid import UUID, uuid4

import pytest
from fastapi import FastAPI
from fastapi.testclient import TestClient
from sqlalchemy import Engine
from sqlalchemy.orm import Session, sessionmaker

from courses_py.api.app import create_app
from courses_py.api.dependencies import create_course_handler
from courses_py.application.course.create import CreateCourseCommand, CreateCourseCommandHandler
from courses_py.application.security import ROLE_STUDENT, ROLE_TEACHER
from courses_py.domain import ApprovalStatus
from courses_py.infrastructure.messaging import topics
from courses_py.infrastructure.messaging.broker import InMemoryMessageBroker, MessageBroker, MessageHandler, Payload
from courses_py.infrastructure.persistence.repositories import SqlAlchemyCourseRepository
from courses_py.infrastructure.security.jwt import JwtTokenProvider
from courses_py.tests.conftest import TEACHER_USERNAME, TEST_SETTINGS, count, insert_teacher


def _auth(token: str) -> dict[str, str]:
    return {"Authorization": f"Bearer {token}"}


def test_create_validRequest_created(
    client: TestClient, teacher_token: str, insert_data: UUID, session_factory: sessionmaker[Session]
) -> None:
    response = client.post(
        "/courses", json={"name": "name", "description": "description"}, headers=_auth(teacher_token)
    )

    assert response.status_code == 201
    uuid = UUID(response.json()["uuid"])
    with session_factory() as session:
        assert count(session, "course") == 2
    assert client.get(f"/courses/{uuid}", headers=_auth(teacher_token)).json()["name"] == "name"


def test_create_withCurriculumItems_createdAndReadable(
    client: TestClient, teacher_token: str, insert_data: UUID
) -> None:
    body = {
        "name": "name",
        "description": "description",
        "curriculumItems": [
            {"type": "Lecture", "title": "l", "description": "d", "serialNumber": 1, "text": "hello"},
            {"type": "Quiz", "title": "q", "description": "d", "serialNumber": 2, "questions": [{"content": "why?"}]},
        ],
    }

    response = client.post("/courses", json=body, headers=_auth(teacher_token))

    assert response.status_code == 201
    course = client.get(f"/courses/{response.json()['uuid']}", headers=_auth(teacher_token)).json()
    assert [item["type"] for item in course["curriculumItems"]] == ["Lecture", "Quiz"]
    assert [item["serialNumber"] for item in course["curriculumItems"]] == [1, 2]
    assert course["curriculumItems"][0]["text"] == "hello"
    assert course["curriculumItems"][1]["questions"] == [{"content": "why?"}]


def test_create_curriculumItemWithoutSerialNumber_badRequest(
    client: TestClient, teacher_token: str, insert_data: UUID, session_factory: sessionmaker[Session]
) -> None:
    body = {
        "name": "name",
        "description": "description",
        "curriculumItems": [{"type": "Lecture", "title": "l", "description": "d", "text": "hello"}],
    }

    response = client.post("/courses", json=body, headers=_auth(teacher_token))

    assert response.status_code == 400
    assert response.json() == {"errors": ["Field required"]}
    with session_factory() as session:
        assert count(session, "course") == 1


def test_create_blankName_badRequest(client: TestClient, teacher_token: str, insert_data: UUID) -> None:
    response = client.post("/courses", json={"name": " ", "description": "description"}, headers=_auth(teacher_token))

    assert response.status_code == 400
    assert response.json() == {"errors": ["Value error, must not be blank"]}


def test_create_bothFieldsMissing_badRequestWithOneErrorPerField(
    client: TestClient, teacher_token: str, insert_data: UUID
) -> None:
    response = client.post("/courses", json={}, headers=_auth(teacher_token))

    assert response.status_code == 400
    assert response.json() == {"errors": ["Field required", "Field required"]}


def test_create_nonJsonBody_badRequest(client: TestClient, teacher_token: str, insert_data: UUID) -> None:
    response = client.post(
        "/courses", content=b"not json", headers={**_auth(teacher_token), "Content-Type": "application/json"}
    )

    assert response.status_code == 400
    assert len(response.json()["errors"]) == 1


def test_create_unknownCurriculumItemType_badRequest(client: TestClient, teacher_token: str, insert_data: UUID) -> None:
    body = {
        "name": "name",
        "description": "description",
        "curriculumItems": [{"type": "Video", "title": "v", "description": "d"}],
    }

    response = client.post("/courses", json=body, headers=_auth(teacher_token))

    assert response.status_code == 400
    assert "Lecture" in response.json()["errors"][0] and "Quiz" in response.json()["errors"][0]


def test_create_snakeCaseCurriculumItems_accepted(client: TestClient, teacher_token: str, insert_data: UUID) -> None:
    body = {
        "name": "name",
        "description": "description",
        "curriculum_items": [{"type": "Lecture", "title": "l", "description": "d", "serial_number": 1}],
    }

    response = client.post("/courses", json=body, headers=_auth(teacher_token))

    assert response.status_code == 201
    course = client.get(f"/courses/{response.json()['uuid']}", headers=_auth(teacher_token)).json()
    assert course["curriculumItems"][0]["text"] == ""  # curriculum_item.content is NOT NULL DEFAULT ''
    assert course["curriculumItems"][0]["serialNumber"] == 1


def test_create_validationFailure_nothingPersisted(
    client: TestClient, teacher_token: str, insert_data: UUID, session_factory: sessionmaker[Session]
) -> None:
    client.post("/courses", json={"name": "", "description": "description"}, headers=_auth(teacher_token))

    with session_factory() as session:
        assert count(session, "course") == 1


def test_create_noToken_forbidden(client: TestClient) -> None:
    response = client.post("/courses", json={"name": "name", "description": "description"})

    assert response.status_code == 403
    assert response.json() == {"errors": ["Access Denied"]}


@pytest.mark.parametrize("header", ["Basic dXNlcjpwYXNz", "Token abc", "Bearer", ""])
def test_create_nonBearerAuthorization_forbidden(client: TestClient, header: str) -> None:
    response = client.post(
        "/courses", json={"name": "name", "description": "description"}, headers={"Authorization": header}
    )

    assert response.status_code == 403
    assert response.json() == {"errors": ["Access Denied"]}


def test_create_invalidToken_badRequest(client: TestClient) -> None:
    response = client.post("/courses", json={"name": "name", "description": "description"}, headers=_auth("garbage"))

    assert response.status_code == 400
    assert response.json() == {"errors": ["Expired or invalid JWT token"]}


def test_create_wrongSecret_badRequest(client: TestClient) -> None:
    token = JwtTokenProvider("another-secret").create_token(TEACHER_USERNAME, [ROLE_TEACHER])

    response = client.post("/courses", json={"name": "name", "description": "description"}, headers=_auth(token))

    assert response.status_code == 400


def test_create_studentRole_forbidden(client: TestClient, jwt_provider: JwtTokenProvider, insert_data: UUID) -> None:
    token = jwt_provider.create_token(TEACHER_USERNAME, [ROLE_STUDENT])

    response = client.post("/courses", json={"name": "name", "description": "description"}, headers=_auth(token))

    assert response.status_code == 403


def test_create_unknownTeacher_badRequest(client: TestClient, jwt_provider: JwtTokenProvider) -> None:
    token = jwt_provider.create_token("nobody", [ROLE_TEACHER])

    response = client.post("/courses", json={"name": "name", "description": "description"}, headers=_auth(token))

    assert response.status_code == 400
    assert response.json() == {"errors": ["Teacher with username: nobody not found"]}


def test_create_unexpectedHandlerError_internalServerErrorShape(
    app: FastAPI, client: TestClient, teacher_token: str, insert_data: UUID
) -> None:
    def failing_handler() -> None:
        raise RuntimeError("database exploded")

    app.dependency_overrides[create_course_handler] = failing_handler
    try:
        response = client.post(
            "/courses", json={"name": "name", "description": "description"}, headers=_auth(teacher_token)
        )
    finally:
        app.dependency_overrides.clear()

    assert response.status_code == 500
    assert response.json() == {"errors": ["database exploded"]}


def test_create_handlerFailsAfterSave_internalServerErrorAndRolledBack(
    client: TestClient,
    teacher_token: str,
    insert_data: UUID,
    session_factory: sessionmaker[Session],
    monkeypatch: pytest.MonkeyPatch,
) -> None:
    """The request session is only committed when the handler returns; a late failure discards the saved course."""
    original = CreateCourseCommandHandler.handle

    def failing(self: CreateCourseCommandHandler, command: CreateCourseCommand) -> UUID:
        original(self, command)
        raise RuntimeError("failure after save")

    monkeypatch.setattr(CreateCourseCommandHandler, "handle", failing)

    response = client.post(
        "/courses", json={"name": "name", "description": "description"}, headers=_auth(teacher_token)
    )

    assert response.status_code == 500
    assert response.json() == {"errors": ["failure after save"]}
    with session_factory() as session:
        assert count(session, "course") == 1


def test_publish_alreadyApprovedCourse_noContent(client: TestClient, teacher_token: str, insert_data: UUID) -> None:
    response = client.put(f"/courses/{insert_data}/publish-status", headers=_auth(teacher_token))

    assert response.status_code == 204
    assert response.content == b""


def test_publish_malformedUuid_badRequest(client: TestClient, teacher_token: str, insert_data: UUID) -> None:
    response = client.put("/courses/not-a-uuid/publish-status", headers=_auth(teacher_token))

    assert response.status_code == 400
    assert response.json()["errors"][0].startswith("Input should be a valid UUID")


def test_publish_noToken_forbidden(client: TestClient, insert_data: UUID) -> None:
    response = client.put(f"/courses/{insert_data}/publish-status")

    assert response.status_code == 403
    assert response.json() == {"errors": ["Access Denied"]}


def test_publish_studentRole_forbidden(client: TestClient, jwt_provider: JwtTokenProvider, insert_data: UUID) -> None:
    token = jwt_provider.create_token(TEACHER_USERNAME, [ROLE_STUDENT])

    response = client.put(f"/courses/{insert_data}/publish-status", headers=_auth(token))

    assert response.status_code == 403
    assert response.json() == {"errors": ["Access Denied"]}


def test_publish_notApprovedCourse_conflict(client: TestClient, teacher_token: str, insert_data: UUID) -> None:
    uuid = client.post(
        "/courses", json={"name": "name", "description": "description"}, headers=_auth(teacher_token)
    ).json()["uuid"]

    response = client.put(f"/courses/{uuid}/publish-status", headers=_auth(teacher_token))

    assert response.status_code == 409
    assert response.json() == {
        "errors": [f"Course with uuid = {uuid} cannot be published, course should be approved by admin at first"]
    }


def test_publish_notOwner_forbidden(
    client: TestClient, jwt_provider: JwtTokenProvider, insert_data: UUID, session_factory: sessionmaker[Session]
) -> None:
    with session_factory() as session:
        insert_teacher(session, "other")
        session.commit()
    token = jwt_provider.create_token("other", [ROLE_TEACHER])

    response = client.put(f"/courses/{insert_data}/publish-status", headers=_auth(token))

    assert response.status_code == 403


def test_publish_unknownCourse_forbidden(client: TestClient, teacher_token: str, insert_data: UUID) -> None:
    response = client.put(f"/courses/{uuid4()}/publish-status", headers=_auth(teacher_token))

    assert response.status_code == 403


def test_sendToApprove_draftCourse_noContentAndEventPublished(
    client: TestClient, teacher_token: str, insert_data: UUID, broker: InMemoryMessageBroker
) -> None:
    uuid = client.post(
        "/courses", json={"name": "name", "description": "description"}, headers=_auth(teacher_token)
    ).json()["uuid"]

    response = client.put(f"/courses/{uuid}/approval-status", headers=_auth(teacher_token))

    assert response.status_code == 204
    assert broker.published == [(topics.SEND_COURSE_TO_APPROVE, {"courseId": uuid})]


def test_sendToApprove_alreadyApproved_conflict(client: TestClient, teacher_token: str, insert_data: UUID) -> None:
    response = client.put(f"/courses/{insert_data}/approval-status", headers=_auth(teacher_token))

    assert response.status_code == 409


def test_sendToApprove_alreadyApproved_javaMessageAndNoEvent(
    client: TestClient, teacher_token: str, insert_data: UUID, broker: InMemoryMessageBroker
) -> None:
    response = client.put(f"/courses/{insert_data}/approval-status", headers=_auth(teacher_token))

    assert response.json() == {
        "errors": [f"Course with uuid = {insert_data} cannot be sent for approval, course was already approved"]
    }
    assert broker.published == []


def test_sendToApprove_notOwner_forbiddenAndNoEvent(
    client: TestClient,
    jwt_provider: JwtTokenProvider,
    insert_data: UUID,
    broker: InMemoryMessageBroker,
    session_factory: sessionmaker[Session],
) -> None:
    with session_factory() as session:
        insert_teacher(session, "other")
        session.commit()
    token = jwt_provider.create_token("other", [ROLE_TEACHER])

    response = client.put(f"/courses/{insert_data}/approval-status", headers=_auth(token))

    assert response.status_code == 403
    assert broker.published == []


def test_sendToApprove_unknownCourse_forbidden(client: TestClient, teacher_token: str, insert_data: UUID) -> None:
    response = client.put(f"/courses/{uuid4()}/approval-status", headers=_auth(teacher_token))

    assert response.status_code == 403


def test_sendToApprove_thenApprovedByAdminEvent_coursePublishable(
    client: TestClient, teacher_token: str, insert_data: UUID, broker: InMemoryMessageBroker
) -> None:
    """Full strangler round trip with the in-memory broker: API publishes, the in-process consumer reacts."""
    uuid = client.post(
        "/courses", json={"name": "name", "description": "description"}, headers=_auth(teacher_token)
    ).json()["uuid"]
    client.put(f"/courses/{uuid}/approval-status", headers=_auth(teacher_token))
    assert client.put(f"/courses/{uuid}/publish-status", headers=_auth(teacher_token)).status_code == 409

    broker.publish(topics.COURSE_APPROVED_BY_ADMIN, {"courseId": uuid})

    assert client.put(f"/courses/{uuid}/publish-status", headers=_auth(teacher_token)).status_code == 204


def test_sendToApprove_brokerFails_stateCommittedAndFailureLogged(
    engine: Engine, teacher_token: str, session_factory: sessionmaker[Session], caplog: pytest.LogCaptureFixture
) -> None:
    """Events are published after the commit, so a broker outage surfaces as an error but never rolls back state."""

    class FailingBroker(InMemoryMessageBroker):
        def publish(self, topic: str, payload: Payload) -> None:
            raise ConnectionError("broker down")

    with session_factory() as session:
        insert_teacher(session)
        session.commit()
    app = create_app(TEST_SETTINGS, engine=engine, broker=FailingBroker())
    with TestClient(app, raise_server_exceptions=False) as client:
        uuid = client.post(
            "/courses", json={"name": "name", "description": "description"}, headers=_auth(teacher_token)
        ).json()["uuid"]

        with caplog.at_level("ERROR", logger="courses_py.api.errors"):
            client.put(f"/courses/{uuid}/approval-status", headers=_auth(teacher_token))

    assert any("Unhandled error" in record.message for record in caplog.records)
    with session_factory() as session:
        course = SqlAlchemyCourseRepository(session).find_by_uuid(UUID(uuid))
        assert course is not None
        assert course.approval_status == ApprovalStatus.WAITING_FOR_APPROVAL


def test_studentEnrolledEvent_visibleInListing(
    client: TestClient, teacher_token: str, insert_data: UUID, broker: InMemoryMessageBroker
) -> None:
    broker.publish(topics.STUDENT_ENROLLED_TO_COURSE, {"courseId": str(insert_data), "username": "student"})
    broker.publish(topics.STUDENT_ENROLLED_TO_COURSE, {"courseId": str(insert_data), "username": "other"})

    listed = client.get("/courses", headers=_auth(teacher_token)).json()

    assert listed[0]["numberOfStudents"] == 2


def test_userCreatedEvent_newTeacherCanCreateCourses(
    client: TestClient, jwt_provider: JwtTokenProvider, broker: InMemoryMessageBroker
) -> None:
    token = jwt_provider.create_token("newcomer", [ROLE_TEACHER])
    assert client.post("/courses", json={"name": "n", "description": "d"}, headers=_auth(token)).status_code == 400

    broker.publish(topics.USER_CREATED, {"username": "newcomer", "email": "newcomer@example.com"})

    assert client.post("/courses", json={"name": "n", "description": "d"}, headers=_auth(token)).status_code == 201


def test_createApp_nonInMemoryBroker_consumersNotRegisteredInApiProcess(engine: Engine) -> None:
    class RecordingBroker(MessageBroker):
        def __init__(self) -> None:
            self.subscribed: list[str] = []

        def publish(self, topic: str, payload: Payload) -> None: ...

        def subscribe(self, topic: str, handler: MessageHandler) -> None:
            self.subscribed.append(topic)

        def start_consuming(self) -> None: ...

        def close(self) -> None: ...

    broker = RecordingBroker()

    app = create_app(TEST_SETTINGS, engine=engine, broker=broker)

    assert broker.subscribed == []
    assert app.state.context.broker is broker


def test_createApp_inMemoryBroker_consumersRegisteredForAllInboundTopics(engine: Engine) -> None:
    broker = InMemoryMessageBroker()

    create_app(TEST_SETTINGS, engine=engine, broker=broker)

    assert set(broker._handlers) == {
        topics.COURSE_APPROVED_BY_ADMIN,
        topics.STUDENT_ENROLLED_TO_COURSE,
        topics.USER_CREATED,
        topics.COURSE_RATING_RECALCULATED,
    }


def test_list_and_get_authenticatedAnyRole(
    client: TestClient, jwt_provider: JwtTokenProvider, insert_data: UUID
) -> None:
    student = _auth(jwt_provider.create_token("student", ["STUDENT"]))
    listed = client.get("/courses", headers=student)
    single = client.get(f"/courses/{insert_data}", headers=student)
    missing = client.get(f"/courses/{uuid4()}", headers=student)

    assert listed.status_code == 200
    assert listed.json() == [
        {"uuid": str(insert_data), "name": "course name", "description": "description", "numberOfStudents": 0}
    ]
    assert single.status_code == 200
    assert single.json()["curriculumItems"] == []
    assert missing.status_code == 404
    assert missing.json()["errors"][0].startswith("Course with uuid: ")


def test_list_withoutToken_forbidden(client: TestClient) -> None:
    response = client.get("/courses")

    assert response.status_code == 403
    assert response.json() == {"errors": ["Access Denied"]}


def test_list_empty_emptyArray(client: TestClient, teacher_token: str) -> None:
    response = client.get("/courses", headers=_auth(teacher_token))

    assert response.status_code == 200
    assert response.json() == []


def test_get_malformedUuid_badRequest(client: TestClient, teacher_token: str) -> None:
    response = client.get("/courses/123", headers=_auth(teacher_token))

    assert response.status_code == 400


def test_get_expiredToken_badRequest(client: TestClient, insert_data: UUID) -> None:
    token = JwtTokenProvider(TEST_SETTINGS.jwt_secret_key, validity_ms=-60_000).create_token(
        TEACHER_USERNAME, [ROLE_TEACHER]
    )

    response = client.get(f"/courses/{insert_data}", headers=_auth(token))

    assert response.status_code == 400
    assert response.json() == {"errors": ["Expired or invalid JWT token"]}


def test_get_courseWithItems_camelCaseDtoShape(client: TestClient, teacher_token: str, insert_data: UUID) -> None:
    body = {
        "name": "name",
        "description": "description",
        "curriculumItems": [
            {"type": "Quiz", "title": "q", "description": "d", "serialNumber": 1, "questions": []},
        ],
    }
    uuid = client.post("/courses", json=body, headers=_auth(teacher_token)).json()["uuid"]

    course = client.get(f"/courses/{uuid}", headers=_auth(teacher_token)).json()

    assert set(course) == {"uuid", "name", "description", "numberOfStudents", "curriculumItems"}
    (quiz,) = course["curriculumItems"]
    assert set(quiz) == {"type", "uuid", "title", "description", "serialNumber", "questions"}
    assert quiz["questions"] == [] and quiz["serialNumber"] == 1
