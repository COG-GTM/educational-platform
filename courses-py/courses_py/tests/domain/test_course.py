"""Port of ``CourseTest.java``: the ``Course`` aggregate state machine."""

from __future__ import annotations

import pytest

from courses_py.application.course.create import CreateCourseCommand
from courses_py.domain.course import Course
from courses_py.domain.curriculum_item import Lecture, Quiz
from courses_py.domain.enums import ApprovalStatus, PublishStatus
from courses_py.domain.exceptions import CourseAlreadyApprovedException, CourseCannotBePublishedException
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


def test_sendToApprove_courseAlreadyApproved_courseAlreadyApprovedException() -> None:
    course = _course()
    course.approve()

    with pytest.raises(CourseAlreadyApprovedException) as e:
        course.send_to_approve()

    assert str(e.value) == f"Course with uuid = {course.uuid} cannot be sent for approval, course was already approved"


def test_sendToApprove_waitingForApprovalStatus() -> None:
    course = _course()

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


def test_increaseNumberOfStudents_incremented() -> None:
    course = _course()

    course.increase_number_of_students()
    course.increase_number_of_students()

    assert course.number_of_students == NumberOfStudents(2)
