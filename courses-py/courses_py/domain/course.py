"""Course aggregate root: port of ``courses/course/Course.java`` including its state machine."""

from __future__ import annotations

from typing import TYPE_CHECKING
from uuid import UUID, uuid4

from courses_py.domain.curriculum_item import CurriculumItem, Lecture, Quiz
from courses_py.domain.enums import ApprovalStatus, PublishStatus
from courses_py.domain.exceptions import CourseAlreadyApprovedException, CourseCannotBePublishedException
from courses_py.domain.value_objects import CourseRating, NumberOfStudents

if TYPE_CHECKING:
    from courses_py.application.course.create import CreateCourseCommand, CreateCurriculumItemCommand


class Course:
    id: int | None
    uuid: UUID
    name: str
    description: str
    publish_status: PublishStatus
    approval_status: ApprovalStatus
    rating: CourseRating
    number_of_students: NumberOfStudents
    teacher: int
    curriculum_items: list[CurriculumItem]

    def __init__(self, command: CreateCourseCommand, teacher: int) -> None:
        self.id = None
        self.uuid = uuid4()
        self.name = command.name
        self.description = command.description
        self.rating = CourseRating(0)
        self.number_of_students = NumberOfStudents(0)
        self.publish_status = PublishStatus.DRAFT
        self.approval_status = ApprovalStatus.NOT_SENT_FOR_APPROVAL
        self.teacher = teacher
        self.curriculum_items = [create_curriculum_item(item, self) for item in (command.curriculum_items or [])]

    def approve(self) -> None:
        self.approval_status = ApprovalStatus.APPROVED

    def decline(self) -> None:
        self.approval_status = ApprovalStatus.DECLINED

    def send_to_approve(self) -> None:
        if self.approval_status == ApprovalStatus.APPROVED:
            raise CourseAlreadyApprovedException(self.uuid)
        self.approval_status = ApprovalStatus.WAITING_FOR_APPROVAL

    def publish(self) -> None:
        if self.approval_status != ApprovalStatus.APPROVED:
            raise CourseCannotBePublishedException(self.uuid)
        self.publish_status = PublishStatus.PUBLISHED

    def archive(self) -> None:
        self.publish_status = PublishStatus.ARCHIVED

    def update_rating(self, value: float) -> None:
        self.rating = CourseRating(value)

    def increase_number_of_students(self) -> None:
        self.number_of_students = NumberOfStudents(self.number_of_students.number + 1)

    def to_identity(self) -> UUID:
        return self.uuid


def create_curriculum_item(command: CreateCurriculumItemCommand, course: Course) -> CurriculumItem:
    """``CurriculumItemFactory.createFrom``: dispatches on the command's ``type`` discriminator."""
    if command.type == "Lecture":
        return Lecture(command, command.serial_number, course)
    if command.type == "Quiz":
        return Quiz(command, command.serial_number, course)
    raise TypeError(f"Unsupported curriculum item type: {command.type!r}")
