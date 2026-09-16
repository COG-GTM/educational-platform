"""Ports of the ``*CommandHandlerTest`` classes, run against the SQLAlchemy repositories on an in-memory SQLite DB."""

from __future__ import annotations

from uuid import uuid4

import pytest
from pydantic import ValidationError
from sqlalchemy.orm import Session

from courses_py.application.course.approve import (
    ApproveCourseCommand,
    ApproveCourseCommandHandler,
    SendCourseToApproveCommand,
    SendCourseToApproveCommandHandler,
)
from courses_py.application.course.create import (
    CourseFactory,
    CreateCourseCommand,
    CreateCourseCommandHandler,
    CurrentUserAsTeacher,
)
from courses_py.application.course.number_of_students import (
    IncreaseNumberOfStudentsCommand,
    IncreaseNumberOfStudentsCommandHandler,
)
from courses_py.application.course.publish import (
    CourseTeacherChecker,
    PublishCourseCommand,
    PublishCourseCommandHandler,
)
from courses_py.application.course.query import (
    CourseByUUIDQuery,
    CourseByUUIDQueryHandler,
    ListCourseQuery,
    ListCourseQueryHandler,
)
from courses_py.application.course.rating import UpdateCourseRatingCommand, UpdateCourseRatingCommandHandler
from courses_py.application.exceptions import (
    AccessDeniedException,
    RelatedResourceIsNotResolvedException,
    ResourceNotFoundException,
)
from courses_py.application.security import Principal, StaticCurrentUser
from courses_py.application.teacher.create import CreateTeacherCommand, CreateTeacherCommandHandler
from courses_py.domain.enums import ApprovalStatus, PublishStatus
from courses_py.domain.exceptions import CourseCannotBePublishedException
from courses_py.infrastructure.messaging import topics
from courses_py.infrastructure.messaging.broker import InMemoryMessageBroker
from courses_py.infrastructure.messaging.publisher import BrokerIntegrationEventPublisher
from courses_py.infrastructure.persistence.repositories import SqlAlchemyCourseRepository, SqlAlchemyTeacherRepository
from courses_py.tests.conftest import TEACHER_USERNAME, insert_approved_course, insert_teacher


@pytest.fixture
def course_repository(session: Session) -> SqlAlchemyCourseRepository:
    return SqlAlchemyCourseRepository(session)


@pytest.fixture
def teacher_repository(session: Session) -> SqlAlchemyTeacherRepository:
    return SqlAlchemyTeacherRepository(session)


def _create_handler(
    course_repository: SqlAlchemyCourseRepository,
    teacher_repository: SqlAlchemyTeacherRepository,
    user: StaticCurrentUser,
) -> CreateCourseCommandHandler:
    factory = CourseFactory(CurrentUserAsTeacher(teacher_repository, user))
    return CreateCourseCommandHandler(course_repository, factory, user)


class TestCreateCourseCommandHandler:
    def test_handle_validCommand_courseSaved(
        self,
        session: Session,
        course_repository: SqlAlchemyCourseRepository,
        teacher_repository: SqlAlchemyTeacherRepository,
        teacher_user: StaticCurrentUser,
    ) -> None:
        teacher = insert_teacher(session)
        handler = _create_handler(course_repository, teacher_repository, teacher_user)

        uuid = handler.handle(CreateCourseCommand(name="name", description="description"))

        course = course_repository.find_by_uuid(uuid)
        assert course is not None
        assert course.name == "name"
        assert course.teacher == teacher.id
        assert course.approval_status == ApprovalStatus.NOT_SENT_FOR_APPROVAL

    def test_handle_teacherNotFound_relatedResourceIsNotResolvedException(
        self,
        course_repository: SqlAlchemyCourseRepository,
        teacher_repository: SqlAlchemyTeacherRepository,
        teacher_user: StaticCurrentUser,
    ) -> None:
        handler = _create_handler(course_repository, teacher_repository, teacher_user)

        with pytest.raises(RelatedResourceIsNotResolvedException):
            handler.handle(CreateCourseCommand(name="name", description="description"))

    def test_handle_notTeacher_accessDenied(
        self,
        session: Session,
        course_repository: SqlAlchemyCourseRepository,
        teacher_repository: SqlAlchemyTeacherRepository,
    ) -> None:
        insert_teacher(session)
        student = StaticCurrentUser(Principal(TEACHER_USERNAME, frozenset({"ROLE_STUDENT"})))
        handler = _create_handler(course_repository, teacher_repository, student)

        with pytest.raises(AccessDeniedException):
            handler.handle(CreateCourseCommand(name="name", description="description"))

    @pytest.mark.parametrize("field", ["name", "description"])
    def test_command_blankField_validationError(self, field: str) -> None:
        with pytest.raises(ValidationError):
            CreateCourseCommand.model_validate({"name": "name", "description": "description", field: "  "})


class TestPublishCourseCommandHandler:
    def _handler(self, repository: SqlAlchemyCourseRepository, user: StaticCurrentUser) -> PublishCourseCommandHandler:
        return PublishCourseCommandHandler(repository, CourseTeacherChecker(repository), user)

    def test_handle_approvedCourse_published(
        self, session: Session, course_repository: SqlAlchemyCourseRepository, teacher_user: StaticCurrentUser
    ) -> None:
        course = insert_approved_course(session, insert_teacher(session))

        self._handler(course_repository, teacher_user).handle(PublishCourseCommand(uuid=course.uuid))

        session.expire_all()
        published = course_repository.find_by_uuid(course.uuid)
        assert published is not None and published.publish_status == PublishStatus.PUBLISHED

    def test_handle_notApprovedCourse_courseCannotBePublishedException(
        self, session: Session, course_repository: SqlAlchemyCourseRepository, teacher_user: StaticCurrentUser
    ) -> None:
        teacher = insert_teacher(session)
        assert teacher.id is not None
        uuid = _create_handler(course_repository, SqlAlchemyTeacherRepository(session), teacher_user).handle(
            CreateCourseCommand(name="name", description="description")
        )

        with pytest.raises(CourseCannotBePublishedException):
            self._handler(course_repository, teacher_user).handle(PublishCourseCommand(uuid=uuid))

    def test_handle_courseNotFound_resourceNotFoundException(
        self, session: Session, course_repository: SqlAlchemyCourseRepository, teacher_user: StaticCurrentUser
    ) -> None:
        # ownership check fails first for an unknown course (as in Java: the @PreAuthorize expression is evaluated
        # before the handler body) -> AccessDenied; an owned-but-missing course cannot exist.
        with pytest.raises(AccessDeniedException):
            self._handler(course_repository, teacher_user).handle(PublishCourseCommand(uuid=uuid4()))

    def test_handle_notOwner_accessDenied(
        self, session: Session, course_repository: SqlAlchemyCourseRepository
    ) -> None:
        course = insert_approved_course(session, insert_teacher(session))
        insert_teacher(session, "other")
        other = StaticCurrentUser(Principal("other", frozenset({"ROLE_TEACHER"})))

        with pytest.raises(AccessDeniedException):
            self._handler(course_repository, other).handle(PublishCourseCommand(uuid=course.uuid))


class TestApproveCourseCommandHandler:
    def test_handle_existingCourse_approved(
        self, session: Session, course_repository: SqlAlchemyCourseRepository, teacher_user: StaticCurrentUser
    ) -> None:
        insert_teacher(session)
        uuid = _create_handler(course_repository, SqlAlchemyTeacherRepository(session), teacher_user).handle(
            CreateCourseCommand(name="name", description="description")
        )

        ApproveCourseCommandHandler(course_repository).handle(ApproveCourseCommand(uuid=uuid))

        course = course_repository.find_by_uuid(uuid)
        assert course is not None and course.approval_status == ApprovalStatus.APPROVED

    def test_handle_courseNotFound_resourceNotFoundException(
        self, course_repository: SqlAlchemyCourseRepository
    ) -> None:
        with pytest.raises(ResourceNotFoundException):
            ApproveCourseCommandHandler(course_repository).handle(ApproveCourseCommand(uuid=uuid4()))


class TestSendCourseToApproveCommandHandler:
    def test_handle_draftCourse_waitingForApprovalAndEventPublished(
        self, session: Session, course_repository: SqlAlchemyCourseRepository, teacher_user: StaticCurrentUser
    ) -> None:
        insert_teacher(session)
        uuid = _create_handler(course_repository, SqlAlchemyTeacherRepository(session), teacher_user).handle(
            CreateCourseCommand(name="name", description="description")
        )
        broker = InMemoryMessageBroker()
        handler = SendCourseToApproveCommandHandler(
            course_repository,
            BrokerIntegrationEventPublisher(broker),
            CourseTeacherChecker(course_repository),
            teacher_user,
        )

        handler.handle(SendCourseToApproveCommand(uuid=uuid))

        course = course_repository.find_by_uuid(uuid)
        assert course is not None and course.approval_status == ApprovalStatus.WAITING_FOR_APPROVAL
        assert broker.published == [(topics.SEND_COURSE_TO_APPROVE, {"courseId": str(uuid)})]


class TestIncreaseNumberOfStudentsCommandHandler:
    def test_handle_existingCourse_incremented(
        self, session: Session, course_repository: SqlAlchemyCourseRepository
    ) -> None:
        course = insert_approved_course(session, insert_teacher(session))

        IncreaseNumberOfStudentsCommandHandler(course_repository).handle(
            IncreaseNumberOfStudentsCommand(uuid=course.uuid)
        )

        session.expire_all()
        updated = course_repository.find_by_uuid(course.uuid)
        assert updated is not None and updated.number_of_students.number == 1


class TestUpdateCourseRatingCommandHandler:
    def test_handle_existingCourse_ratingUpdated(
        self, session: Session, course_repository: SqlAlchemyCourseRepository
    ) -> None:
        course = insert_approved_course(session, insert_teacher(session))

        UpdateCourseRatingCommandHandler(course_repository).handle(
            UpdateCourseRatingCommand(uuid=course.uuid, rating=4.5)
        )

        session.expire_all()
        updated = course_repository.find_by_uuid(course.uuid)
        assert updated is not None and updated.rating.rating == 4.5


class TestCreateTeacherCommandHandler:
    def test_handle_validCommand_teacherSaved(self, teacher_repository: SqlAlchemyTeacherRepository) -> None:
        CreateTeacherCommandHandler(teacher_repository).handle(CreateTeacherCommand(username="username"))

        teacher = teacher_repository.find_by_username("username")
        assert teacher is not None and teacher.id is not None


class TestQueryHandlers:
    def test_list_and_byUuid(self, session: Session, course_repository: SqlAlchemyCourseRepository) -> None:
        course = insert_approved_course(session, insert_teacher(session))

        listed = ListCourseQueryHandler(course_repository).handle(ListCourseQuery())
        found = CourseByUUIDQueryHandler(course_repository).handle(CourseByUUIDQuery(uuid=course.uuid))
        missing = CourseByUUIDQueryHandler(course_repository).handle(CourseByUUIDQuery(uuid=uuid4()))

        assert [c.uuid for c in listed] == [course.uuid]
        assert found is not None and found.name == "course name" and found.curriculum_items == []
        assert missing is None
