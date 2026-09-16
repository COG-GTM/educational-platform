"""FastAPI dependency wiring: DB session per request, JWT authentication, and handler construction."""

from __future__ import annotations

from collections.abc import Iterator
from dataclasses import dataclass
from typing import Annotated

from fastapi import Depends, Request
from fastapi.security import HTTPAuthorizationCredentials, HTTPBearer
from sqlalchemy.orm import Session, sessionmaker

from courses_py.application.course.approve import SendCourseToApproveCommandHandler
from courses_py.application.course.create import CourseFactory, CreateCourseCommandHandler, CurrentUserAsTeacher
from courses_py.application.course.publish import CourseTeacherChecker, PublishCourseCommandHandler
from courses_py.application.course.query import CourseByUUIDQueryHandler, ListCourseQueryHandler
from courses_py.application.exceptions import AccessDeniedException
from courses_py.application.security import Principal, StaticCurrentUser
from courses_py.infrastructure.messaging.broker import MessageBroker
from courses_py.infrastructure.messaging.publisher import BrokerIntegrationEventPublisher
from courses_py.infrastructure.persistence.database import transactional
from courses_py.infrastructure.persistence.repositories import SqlAlchemyCourseRepository, SqlAlchemyTeacherRepository
from courses_py.infrastructure.security.jwt import JwtTokenProvider


@dataclass
class AppContext:
    """Process-wide collaborators, stored on ``app.state.context``."""

    session_factory: sessionmaker[Session]
    broker: MessageBroker
    jwt: JwtTokenProvider


def get_context(request: Request) -> AppContext:
    context: AppContext = request.app.state.context
    return context


Context = Annotated[AppContext, Depends(get_context)]


def get_session(context: Context) -> Iterator[Session]:
    """``@Transactional`` per request: commit on success, rollback when the handler raises."""
    with transactional(context.session_factory) as session:
        yield session


DbSession = Annotated[Session, Depends(get_session)]

_bearer = HTTPBearer(auto_error=False)


def get_principal(
    context: Context,
    credentials: Annotated[HTTPAuthorizationCredentials | None, Depends(_bearer)],
) -> Principal:
    """``JwtTokenFilter`` + ``SecurityContextHolder``: resolves the caller from ``Authorization: Bearer <jwt>``."""
    if credentials is None:
        raise AccessDeniedException("Access Denied")
    return context.jwt.validate_token(credentials.credentials)


CurrentPrincipal = Annotated[Principal, Depends(get_principal)]


def create_course_handler(session: DbSession, principal: CurrentPrincipal) -> CreateCourseCommandHandler:
    current_user = StaticCurrentUser(principal)
    factory = CourseFactory(CurrentUserAsTeacher(SqlAlchemyTeacherRepository(session), current_user))
    return CreateCourseCommandHandler(SqlAlchemyCourseRepository(session), factory, current_user)


def publish_course_handler(session: DbSession, principal: CurrentPrincipal) -> PublishCourseCommandHandler:
    repository = SqlAlchemyCourseRepository(session)
    return PublishCourseCommandHandler(repository, CourseTeacherChecker(repository), StaticCurrentUser(principal))


def send_course_to_approve_handler(
    session: DbSession, principal: CurrentPrincipal, context: Context
) -> SendCourseToApproveCommandHandler:
    repository = SqlAlchemyCourseRepository(session)
    return SendCourseToApproveCommandHandler(
        repository,
        BrokerIntegrationEventPublisher(context.broker),
        CourseTeacherChecker(repository),
        StaticCurrentUser(principal),
    )


def list_course_handler(session: DbSession) -> ListCourseQueryHandler:
    return ListCourseQueryHandler(SqlAlchemyCourseRepository(session))


def course_by_uuid_handler(session: DbSession) -> CourseByUUIDQueryHandler:
    return CourseByUUIDQueryHandler(SqlAlchemyCourseRepository(session))
