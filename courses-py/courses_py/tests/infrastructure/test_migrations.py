"""The Alembic migration reproduces the Liquibase schema and is a no-op when the tables already exist."""

from __future__ import annotations

from pathlib import Path

import pytest
from alembic import command
from alembic.config import Config
from sqlalchemy import create_engine, inspect

from courses_py.infrastructure.persistence.orm import metadata

PROJECT_ROOT = Path(__file__).resolve().parents[3]
LIQUIBASE_TABLES = {"teacher", "course", "curriculum_item", "question"}


def _alembic_config(url: str) -> Config:
    config = Config(str(PROJECT_ROOT / "alembic.ini"))
    config.set_main_option("script_location", str(PROJECT_ROOT / "migrations"))
    config.set_main_option("sqlalchemy.url", url)
    return config


@pytest.fixture
def db_url(tmp_path: Path, monkeypatch: pytest.MonkeyPatch) -> str:
    monkeypatch.delenv("COURSES_DATABASE_URL", raising=False)
    return f"sqlite:///{tmp_path / 'courses.db'}"


def test_upgrade_emptyDatabase_createsLiquibaseSchema(db_url: str) -> None:
    command.upgrade(_alembic_config(db_url), "head")

    inspector = inspect(create_engine(db_url))
    assert LIQUIBASE_TABLES <= set(inspector.get_table_names())
    course_columns = {c["name"] for c in inspector.get_columns("course")}
    assert course_columns == {
        "id",
        "uuid",
        "name",
        "description",
        "publish_status",
        "approval_status",
        "rating",
        "number_of_students",
        "teacher",
    }
    assert {fk["name"] for fk in inspector.get_foreign_keys("curriculum_item")} == {"course_course_fkey"}
    assert {fk["name"] for fk in inspector.get_foreign_keys("question")} == {"quiz_fkey"}
    assert {fk["name"] for fk in inspector.get_foreign_keys("course")} == {"teacher_fkey"}


def test_upgrade_tablesManagedByLiquibase_doesNotTouchThem(db_url: str) -> None:
    engine = create_engine(db_url)
    metadata.create_all(engine)  # stands in for Liquibase having created the schema
    with engine.begin() as connection:
        connection.exec_driver_sql("insert into teacher (username) values ('existing')")

    command.upgrade(_alembic_config(db_url), "head")

    with engine.connect() as connection:
        assert connection.exec_driver_sql("select count(*) from teacher").scalar_one() == 1
    assert "alembic_version" in inspect(engine).get_table_names()


def test_downgrade_sharedDatabase_refusesToDropTables(db_url: str) -> None:
    config = _alembic_config(db_url)
    command.upgrade(config, "head")

    with pytest.raises(RuntimeError, match="managed by Liquibase"):
        command.downgrade(config, "base")

    assert LIQUIBASE_TABLES <= set(inspect(create_engine(db_url)).get_table_names())


def test_downgrade_standalone_dropsTables(db_url: str) -> None:
    config = _alembic_config(db_url)
    command.upgrade(config, "head")
    config.cmd_opts = type("Opts", (), {"x": ["standalone=true"]})()

    command.downgrade(config, "base")

    assert not LIQUIBASE_TABLES & set(inspect(create_engine(db_url)).get_table_names())


def test_upgrade_coursesDatabaseUrlSet_targetsThatDatabaseInsteadOfIniUrl(
    db_url: str, tmp_path: Path, monkeypatch: pytest.MonkeyPatch
) -> None:
    # given
    env_url = f"sqlite:///{tmp_path / 'from-env.db'}"
    monkeypatch.setenv("COURSES_DATABASE_URL", env_url)

    # when
    command.upgrade(_alembic_config(db_url), "head")

    # then
    assert LIQUIBASE_TABLES <= set(inspect(create_engine(env_url)).get_table_names())
    assert not (tmp_path / "courses.db").exists()


def test_upgrade_coursesDatabaseUrlEmpty_fallsBackToIniUrl(db_url: str, monkeypatch: pytest.MonkeyPatch) -> None:
    # given
    monkeypatch.setenv("COURSES_DATABASE_URL", "")

    # when
    command.upgrade(_alembic_config(db_url), "head")

    # then
    assert LIQUIBASE_TABLES <= set(inspect(create_engine(db_url)).get_table_names())
