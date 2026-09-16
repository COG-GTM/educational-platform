from __future__ import annotations

from collections.abc import Iterator
from uuid import UUID

import pytest
from fastapi import FastAPI
from fastapi.testclient import TestClient
from sqlalchemy import Engine, text
from sqlalchemy.orm import Session, sessionmaker

from courses_py.api.app import create_app
from courses_py.application.course.create import CreateCourseCommand
from courses_py.application.security import ROLE_TEACHER, Principal, StaticCurrentUser
from courses_py.application.teacher.create import CreateTeacherCommand
from courses_py.config import Settings
from courses_py.domain.course import Course
from courses_py.domain.teacher import Teacher
from courses_py.infrastructure.messaging.broker import InMemoryMessageBroker
from courses_py.infrastructure.persistence.database import build_engine, build_session_factory
from courses_py.infrastructure.persistence.orm import metadata, start_mappers
from courses_py.infrastructure.security.jwt import JwtTokenProvider

TEACHER_USERNAME = "teacher"
APPROVED_COURSE_UUID = UUID("123e4567-e89b-12d3-a456-426655440001")

TEST_SETTINGS = Settings(
    database_url="sqlite://", jwt_secret_key="secret-key", broker="memory", rabbitmq_url="amqp://unused"
)


@pytest.fixture
def engine() -> Iterator[Engine]:
    start_mappers()
    engine = build_engine(TEST_SETTINGS.database_url)
    metadata.create_all(engine)
    yield engine
    engine.dispose()


@pytest.fixture
def session_factory(engine: Engine) -> sessionmaker[Session]:
    return build_session_factory(engine)


@pytest.fixture
def session(session_factory: sessionmaker[Session]) -> Iterator[Session]:
    with session_factory() as session:
        yield session


@pytest.fixture
def teacher_principal() -> Principal:
    return Principal(username=TEACHER_USERNAME, authorities=frozenset({"ROLE_TEACHER"}))


@pytest.fixture
def teacher_user(teacher_principal: Principal) -> StaticCurrentUser:
    return StaticCurrentUser(teacher_principal)


@pytest.fixture
def broker() -> InMemoryMessageBroker:
    return InMemoryMessageBroker()


@pytest.fixture
def app(engine: Engine, broker: InMemoryMessageBroker) -> FastAPI:
    return create_app(TEST_SETTINGS, engine=engine, broker=broker)


@pytest.fixture
def client(app: FastAPI) -> Iterator[TestClient]:
    with TestClient(app, raise_server_exceptions=False) as client:
        yield client


@pytest.fixture
def jwt_provider() -> JwtTokenProvider:
    return JwtTokenProvider(TEST_SETTINGS.jwt_secret_key)


@pytest.fixture
def teacher_token(jwt_provider: JwtTokenProvider) -> str:
    """``SignUpHelper.signUpTeacher()``: a token for a user with role TEACHER."""
    return jwt_provider.create_token(TEACHER_USERNAME, [ROLE_TEACHER])


def insert_teacher(session: Session, username: str = TEACHER_USERNAME) -> Teacher:
    teacher = Teacher(CreateTeacherCommand(username=username))
    session.add(teacher)
    session.flush()
    return teacher


def insert_approved_course(session: Session, teacher: Teacher, uuid: UUID = APPROVED_COURSE_UUID) -> Course:
    """``approved_course.sql`` / ``insert_data.sql``: an APPROVED course owned by ``teacher``."""
    assert teacher.id is not None
    course = Course(CreateCourseCommand(name="course name", description="description"), teacher.id)
    course.uuid = uuid
    course.approve()
    session.add(course)
    session.flush()
    return course


@pytest.fixture
def insert_data(session_factory: sessionmaker[Session]) -> UUID:
    """``@Sql(scripts = "classpath:insert_data.sql")``"""
    with session_factory() as session:
        insert_approved_course(session, insert_teacher(session))
        session.commit()
    return APPROVED_COURSE_UUID


def count(session: Session, table: str) -> int:
    return int(session.execute(text(f"select count(*) from {table}")).scalar_one())
