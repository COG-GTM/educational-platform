"""Engine/session-factory construction and the ``transactional`` unit of work (commit/rollback, ``after_commit``)."""

from __future__ import annotations

import threading
from pathlib import Path

import pytest
from sqlalchemy import Engine, text
from sqlalchemy.orm import Session, sessionmaker
from sqlalchemy.pool import StaticPool

from courses_py.infrastructure.persistence import database
from courses_py.infrastructure.persistence.database import build_engine, build_session_factory, transactional
from courses_py.tests.conftest import count, insert_teacher


class TestBuildEngine:
    @pytest.mark.parametrize("url", ["sqlite://", "sqlite:///:memory:"])
    def test_inMemorySqlite_singleSharedConnection(self, url: str) -> None:
        # given
        engine = build_engine(url)

        # when: two independent connections see the same in-memory database
        with engine.connect() as first:
            first.execute(text("CREATE TABLE t (id INTEGER)"))
            first.commit()
        with engine.connect() as second:
            rows = second.execute(text("SELECT COUNT(*) FROM t")).scalar_one()

        # then
        assert isinstance(engine.pool, StaticPool)
        assert rows == 0
        engine.dispose()

    def test_fileSqlite_notPinnedToStaticPool(self, tmp_path: Path) -> None:
        # given
        engine = build_engine(f"sqlite:///{tmp_path / 'courses.db'}")

        # then
        assert not isinstance(engine.pool, StaticPool)
        assert engine.url.database == str(tmp_path / "courses.db")
        engine.dispose()

    def test_postgresUrl_noSqliteOnlyConnectArgsOrPool(self, monkeypatch: pytest.MonkeyPatch) -> None:
        # given: the Docker image targets PostgreSQL; psycopg rejects sqlite's check_same_thread argument
        recorded: list[tuple[str, dict[str, object]]] = []
        sentinel = build_engine("sqlite://")

        def fake_create_engine(url: str, **kwargs: object) -> Engine:
            recorded.append((url, kwargs))
            return sentinel

        monkeypatch.setattr(database, "create_engine", fake_create_engine)
        url = "postgresql+psycopg://user:pw@db/courses"

        # when
        engine = build_engine(url)

        # then
        assert engine is sentinel
        assert recorded == [(url, {"connect_args": {}})]
        sentinel.dispose()

    @pytest.mark.parametrize("url", ["sqlite://", "sqlite:///{tmp}/courses.db"])
    def test_sqlite_connectionUsableFromAnotherThread(self, url: str, tmp_path: Path) -> None:
        # given: FastAPI runs sync endpoints on a thread pool, so the sqlite same-thread check must be off
        engine = build_engine(url.format(tmp=tmp_path))
        with engine.connect() as setup:
            setup.execute(text("CREATE TABLE t (id INTEGER)"))
            setup.commit()
        connection = engine.raw_connection()
        errors: list[Exception] = []

        def use_from_other_thread() -> None:
            try:
                connection.cursor().execute("SELECT COUNT(*) FROM t")
            except Exception as e:
                errors.append(e)

        # when
        worker = threading.Thread(target=use_from_other_thread)
        worker.start()
        worker.join()
        connection.close()

        # then
        assert errors == []
        engine.dispose()


class TestBuildSessionFactory:
    def test_sessionConfiguredForAfterCommitPublishing(self) -> None:
        # given
        engine = build_engine("sqlite://")

        # when
        sut = build_session_factory(engine)

        # then: objects stay readable after commit (the after-commit publisher reads them) and nothing is
        # flushed behind the repository's back
        with sut() as session:
            assert session.bind is engine
            assert session.autoflush is False
            assert session.expire_on_commit is False
        engine.dispose()

    def test_sessionsAreIndependentUnitsOfWork(self) -> None:
        # given
        engine = build_engine("sqlite://")
        sut = build_session_factory(engine)

        # when / then
        with sut() as first, sut() as second:
            assert first is not second
        engine.dispose()


def test_transactional_success_committedThenAfterCommitCalledWithSession(
    session_factory: sessionmaker[Session],
) -> None:
    seen: list[tuple[Session, int]] = []

    def after_commit(session: Session) -> None:
        seen.append((session, count(session, "teacher")))

    with transactional(session_factory, after_commit) as session:
        insert_teacher(session, "teacher")
        assert seen == []

    assert len(seen) == 1
    assert seen[0][0] is session and seen[0][1] == 1
    with session_factory() as check:
        assert count(check, "teacher") == 1


def test_transactional_handlerRaises_rolledBackAndAfterCommitSkipped(session_factory: sessionmaker[Session]) -> None:
    calls: list[Session] = []

    with pytest.raises(RuntimeError, match="boom"):
        with transactional(session_factory, calls.append) as session:
            insert_teacher(session, "teacher")
            raise RuntimeError("boom")

    assert calls == []
    with session_factory() as check:
        assert count(check, "teacher") == 0


def test_transactional_afterCommitRaises_dataStaysCommittedAndErrorPropagates(
    session_factory: sessionmaker[Session],
) -> None:
    def failing(session: Session) -> None:
        raise ConnectionError("broker down")

    with pytest.raises(ConnectionError, match="broker down"):
        with transactional(session_factory, failing) as session:
            insert_teacher(session, "teacher")

    with session_factory() as check:
        assert count(check, "teacher") == 1


def test_transactional_noAfterCommit_commits(session_factory: sessionmaker[Session]) -> None:
    with transactional(session_factory) as session:
        session.execute(text("INSERT INTO teacher (username) VALUES ('x')"))

    with session_factory() as check:
        assert count(check, "teacher") == 1
