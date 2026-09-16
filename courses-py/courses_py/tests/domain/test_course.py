"""Port of ``CourseTest.java``: the ``Course`` aggregate state machine."""

from __future__ import annotations

import pytest
from pydantic import BaseModel, ValidationError

from courses_py.application.course.create import CreateCourseCommand, CreateLectureCommand, CreateQuizCommand
from courses_py.application.teacher.create import CreateTeacherCommand
from courses_py.domain.course import Course, create_curriculum_item
from courses_py.domain.curriculum_item import Lecture, Quiz
from courses_py.domain.enums import ApprovalStatus, PublishStatus
from courses_py.domain.exceptions import CourseAlreadyApprovedException, CourseCannotBePublishedException
from courses_py.domain.teacher import Teacher
from courses_py.domain.value_objects import CourseRating, NumberOfStudents

TEACHER_ID = 1


def _course() -> Course:
    return Course(CreateCourseCommand(name="name", description="description"), TEACHER_ID)


def test_approve_approvedStatus() -> None:
    course = _course()

    course.approve()

    assert course.approval_status == ApprovalStatus.APPROVED


def test_decline_declinedStatus() -> None:
    course = _course()

    course.decline()

    assert course.approval_status == ApprovalStatus.DECLINED


def test_publish_approvedCourse_publishedStatus() -> None:
    course = _course()
    course.approve()

    course.publish()

    assert course.publish_status == PublishStatus.PUBLISHED


def test_publish_notApprovedCourse_courseCannotBePublishedException() -> None:
    course = _course()

    with pytest.raises(CourseCannotBePublishedException) as e:
        course.publish()

    assert str(e.value) == (
        f"Course with uuid = {course.uuid} cannot be published, course should be approved by admin at first"
    )
    assert course.publish_status == PublishStatus.DRAFT


@pytest.mark.parametrize("prepare", ["send_to_approve", "decline"])
def test_publish_waitingOrDeclinedCourse_courseCannotBePublishedException(prepare: str) -> None:
    course = _course()
    getattr(course, prepare)()

    with pytest.raises(CourseCannotBePublishedException) as e:
        course.publish()

    assert e.value.uuid == course.uuid
    assert course.publish_status == PublishStatus.DRAFT


def test_publish_archivedApprovedCourse_publishedAgain() -> None:
    course = _course()
    course.approve()
    course.archive()

    course.publish()

    assert course.publish_status == PublishStatus.PUBLISHED


def test_create_validCommand_createdCourse() -> None:
    course = _course()

    assert course.name == "name"
    assert course.description == "description"
    assert course.teacher == TEACHER_ID
    assert course.rating == CourseRating(0)
    assert course.number_of_students == NumberOfStudents(0)
    assert course.publish_status == PublishStatus.DRAFT
    assert course.approval_status == ApprovalStatus.NOT_SENT_FOR_APPROVAL
    assert course.uuid is not None
    assert course.to_identity() == course.uuid


def test_create_twoCourses_distinctUuidsAndNoIdUntilPersisted() -> None:
    first, second = _course(), _course()

    assert first.uuid != second.uuid
    assert first.id is None and second.id is None


def test_create_noCurriculumItems_emptyList() -> None:
    assert _course().curriculum_items == []
    assert (
        Course(CreateCourseCommand(name="n", description="d", curriculum_items=[]), TEACHER_ID).curriculum_items == []
    )


def test_create_withCurriculumItems_createdCourse() -> None:
    command = CreateCourseCommand.model_validate(
        {
            "name": "name",
            "description": "description",
            "curriculum_items": [
                {"type": "Lecture", "title": "lecture", "description": "d", "serial_number": 1, "text": "text"},
                {
                    "type": "Quiz",
                    "title": "quiz",
                    "description": "d",
                    "serial_number": 2,
                    "questions": [{"content": "q"}],
                },
            ],
        }
    )

    course = Course(command, TEACHER_ID)

    lecture, quiz = course.curriculum_items
    assert isinstance(lecture, Lecture) and lecture.content == "text" and lecture.course is course
    assert isinstance(quiz, Quiz) and [q.content for q in quiz.questions] == ["q"]


def test_createCurriculumItem_lectureWithoutText_noneContent() -> None:
    course = _course()

    lecture = create_curriculum_item(CreateLectureCommand(title="t", description="d", serial_number=1), course)

    assert isinstance(lecture, Lecture)
    assert lecture.content is None and lecture.serial_number == 1
    assert lecture.title == "t" and lecture.description == "d"
    assert lecture.course is course and lecture.id is None and lecture.uuid is not None


@pytest.mark.parametrize("command_type", [CreateLectureCommand, CreateQuizCommand])
def test_createCurriculumItemCommand_serialNumberRequired(command_type: type[BaseModel]) -> None:
    with pytest.raises(ValidationError, match="serial_number"):
        command_type.model_validate({"title": "t", "description": "d"})


def test_createCurriculumItem_quizWithoutQuestions_emptyQuestions() -> None:
    quiz = create_curriculum_item(CreateQuizCommand(title="t", description="d", serial_number=3), _course())

    assert isinstance(quiz, Quiz)
    assert quiz.questions == [] and quiz.serial_number == 3


def test_createCurriculumItem_quizQuestionsLinkedBackToQuiz() -> None:
    command = CreateQuizCommand.model_validate(
        {"title": "t", "description": "d", "serial_number": 2, "questions": [{"content": "a"}, {"content": "b"}]}
    )

    quiz = create_curriculum_item(command, _course())

    assert isinstance(quiz, Quiz)
    assert [q.content for q in quiz.questions] == ["a", "b"]
    assert all(q.quiz is quiz and q.id is None for q in quiz.questions)


def test_createCurriculumItem_unsupportedType_typeError() -> None:
    command = CreateLectureCommand.model_construct(type="Video", title="t", description="d", serial_number=1)

    with pytest.raises(TypeError, match="Unsupported curriculum item type: 'Video'"):
        create_curriculum_item(command, _course())


def test_curriculumItems_discriminatorsMatchJavaEntityNames() -> None:
    assert Lecture.DISCRIMINATOR == "Lecture"
    assert Quiz.DISCRIMINATOR == "Quiz"


def test_sendToApprove_courseAlreadyApproved_courseAlreadyApprovedException() -> None:
    course = _course()
    course.approve()

    with pytest.raises(CourseAlreadyApprovedException) as e:
        course.send_to_approve()

    assert str(e.value) == f"Course with uuid = {course.uuid} cannot be sent for approval, course was already approved"
    assert e.value.uuid == course.uuid
    assert course.approval_status == ApprovalStatus.APPROVED


def test_sendToApprove_waitingForApprovalStatus() -> None:
    course = _course()

    course.send_to_approve()

    assert course.approval_status == ApprovalStatus.WAITING_FOR_APPROVAL


@pytest.mark.parametrize("prepare", ["send_to_approve", "decline"])
def test_sendToApprove_waitingOrDeclinedCourse_canBeResent(prepare: str) -> None:
    course = _course()
    getattr(course, prepare)()

    course.send_to_approve()

    assert course.approval_status == ApprovalStatus.WAITING_FOR_APPROVAL


def test_archive_archivedStatus() -> None:
    course = _course()

    course.archive()

    assert course.publish_status == PublishStatus.ARCHIVED


def test_updateRating_ratingReplaced() -> None:
    course = _course()

    course.update_rating(4.5)

    assert course.rating == CourseRating(4.5)


def test_updateRating_zero_resetsRating() -> None:
    course = _course()
    course.update_rating(4.5)

    course.update_rating(0)

    assert course.rating == CourseRating(0)


def test_increaseNumberOfStudents_incremented() -> None:
    course = _course()

    course.increase_number_of_students()
    course.increase_number_of_students()

    assert course.number_of_students == NumberOfStudents(2)


def test_valueObjects_immutable() -> None:
    with pytest.raises(AttributeError):
        CourseRating(1.0).rating = 2.0  # type: ignore[misc]
    with pytest.raises(AttributeError):
        NumberOfStudents(1).number = 2  # type: ignore[misc]


def test_teacher_identityIsUsername() -> None:
    teacher = Teacher(CreateTeacherCommand(username="teacher"))

    assert teacher.id is None
    assert teacher.to_identity() == "teacher"
