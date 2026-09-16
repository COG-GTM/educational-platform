"""``transactional`` unit of work: commit/rollback and the ``after_commit`` hook."""

from __future__ import annotations

import pytest
from sqlalchemy import text
from sqlalchemy.orm import Session, sessionmaker

from courses_py.infrastructure.persistence.database import transactional
from courses_py.tests.conftest import count, insert_teacher


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
