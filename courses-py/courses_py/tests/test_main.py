"""``python -m courses_py.main``: ``COURSES_HOST`` / ``COURSES_PORT`` / ``COURSES_RELOAD`` -> uvicorn wiring."""

from __future__ import annotations

from pathlib import Path

import pytest
import uvicorn
from sqlalchemy import create_engine, inspect

from courses_py import main

PROJECT_ROOT = Path(__file__).resolve().parents[2]


class _RecordedRun:
    def __init__(self) -> None:
        self.calls: list[tuple[tuple[object, ...], dict[str, object]]] = []

    def __call__(self, *args: object, **kwargs: object) -> None:
        self.calls.append((args, kwargs))


@pytest.fixture
def run(monkeypatch: pytest.MonkeyPatch) -> _RecordedRun:
    for name in ("COURSES_HOST", "COURSES_PORT", "COURSES_RELOAD", "COURSES_RUN_MIGRATIONS", "COURSES_DATABASE_URL"):
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


def test_main_nonNumericPort_valueErrorBeforeServerStarts(run: _RecordedRun, monkeypatch: pytest.MonkeyPatch) -> None:
    # given
    monkeypatch.setenv("COURSES_PORT", "eight-thousand")

    # when / then
    with pytest.raises(ValueError):
        main.main()
    assert run.calls == []
