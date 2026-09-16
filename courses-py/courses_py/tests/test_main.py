"""``python -m courses_py.main``: ``COURSES_HOST`` / ``COURSES_PORT`` / ``COURSES_RELOAD`` -> uvicorn wiring."""

from __future__ import annotations

from configparser import ConfigParser
from pathlib import Path

import pytest
import uvicorn
from alembic.util.exc import CommandError
from sqlalchemy import create_engine, inspect

from courses_py import main
from courses_py.config import Settings
from courses_py.infrastructure.persistence.orm import metadata

PROJECT_ROOT = Path(__file__).resolve().parents[2]
LIQUIBASE_TABLES = {"teacher", "course", "curriculum_item", "question"}


class _RecordedRun:
    def __init__(self) -> None:
        self.calls: list[tuple[tuple[object, ...], dict[str, object]]] = []

    def __call__(self, *args: object, **kwargs: object) -> None:
        self.calls.append((args, kwargs))


@pytest.fixture
def run(monkeypatch: pytest.MonkeyPatch) -> _RecordedRun:
    for name in (
        "COURSES_HOST",
        "COURSES_PORT",
        "COURSES_RELOAD",
        "COURSES_RUN_MIGRATIONS",
        "COURSES_DATABASE_URL",
        "COURSES_MIGRATIONS_DIR",
    ):
        monkeypatch.delenv(name, raising=False)
    recorded = _RecordedRun()
    monkeypatch.setattr(uvicorn, "run", recorded)
    return recorded


def test_main_noVariables_servesAppOnAllInterfacesPort8081WithoutReload(run: _RecordedRun) -> None:
    # when
    main.main()

    # then
    assert run.calls == [(("courses_py.api.app:app",), {"host": "0.0.0.0", "port": 8081, "reload": False})]


def test_main_hostAndPortVariables_overrideDefaults(run: _RecordedRun, monkeypatch: pytest.MonkeyPatch) -> None:
    # given
    monkeypatch.setenv("COURSES_HOST", "127.0.0.1")
    monkeypatch.setenv("COURSES_PORT", "9000")

    # when
    main.main()

    # then
    [(_, kwargs)] = run.calls
    assert (kwargs["host"], kwargs["port"]) == ("127.0.0.1", 9000)
    assert isinstance(kwargs["port"], int)


@pytest.mark.parametrize(
    ("value", "expected"),
    [("true", True), ("True", False), ("1", False), ("false", False), ("", False)],
)
def test_main_reloadVariable_enabledOnlyForLowercaseTrue(
    run: _RecordedRun, monkeypatch: pytest.MonkeyPatch, value: str, expected: bool
) -> None:
    # given
    monkeypatch.setenv("COURSES_RELOAD", value)

    # when
    main.main()

    # then
    [(_, kwargs)] = run.calls
    assert kwargs["reload"] is expected


def test_main_runMigrations_createsStandaloneSchemaBeforeServing(
    run: _RecordedRun, monkeypatch: pytest.MonkeyPatch, tmp_path: Path
) -> None:
    # given
    db_url = f"sqlite:///{tmp_path / 'standalone.db'}"
    monkeypatch.setenv("COURSES_DATABASE_URL", db_url)
    monkeypatch.setenv("COURSES_RUN_MIGRATIONS", "true")
    monkeypatch.setenv("COURSES_MIGRATIONS_DIR", str(PROJECT_ROOT / "migrations"))

    # when
    main.main()

    # then
    assert {"teacher", "course", "curriculum_item", "question", "alembic_version"} <= set(
        inspect(create_engine(db_url)).get_table_names()
    )
    assert len(run.calls) == 1


def test_main_runMigrationsUnset_doesNotTouchDatabase(
    run: _RecordedRun, monkeypatch: pytest.MonkeyPatch, tmp_path: Path
) -> None:
    # given
    db_file = tmp_path / "untouched.db"
    monkeypatch.setenv("COURSES_DATABASE_URL", f"sqlite:///{db_file}")

    # when
    main.main()

    # then
    assert not db_file.exists()
    assert len(run.calls) == 1


def test_main_runMigrationsAgainstSharedDatabase_leavesLiquibaseTablesAndRowsUntouched(
    run: _RecordedRun, monkeypatch: pytest.MonkeyPatch, tmp_path: Path
) -> None:
    # given
    db_url = f"sqlite:///{tmp_path / 'shared.db'}"
    engine = create_engine(db_url)
    metadata.create_all(engine)  # stands in for Liquibase having created the schema
    with engine.begin() as connection:
        connection.exec_driver_sql("insert into teacher (username) values ('existing')")
    monkeypatch.setenv("COURSES_DATABASE_URL", db_url)
    monkeypatch.setenv("COURSES_RUN_MIGRATIONS", "true")
    monkeypatch.setenv("COURSES_MIGRATIONS_DIR", str(PROJECT_ROOT / "migrations"))

    # when
    main.main()

    # then
    with engine.connect() as connection:
        assert connection.exec_driver_sql("select count(*) from teacher").scalar_one() == 1
    assert "alembic_version" in inspect(engine).get_table_names()
    assert len(run.calls) == 1


def test_main_runMigrationsTwice_idempotentAndStillServes(
    run: _RecordedRun, monkeypatch: pytest.MonkeyPatch, tmp_path: Path
) -> None:
    # given
    db_url = f"sqlite:///{tmp_path / 'restarted.db'}"
    monkeypatch.setenv("COURSES_DATABASE_URL", db_url)
    monkeypatch.setenv("COURSES_RUN_MIGRATIONS", "true")
    monkeypatch.setenv("COURSES_MIGRATIONS_DIR", str(PROJECT_ROOT / "migrations"))

    # when
    main.main()
    main.main()

    # then
    with create_engine(db_url).connect() as connection:
        assert connection.exec_driver_sql("select count(*) from alembic_version").scalar_one() == 1
    assert len(run.calls) == 2


def test_main_runMigrationsFail_serverNotStarted(
    run: _RecordedRun, monkeypatch: pytest.MonkeyPatch, tmp_path: Path
) -> None:
    # given
    monkeypatch.setenv("COURSES_DATABASE_URL", f"sqlite:///{tmp_path / 'never-served.db'}")
    monkeypatch.setenv("COURSES_RUN_MIGRATIONS", "true")
    monkeypatch.setenv("COURSES_MIGRATIONS_DIR", str(tmp_path / "no-such-migrations"))

    # when / then
    with pytest.raises(CommandError):
        main.main()
    assert run.calls == []


def test_main_runMigrationsDefaultDir_resolvedRelativeToWorkingDirectory(
    run: _RecordedRun, monkeypatch: pytest.MonkeyPatch, tmp_path: Path
) -> None:
    # given: the Docker CMD runs from the project root, where ./migrations is the Alembic script location
    monkeypatch.chdir(PROJECT_ROOT)
    monkeypatch.delenv("COURSES_MIGRATIONS_DIR", raising=False)
    db_url = f"sqlite:///{tmp_path / 'standalone.db'}"
    monkeypatch.setenv("COURSES_DATABASE_URL", db_url)
    monkeypatch.setenv("COURSES_RUN_MIGRATIONS", "true")

    # when
    main.main()

    # then
    assert "course" in inspect(create_engine(db_url)).get_table_names()
    assert len(run.calls) == 1


def test_main_nonNumericPort_valueErrorBeforeServerStarts(run: _RecordedRun, monkeypatch: pytest.MonkeyPatch) -> None:
    # given
    monkeypatch.setenv("COURSES_PORT", "eight-thousand")

    # when / then
    with pytest.raises(ValueError):
        main.main()
    assert run.calls == []


def test_upgradeDatabase_settingsUrl_migratedWithoutDatabaseUrlVariable(
    monkeypatch: pytest.MonkeyPatch, tmp_path: Path
) -> None:
    # given: Settings built in code, not from the environment (env.py only reads COURSES_DATABASE_URL when set)
    monkeypatch.delenv("COURSES_DATABASE_URL", raising=False)
    db_file = tmp_path / "from-settings.db"
    config = Settings(
        database_url=f"sqlite:///{db_file}",
        jwt_secret_key="secret-key",
        broker="memory",
        rabbitmq_url="amqp://guest:guest@localhost:5672/%2F",
        migrations_dir=str(PROJECT_ROOT / "migrations"),
    )

    # when
    main.upgrade_database(config)

    # then
    assert db_file.exists()
    assert LIQUIBASE_TABLES | {"alembic_version"} <= set(inspect(create_engine(config.database_url)).get_table_names())


def test_main_defaultMigrationsDir_resolvesRelativeToWorkingDirectoryLikeDockerImage(
    run: _RecordedRun, monkeypatch: pytest.MonkeyPatch, tmp_path: Path
) -> None:
    # given: Dockerfile runs `python -m courses_py.main` from WORKDIR /app with ./migrations copied next to the package
    monkeypatch.chdir(PROJECT_ROOT)
    db_url = f"sqlite:///{tmp_path / 'docker-like.db'}"
    monkeypatch.setenv("COURSES_DATABASE_URL", db_url)
    monkeypatch.setenv("COURSES_RUN_MIGRATIONS", "true")

    # when
    main.main()

    # then
    assert LIQUIBASE_TABLES <= set(inspect(create_engine(db_url)).get_table_names())
    assert len(run.calls) == 1


def test_defaultMigrationsDir_matchesAlembicIniScriptLocation(monkeypatch: pytest.MonkeyPatch) -> None:
    # given
    monkeypatch.delenv("COURSES_MIGRATIONS_DIR", raising=False)
    alembic_ini = ConfigParser()
    alembic_ini.read(PROJECT_ROOT / "alembic.ini")

    # when
    settings = Settings.from_env()

    # then: `alembic upgrade head` and COURSES_RUN_MIGRATIONS=true use the same revisions
    assert settings.migrations_dir == alembic_ini.get("alembic", "script_location") == "migrations"
    assert (PROJECT_ROOT / settings.migrations_dir / "versions").is_dir()


def test_main_defaultDatabaseUrl_createsCoursesDbInWorkingDirectory(
    run: _RecordedRun, monkeypatch: pytest.MonkeyPatch, tmp_path: Path
) -> None:
    # given: README's standalone default `sqlite:///./courses.db` + COURSES_RUN_MIGRATIONS=true
    monkeypatch.chdir(tmp_path)
    monkeypatch.setenv("COURSES_RUN_MIGRATIONS", "true")
    monkeypatch.setenv("COURSES_MIGRATIONS_DIR", str(PROJECT_ROOT / "migrations"))

    # when
    main.main()

    # then
    db_file = tmp_path / "courses.db"
    assert db_file.exists()
    assert LIQUIBASE_TABLES <= set(inspect(create_engine(f"sqlite:///{db_file}")).get_table_names())
    assert len(run.calls) == 1
