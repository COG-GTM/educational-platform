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
from courses_py.domain.course import Course
from courses_py.domain.enums import ApprovalStatus, PublishStatus
from courses_py.domain.exceptions import CourseAlreadyApprovedException, CourseCannotBePublishedException
from courses_py.domain.teacher import Teacher
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

    @pytest.mark.parametrize("field", ["name", "description"])
    def test_command_missingField_validationError(self, field: str) -> None:
        payload = {"name": "name", "description": "description"}
        del payload[field]

        with pytest.raises(ValidationError):
            CreateCourseCommand.model_validate(payload)

    def test_command_unknownCurriculumItemType_validationError(self) -> None:
        with pytest.raises(ValidationError):
            CreateCourseCommand.model_validate(
                {
                    "name": "n",
                    "description": "d",
                    "curriculum_items": [{"type": "Video", "title": "t", "description": "d"}],
                }
            )

    def test_command_blankQuestionContent_validationError(self) -> None:
        with pytest.raises(ValidationError):
            CreateCourseCommand.model_validate(
                {
                    "name": "n",
                    "description": "d",
                    "curriculum_items": [
                        {"type": "Quiz", "title": "t", "description": "d", "questions": [{"content": " "}]}
                    ],
                }
            )

    def test_command_isImmutable(self) -> None:
        command = CreateCourseCommand(name="name", description="description")

        with pytest.raises(ValidationError):
            command.name = "other"  # type: ignore[misc]

    def test_handle_withCurriculumItems_itemsPersistedWithCourse(
        self,
        session: Session,
        course_repository: SqlAlchemyCourseRepository,
        teacher_repository: SqlAlchemyTeacherRepository,
        teacher_user: StaticCurrentUser,
    ) -> None:
        insert_teacher(session)
        handler = _create_handler(course_repository, teacher_repository, teacher_user)
        command = CreateCourseCommand.model_validate(
            {
                "name": "name",
                "description": "description",
                "curriculum_items": [
                    {"type": "Lecture", "title": "l", "description": "d", "serial_number": 1, "text": "hello"},
                    {
                        "type": "Quiz",
                        "title": "q",
                        "description": "d",
                        "serial_number": 2,
                        "questions": [{"content": "?"}],
                    },
                ],
            }
        )

        uuid = handler.handle(command)

        session.expire_all()
        dto = course_repository.find_dto_by_uuid(uuid)
        assert dto is not None
        assert [item.type for item in dto.curriculum_items] == ["Lecture", "Quiz"]

    def test_handle_accessCheckedBeforeTeacherLookup(
        self,
        course_repository: SqlAlchemyCourseRepository,
        teacher_repository: SqlAlchemyTeacherRepository,
    ) -> None:
        # unknown user without TEACHER role: 403 (role check) wins over 400 (teacher not resolved), as in Java
        nobody = StaticCurrentUser(Principal("nobody", frozenset()))
        handler = _create_handler(course_repository, teacher_repository, nobody)

        with pytest.raises(AccessDeniedException):
            handler.handle(CreateCourseCommand(name="name", description="description"))


class TestCourseFactory:
    def test_createFrom_unsavedTeacher_relatedResourceIsNotResolvedException(
        self, teacher_user: StaticCurrentUser
    ) -> None:
        class UnsavedTeacherRepository:
            def save(self, teacher: Teacher) -> Teacher:
                return teacher

            def find_by_username(self, username: str) -> Teacher | None:
                return Teacher(CreateTeacherCommand(username=username))

        factory = CourseFactory(CurrentUserAsTeacher(UnsavedTeacherRepository(), teacher_user))

        with pytest.raises(RelatedResourceIsNotResolvedException, match="is not persisted"):
            factory.create_from(CreateCourseCommand(name="name", description="description"))

    def test_createFrom_persistedTeacher_courseOwnedByTeacher(
        self, session: Session, teacher_repository: SqlAlchemyTeacherRepository, teacher_user: StaticCurrentUser
    ) -> None:
        teacher = insert_teacher(session)
        factory = CourseFactory(CurrentUserAsTeacher(teacher_repository, teacher_user))

        course = factory.create_from(CreateCourseCommand(name="name", description="description"))

        assert isinstance(course, Course)
        assert course.teacher == teacher.id
        assert course.id is None


class TestCourseTeacherChecker:
    def test_hasAccess_ownerTrueOthersFalse(
        self, session: Session, course_repository: SqlAlchemyCourseRepository
    ) -> None:
        course = insert_approved_course(session, insert_teacher(session))
        insert_teacher(session, "other")
        checker = CourseTeacherChecker(course_repository)

        assert checker.has_access(Principal(TEACHER_USERNAME), course.uuid)
        assert not checker.has_access(Principal("other"), course.uuid)
        assert not checker.has_access(Principal("nobody"), course.uuid)
        assert not checker.has_access(Principal(TEACHER_USERNAME), uuid4())

    def test_checkAccess_notOwner_accessDeniedWithJavaMessage(
        self, session: Session, course_repository: SqlAlchemyCourseRepository
    ) -> None:
        course = insert_approved_course(session, insert_teacher(session))

        with pytest.raises(AccessDeniedException, match="Access Denied"):
            CourseTeacherChecker(course_repository).check_access(Principal("other"), course.uuid)


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

    def test_handle_ownerWithoutTeacherRole_accessDenied(
        self, session: Session, course_repository: SqlAlchemyCourseRepository
    ) -> None:
        course = insert_approved_course(session, insert_teacher(session))
        student = StaticCurrentUser(Principal(TEACHER_USERNAME, frozenset({"ROLE_STUDENT"})))

        with pytest.raises(AccessDeniedException):
            self._handler(course_repository, student).handle(PublishCourseCommand(uuid=course.uuid))
        assert course.publish_status == PublishStatus.DRAFT

    def test_handle_alreadyPublished_staysPublished(
        self, session: Session, course_repository: SqlAlchemyCourseRepository, teacher_user: StaticCurrentUser
    ) -> None:
        course = insert_approved_course(session, insert_teacher(session))
        handler = self._handler(course_repository, teacher_user)

        handler.handle(PublishCourseCommand(uuid=course.uuid))
        handler.handle(PublishCourseCommand(uuid=course.uuid))

        session.expire_all()
        published = course_repository.find_by_uuid(course.uuid)
        assert published is not None and published.publish_status == PublishStatus.PUBLISHED


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
    def _handler(
        self, repository: SqlAlchemyCourseRepository, broker: InMemoryMessageBroker, user: StaticCurrentUser
    ) -> SendCourseToApproveCommandHandler:
        return SendCourseToApproveCommandHandler(
            repository, BrokerIntegrationEventPublisher(broker), CourseTeacherChecker(repository), user
        )

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

    def test_handle_alreadyApproved_courseAlreadyApprovedExceptionAndNoEvent(
        self, session: Session, course_repository: SqlAlchemyCourseRepository, teacher_user: StaticCurrentUser
    ) -> None:
        course = insert_approved_course(session, insert_teacher(session))
        broker = InMemoryMessageBroker()

        with pytest.raises(CourseAlreadyApprovedException):
            self._handler(course_repository, broker, teacher_user).handle(SendCourseToApproveCommand(uuid=course.uuid))

        assert course.approval_status == ApprovalStatus.APPROVED
        assert broker.published == []

    def test_handle_notOwner_accessDeniedAndNoEvent(
        self, session: Session, course_repository: SqlAlchemyCourseRepository
    ) -> None:
        course = insert_approved_course(session, insert_teacher(session))
        insert_teacher(session, "other")
        broker = InMemoryMessageBroker()
        other = StaticCurrentUser(Principal("other", frozenset({"ROLE_TEACHER"})))

        with pytest.raises(AccessDeniedException):
            self._handler(course_repository, broker, other).handle(SendCourseToApproveCommand(uuid=course.uuid))

        assert broker.published == []

    def test_handle_notTeacher_accessDeniedAndNoEvent(
        self, session: Session, course_repository: SqlAlchemyCourseRepository
    ) -> None:
        course = insert_approved_course(session, insert_teacher(session))
        broker = InMemoryMessageBroker()
        student = StaticCurrentUser(Principal(TEACHER_USERNAME, frozenset({"ROLE_STUDENT"})))

        with pytest.raises(AccessDeniedException):
            self._handler(course_repository, broker, student).handle(SendCourseToApproveCommand(uuid=course.uuid))

        assert broker.published == []

    def test_handle_unknownCourse_accessDenied(
        self, course_repository: SqlAlchemyCourseRepository, teacher_user: StaticCurrentUser
    ) -> None:
        broker = InMemoryMessageBroker()

        with pytest.raises(AccessDeniedException):
            self._handler(course_repository, broker, teacher_user).handle(SendCourseToApproveCommand(uuid=uuid4()))

        assert broker.published == []

    def test_handle_declinedCourse_canBeResent(
        self, session: Session, course_repository: SqlAlchemyCourseRepository, teacher_user: StaticCurrentUser
    ) -> None:
        teacher = insert_teacher(session)
        assert teacher.id is not None
        course = Course(CreateCourseCommand(name="name", description="description"), teacher.id)
        course.decline()
        course_repository.save(course)
        broker = InMemoryMessageBroker()

        self._handler(course_repository, broker, teacher_user).handle(SendCourseToApproveCommand(uuid=course.uuid))

        assert course.approval_status == ApprovalStatus.WAITING_FOR_APPROVAL
        assert broker.published == [(topics.SEND_COURSE_TO_APPROVE, {"courseId": str(course.uuid)})]


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

    def test_handle_twice_incrementedTwice(
        self, session: Session, course_repository: SqlAlchemyCourseRepository
    ) -> None:
        course = insert_approved_course(session, insert_teacher(session))
        handler = IncreaseNumberOfStudentsCommandHandler(course_repository)

        handler.handle(IncreaseNumberOfStudentsCommand(uuid=course.uuid))
        handler.handle(IncreaseNumberOfStudentsCommand(uuid=course.uuid))

        session.expire_all()
        updated = course_repository.find_by_uuid(course.uuid)
        assert updated is not None and updated.number_of_students.number == 2

    def test_handle_courseNotFound_resourceNotFoundException(
        self, course_repository: SqlAlchemyCourseRepository
    ) -> None:
        uuid = uuid4()

        with pytest.raises(ResourceNotFoundException, match=f"Course with uuid: {uuid} not found"):
            IncreaseNumberOfStudentsCommandHandler(course_repository).handle(IncreaseNumberOfStudentsCommand(uuid=uuid))


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

    def test_handle_courseNotFound_resourceNotFoundException(
        self, course_repository: SqlAlchemyCourseRepository
    ) -> None:
        with pytest.raises(ResourceNotFoundException):
            UpdateCourseRatingCommandHandler(course_repository).handle(
                UpdateCourseRatingCommand(uuid=uuid4(), rating=1.0)
            )

    def test_handle_zeroRating_replacesPreviousRating(
        self, session: Session, course_repository: SqlAlchemyCourseRepository
    ) -> None:
        course = insert_approved_course(session, insert_teacher(session))
        handler = UpdateCourseRatingCommandHandler(course_repository)

        handler.handle(UpdateCourseRatingCommand(uuid=course.uuid, rating=4.5))
        handler.handle(UpdateCourseRatingCommand(uuid=course.uuid, rating=0))

        session.expire_all()
        updated = course_repository.find_by_uuid(course.uuid)
        assert updated is not None and updated.rating.rating == 0


class TestCreateTeacherCommandHandler:
    def test_handle_validCommand_teacherSaved(self, teacher_repository: SqlAlchemyTeacherRepository) -> None:
        CreateTeacherCommandHandler(teacher_repository).handle(CreateTeacherCommand(username="username"))

        teacher = teacher_repository.find_by_username("username")
        assert teacher is not None and teacher.id is not None

    def test_findByUsername_unknown_none(self, teacher_repository: SqlAlchemyTeacherRepository) -> None:
        assert teacher_repository.find_by_username("nobody") is None


class TestQueryHandlers:
    def test_list_and_byUuid(self, session: Session, course_repository: SqlAlchemyCourseRepository) -> None:
        course = insert_approved_course(session, insert_teacher(session))

        listed = ListCourseQueryHandler(course_repository).handle(ListCourseQuery())
        found = CourseByUUIDQueryHandler(course_repository).handle(CourseByUUIDQuery(uuid=course.uuid))
        missing = CourseByUUIDQueryHandler(course_repository).handle(CourseByUUIDQuery(uuid=uuid4()))

        assert [c.uuid for c in listed] == [course.uuid]
        assert found is not None and found.name == "course name" and found.curriculum_items == []
        assert missing is None

    def test_list_empty_emptyList(self, course_repository: SqlAlchemyCourseRepository) -> None:
        assert ListCourseQueryHandler(course_repository).handle(ListCourseQuery()) == []

    def test_list_orderedByInsertionAndReflectsStudents(
        self, session: Session, course_repository: SqlAlchemyCourseRepository
    ) -> None:
        teacher = insert_teacher(session)
        first = insert_approved_course(session, teacher)
        second = insert_approved_course(session, teacher, uuid4())
        first.increase_number_of_students()
        course_repository.save(first)

        listed = ListCourseQueryHandler(course_repository).handle(ListCourseQuery())

        assert [(c.uuid, c.number_of_students) for c in listed] == [(first.uuid, 1), (second.uuid, 0)]
