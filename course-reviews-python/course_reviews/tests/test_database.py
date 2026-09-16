"""Tests for the SQLAlchemy engine factory, the per-request session dependency and the Alembic migration."""

from __future__ import annotations

import os
import subprocess
import sys
from collections.abc import Generator
from pathlib import Path
from typing import cast
from uuid import uuid4

import pytest
from sqlalchemy import String, create_engine, inspect, select
from sqlalchemy.engine import make_url
from sqlalchemy.orm import Session, sessionmaker
from sqlalchemy.pool import StaticPool

from course_reviews.domain.course_review import CourseReview
from course_reviews.domain.reviewable_course import ReviewableCourse
from course_reviews.domain.reviewer import Reviewer
from course_reviews.infrastructure import database
from course_reviews.infrastructure.database import build_engine, get_session
from course_reviews.infrastructure.orm import metadata
from course_reviews.infrastructure.repositories import (
    CourseReviewRepository,
    ReviewableCourseRepository,
    ReviewerRepository,
)

PROJECT_ROOT = Path(__file__).resolve().parents[2]


# --- build_engine ---------------------------------------------------------------------------------------------------


@pytest.mark.parametrize("url", ["sqlite://", "sqlite:///:memory:"])
def test_build_engine_in_memory_sqlite_uses_static_pool(url: str) -> None:
    engine = build_engine(url)

    assert isinstance(engine.pool, StaticPool)
    assert engine.dialect.name == "sqlite"
    engine.dispose()


def test_build_engine_file_sqlite_does_not_use_static_pool(tmp_path: Path) -> None:
    engine = build_engine(f"sqlite:///{tmp_path / 'reviews.db'}")

    assert not isinstance(engine.pool, StaticPool)
    engine.dispose()


def test_build_engine_in_memory_sqlite_shares_schema_across_connections() -> None:
    # given
    engine = build_engine("sqlite://")
    metadata.create_all(engine)

    # when / then: a second connection sees tables created through the first one
    with engine.connect() as connection:
        assert set(inspect(connection).get_table_names()) >= {"course_review", "reviewable_course", "reviewer"}
    engine.dispose()


def test_build_engine_without_url_uses_configured_database_url() -> None:
    engine = build_engine()

    assert engine.url == make_url(database.DATABASE_URL)
    engine.dispose()


# --- get_session ----------------------------------------------------------------------------------------------------


@pytest.fixture
def patched_session_local(
    monkeypatch: pytest.MonkeyPatch, session_factory: sessionmaker[Session]
) -> sessionmaker[Session]:
    monkeypatch.setattr(database, "SessionLocal", session_factory)
    return session_factory


def test_get_session_commits_on_success(patched_session_local: sessionmaker[Session]) -> None:
    # given
    generator = get_session()
    session = next(generator)

    # when
    session.add(Reviewer("committed"))
    with pytest.raises(StopIteration):
        next(generator)

    # then
    with patched_session_local() as verification:
        assert verification.scalars(select(Reviewer)).one().username == "committed"


def test_get_session_rolls_back_and_reraises_on_error(patched_session_local: sessionmaker[Session]) -> None:
    # given
    generator = cast(Generator[Session, None, None], get_session())
    session = next(generator)
    session.add(Reviewer("rolled-back"))

    # when / then
    with pytest.raises(RuntimeError, match="request failed"):
        generator.throw(RuntimeError("request failed"))
    with patched_session_local() as verification:
        assert verification.scalars(select(Reviewer)).all() == []


def test_get_session_closes_session_after_request(patched_session_local: sessionmaker[Session]) -> None:
    generator = get_session()
    session = next(generator)
    session.add(Reviewer("x"))
    session.flush()
    assert session.is_active

    with pytest.raises(StopIteration):
        next(generator)

    assert not session.in_transaction()


# --- Alembic migration ----------------------------------------------------------------------------------------------


def _alembic(url: str, *args: str) -> subprocess.CompletedProcess[str]:
    env = {**os.environ, "COURSE_REVIEWS_DATABASE_URL": url}
    return subprocess.run(
        [sys.executable, "-m", "alembic", *args],
        cwd=PROJECT_ROOT,
        env=env,
        capture_output=True,
        text=True,
        check=False,
    )


@pytest.fixture
def migrated_database_url(tmp_path: Path) -> str:
    url = f"sqlite:///{tmp_path / 'migrated.db'}"
    result = _alembic(url, "upgrade", "head")
    assert result.returncode == 0, result.stderr
    return url


def test_alembic_upgrade_head_creates_schema_matching_orm_metadata(migrated_database_url: str) -> None:
    engine = create_engine(migrated_database_url)
    inspector = inspect(engine)

    assert set(inspector.get_table_names()) == set(metadata.tables) | {"alembic_version"}
    for table in metadata.tables.values():
        migrated_columns = {c["name"] for c in inspector.get_columns(table.name)}
        assert migrated_columns == {c.name for c in table.columns}, table.name

    fks = {fk["name"] for fk in inspector.get_foreign_keys("course_review")}
    assert fks == {"reviewer_fkey", "reviewable_course_fkey"}
    engine.dispose()


def test_alembic_migrated_columns_match_orm_nullability_and_string_lengths(migrated_database_url: str) -> None:
    engine = create_engine(migrated_database_url)
    inspector = inspect(engine)

    for table in metadata.tables.values():
        migrated = {c["name"]: c for c in inspector.get_columns(table.name)}
        for column in table.columns:
            assert migrated[column.name]["nullable"] is column.nullable, f"{table.name}.{column.name}"
            if isinstance(column.type, String):
                migrated_type = migrated[column.name]["type"]
                assert isinstance(migrated_type, String), f"{table.name}.{column.name}"
                assert migrated_type.length == column.type.length, f"{table.name}.{column.name}"

    comment = {c["name"]: c for c in inspector.get_columns("course_review")}["comment"]
    assert comment["nullable"] is True
    assert isinstance(comment["type"], String)
    assert comment["type"].length == 100
    engine.dispose()


def test_alembic_offline_mode_emits_ddl_without_touching_database(tmp_path: Path) -> None:
    database_file = tmp_path / "never-created.db"

    result = _alembic(f"sqlite:///{database_file}", "upgrade", "head", "--sql")

    assert result.returncode == 0, result.stderr
    assert not database_file.exists()
    for table in ("reviewable_course", "reviewer", "course_review"):
        assert f"CREATE TABLE {table} (" in result.stdout
    assert "CONSTRAINT reviewer_fkey FOREIGN KEY(reviewer) REFERENCES reviewer (id)" in result.stdout
    assert "CONSTRAINT reviewable_course_fkey FOREIGN KEY(course) REFERENCES reviewable_course (id)" in result.stdout


def test_alembic_migrated_schema_accepts_orm_writes(migrated_database_url: str) -> None:
    engine = build_engine(migrated_database_url)
    with Session(engine) as session:
        course = ReviewableCourseRepository(session).save(ReviewableCourse(uuid4()))
        reviewer = ReviewerRepository(session).save(Reviewer("username"))
        reviews = CourseReviewRepository(session)
        saved = reviews.save(
            CourseReview.create(course=course.local_id, reviewer=reviewer.local_id, rating=4.5, comment="c")
        )
        session.commit()

        listed = reviews.list_course_reviews(course.original_course_id)
        assert [dto.uuid for dto in listed] == [saved.uuid]
        assert listed[0].rating == 4.5
    engine.dispose()


def test_alembic_upgrade_head_accepts_database_url_containing_percent_sign(tmp_path: Path) -> None:
    # given: a literal '%' would be read as configparser interpolation unless env.py escapes it
    database_file = tmp_path / "100%25done.db"
    url = f"sqlite:///{database_file}"

    # when
    result = _alembic(url, "upgrade", "head")

    # then
    assert result.returncode == 0, result.stderr
    assert database_file.exists()
    engine = create_engine(url)
    assert set(inspect(engine).get_table_names()) == set(metadata.tables) | {"alembic_version"}
    engine.dispose()


def test_alembic_downgrade_base_drops_all_tables(migrated_database_url: str) -> None:
    result = _alembic(migrated_database_url, "downgrade", "base")
    assert result.returncode == 0, result.stderr

    engine = create_engine(migrated_database_url)
    assert set(inspect(engine).get_table_names()) == {"alembic_version"}
    engine.dispose()
