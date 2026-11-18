from __future__ import annotations

from collections.abc import Iterator
from uuid import UUID

import pytest
from sqlalchemy import Engine, text
from sqlalchemy.orm import Session, sessionmaker

from course_reviews.infrastructure.database import build_engine
from course_reviews.infrastructure.orm import metadata, start_mappers

COURSE_UUID = UUID("123e4567-e89b-12d3-a456-426655440000")
COURSE_REVIEW_UUID = UUID("123e4567-e89b-12d3-a456-426655440001")
REVIEWER_USERNAME = "reviewer"

# Port of application/src/test/resources/course_review.sql. UUIDs are stored by SQLAlchemy's
# ``Uuid`` type as lowercase 32-char hex on SQLite, hence the lowercase literals.
COURSE_REVIEW_SQL = """
DELETE FROM course_review;
DELETE FROM reviewer;
DELETE FROM reviewable_course;

INSERT INTO reviewer (username) VALUES ('reviewer');
INSERT INTO reviewable_course (original_course_id) VALUES ('123e4567e89b12d3a456426655440000');
INSERT INTO course_review (uuid, rating, comment, reviewer, course) VALUES (
    '123e4567e89b12d3a456426655440001', 4, 'comment',
    (SELECT reviewer.id FROM reviewer WHERE reviewer.username = 'reviewer'),
    (SELECT reviewable_course.id FROM reviewable_course
       WHERE reviewable_course.original_course_id = '123e4567e89b12d3a456426655440000')
);

INSERT INTO reviewer (username) VALUES ('another-reviewer');
"""


def execute_script(session: Session, script: str) -> None:
    for statement in filter(None, (s.strip() for s in script.split(";"))):
        session.execute(text(statement))
    session.commit()


@pytest.fixture(scope="session", autouse=True)
def mappers() -> None:
    start_mappers()


@pytest.fixture
def engine() -> Iterator[Engine]:
    engine = build_engine("sqlite://")
    metadata.create_all(engine)
    yield engine
    engine.dispose()


@pytest.fixture
def session_factory(engine: Engine) -> sessionmaker[Session]:
    return sessionmaker(bind=engine, autoflush=False, expire_on_commit=False)


@pytest.fixture
def session(session_factory: sessionmaker[Session]) -> Iterator[Session]:
    with session_factory() as session:
        yield session
