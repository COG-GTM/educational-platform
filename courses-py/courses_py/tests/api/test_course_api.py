"""Port of ``CourseApiTest.java`` (plus error-shape and read-endpoint coverage) using FastAPI's ``TestClient``."""

from __future__ import annotations

from uuid import UUID, uuid4

from fastapi.testclient import TestClient
from sqlalchemy.orm import Session, sessionmaker

from courses_py.application.security import ROLE_STUDENT, ROLE_TEACHER
from courses_py.infrastructure.messaging import topics
from courses_py.infrastructure.messaging.broker import InMemoryMessageBroker
from courses_py.infrastructure.security.jwt import JwtTokenProvider
from courses_py.tests.conftest import TEACHER_USERNAME, count, insert_teacher


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
            {"type": "Lecture", "title": "l", "description": "d", "serial_number": 1, "text": "hello"},
            {"type": "Quiz", "title": "q", "description": "d", "serial_number": 2, "questions": [{"content": "why?"}]},
        ],
    }

    response = client.post("/courses", json=body, headers=_auth(teacher_token))

    assert response.status_code == 201
    course = client.get(f"/courses/{response.json()['uuid']}", headers=_auth(teacher_token)).json()
    assert [item["type"] for item in course["curriculumItems"]] == ["Lecture", "Quiz"]
    assert course["curriculumItems"][0]["text"] == "hello"
    assert course["curriculumItems"][1]["questions"] == [{"content": "why?"}]


def test_create_blankName_badRequest(client: TestClient, teacher_token: str, insert_data: UUID) -> None:
    response = client.post("/courses", json={"name": " ", "description": "description"}, headers=_auth(teacher_token))

    assert response.status_code == 400
    assert response.json() == {"errors": ["Value error, must not be blank"]}


def test_create_noToken_forbidden(client: TestClient) -> None:
    response = client.post("/courses", json={"name": "name", "description": "description"})

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


def test_publish_alreadyApprovedCourse_noContent(client: TestClient, teacher_token: str, insert_data: UUID) -> None:
    response = client.put(f"/courses/{insert_data}/publish-status", headers=_auth(teacher_token))

    assert response.status_code == 204
    assert response.content == b""


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
