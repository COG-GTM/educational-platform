"""SQLAlchemy engine/session factory and the per-unit-of-work session context (``@Transactional`` analogue)."""

from __future__ import annotations

from collections.abc import Callable, Iterator
from contextlib import contextmanager

from sqlalchemy import Engine, create_engine
from sqlalchemy.orm import Session, sessionmaker
from sqlalchemy.pool import StaticPool

from courses_py.config import settings


def build_engine(url: str = settings.database_url) -> Engine:
    connect_args: dict[str, object] = {}
    kwargs: dict[str, object] = {}
    if url.startswith("sqlite"):
        connect_args["check_same_thread"] = False
        if ":memory:" in url or url == "sqlite://":
            kwargs["poolclass"] = StaticPool
    return create_engine(url, connect_args=connect_args, **kwargs)


def build_session_factory(engine: Engine) -> sessionmaker[Session]:
    return sessionmaker(bind=engine, autoflush=False, expire_on_commit=False)


@contextmanager
def transactional(
    session_factory: sessionmaker[Session], after_commit: Callable[[Session], None] | None = None
) -> Iterator[Session]:
    """One session per unit of work: commit on success, rollback on error; ``after_commit`` runs only after a commit."""
    session = session_factory()
    try:
        yield session
        session.commit()
        if after_commit is not None:
            after_commit(session)
    except Exception:
        session.rollback()
        raise
    finally:
        session.close()
