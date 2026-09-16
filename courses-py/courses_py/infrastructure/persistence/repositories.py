"""SQLAlchemy implementations of the ``CourseRepository`` / ``TeacherRepository`` ports."""

from __future__ import annotations

from uuid import UUID

from sqlalchemy import exists, select
from sqlalchemy.orm import Session

from courses_py.application.dtos import CourseDTO, CourseLightDTO, LectureDTO, QuestionDTO, QuizDTO
from courses_py.domain.course import Course
from courses_py.domain.curriculum_item import CurriculumItem, Lecture, Quiz
from courses_py.domain.teacher import Teacher
from courses_py.infrastructure.persistence.orm import course_table, teacher_table


class SqlAlchemyCourseRepository:
    def __init__(self, session: Session) -> None:
        self._session = session

    def save(self, course: Course) -> Course:
        self._session.add(course)
        self._session.flush()
        return course

    def find_by_uuid(self, uuid: UUID) -> Course | None:
        return self._session.scalars(select(Course).where(course_table.c.uuid == uuid)).first()

    def find_dto_by_uuid(self, uuid: UUID) -> CourseDTO | None:
        """``CourseRepository.findDTOByUuid`` (projection ``CourseDTO`` with curriculum items)."""
        course = self.find_by_uuid(uuid)
        return None if course is None else _to_course_dto(course)

    def list(self) -> list[CourseLightDTO]:
        """``CourseRepository.list`` (projection ``CourseLightDTO``)."""
        rows = self._session.execute(
            select(
                course_table.c.uuid,
                course_table.c.name,
                course_table.c.description,
                course_table.c.number_of_students,
            ).order_by(course_table.c.id)
        )
        return [
            CourseLightDTO(
                uuid=row.uuid, name=row.name, description=row.description, number_of_students=row.number_of_students
            )
            for row in rows
        ]

    def is_teacher(self, uuid: UUID, username: str) -> bool:
        """``CourseRepository.isTeacher``: does ``username`` own the course identified by ``uuid``?"""
        stmt = select(
            exists()
            .where(course_table.c.uuid == uuid)
            .where(course_table.c.teacher == teacher_table.c.id)
            .where(teacher_table.c.username == username)
        )
        return bool(self._session.execute(stmt).scalar())


class SqlAlchemyTeacherRepository:
    def __init__(self, session: Session) -> None:
        self._session = session

    def save(self, teacher: Teacher) -> Teacher:
        self._session.add(teacher)
        self._session.flush()
        return teacher

    def find_by_username(self, username: str) -> Teacher | None:
        return self._session.scalars(select(Teacher).where(teacher_table.c.username == username)).first()


def _to_course_dto(course: Course) -> CourseDTO:
    return CourseDTO(
        uuid=course.uuid,
        name=course.name,
        description=course.description,
        number_of_students=course.number_of_students.number,
        curriculum_items=[_to_item_dto(item) for item in course.curriculum_items],
    )


def _to_item_dto(item: CurriculumItem) -> LectureDTO | QuizDTO:
    if isinstance(item, Lecture):
        return LectureDTO(
            uuid=item.uuid,
            title=item.title,
            description=item.description,
            serial_number=item.serial_number,
            text=item.content,
        )
    if isinstance(item, Quiz):
        return QuizDTO(
            uuid=item.uuid,
            title=item.title,
            description=item.description,
            serial_number=item.serial_number,
            questions=[QuestionDTO(content=question.content) for question in item.questions],
        )
    raise TypeError(f"Unsupported curriculum item: {type(item).__name__}")
