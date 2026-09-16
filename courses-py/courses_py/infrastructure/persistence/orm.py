"""Imperative SQLAlchemy mapping of the domain onto the tables defined in
``courses/application/src/main/resources/db/courses.yml`` (managed by Liquibase in phase 1).

Table and column names are taken verbatim from the Liquibase changelog. The domain classes stay ORM-agnostic;
``CourseRating`` / ``NumberOfStudents`` are mapped as composites onto ``rating`` / ``number_of_students``,
mirroring the JPA ``@Embeddable`` layout.
"""

from __future__ import annotations

from typing import Any

from sqlalchemy import Column, Float, ForeignKey, Integer, MetaData, String, Table, Uuid
from sqlalchemy.engine.interfaces import Dialect
from sqlalchemy.orm import composite, registry, relationship
from sqlalchemy.types import TypeDecorator

from courses_py.domain.course import Course
from courses_py.domain.curriculum_item import CurriculumItem, Lecture, Question, Quiz
from courses_py.domain.enums import ApprovalStatus, PublishStatus
from courses_py.domain.teacher import Teacher
from courses_py.domain.value_objects import CourseRating, NumberOfStudents

metadata = MetaData()
mapper_registry = registry(metadata=metadata)


class IntAsString(TypeDecorator[int]):
    """``curriculum_item.serial_number`` is ``VARCHAR(100)`` in Liquibase but an ``Integer`` in the domain."""

    impl = String(100)
    cache_ok = True

    def process_bind_param(self, value: int | None, dialect: Dialect) -> str | None:
        return None if value is None else str(value)

    def process_result_value(self, value: str | None, dialect: Dialect) -> int | None:
        return None if value is None or value == "" else int(value)


class EnumByName(TypeDecorator[Any]):
    """``@Enumerated(EnumType.STRING)``: persist enum members by name in a VARCHAR column."""

    impl = String(100)
    cache_ok = True

    def __init__(self, enum_type: type[Any]) -> None:
        super().__init__()
        self._enum_type = enum_type

    def process_bind_param(self, value: Any, dialect: Dialect) -> str | None:
        if value is None:
            return None
        return str(value.name if isinstance(value, self._enum_type) else self._enum_type(value).name)

    def process_result_value(self, value: str | None, dialect: Dialect) -> Any:
        return None if value is None else self._enum_type[value]


teacher_table = Table(
    "teacher",
    metadata,
    Column("id", Integer, primary_key=True, autoincrement=True),
    Column("username", String(100), nullable=False),
)

course_table = Table(
    "course",
    metadata,
    Column("id", Integer, primary_key=True, autoincrement=True),
    Column("uuid", Uuid, nullable=False),
    Column("name", String(100), nullable=False),
    Column("description", String(100), nullable=False),
    Column("publish_status", EnumByName(PublishStatus), nullable=False),
    Column("approval_status", EnumByName(ApprovalStatus), nullable=False),
    Column("rating", Float, nullable=False),
    Column("number_of_students", Integer, nullable=False),
    Column("teacher", Integer, ForeignKey("teacher.id", name="teacher_fkey"), nullable=False),
)

curriculum_item_table = Table(
    "curriculum_item",
    metadata,
    Column("id", Integer, primary_key=True, autoincrement=True),
    Column("uuid", Uuid, nullable=False),
    Column("title", String(100), nullable=False),
    Column("description", String(100), nullable=False),
    Column("serial_number", IntAsString, nullable=False),
    Column("course", Integer, ForeignKey("course.id", name="course_course_fkey"), nullable=False),
    Column("type", String(100), nullable=False),
    Column("content", String(100), nullable=False, default=""),
)

question_table = Table(
    "question",
    metadata,
    Column("id", Integer, primary_key=True, autoincrement=True),
    Column("content", String(100), nullable=False),
    Column("quiz_id", Integer, ForeignKey("curriculum_item.id", name="quiz_fkey"), nullable=False),
)


def start_mappers() -> None:
    if Course in {m.class_ for m in mapper_registry.mappers}:
        return

    mapper_registry.map_imperatively(Teacher, teacher_table)

    mapper_registry.map_imperatively(
        Course,
        course_table,
        properties={
            "_rating": course_table.c.rating,
            "_number_of_students": course_table.c.number_of_students,
            "rating": composite(CourseRating, "_rating"),
            "number_of_students": composite(NumberOfStudents, "_number_of_students"),
            "curriculum_items": relationship(
                CurriculumItem,
                back_populates="course",
                cascade="all, delete-orphan",
                lazy="selectin",
                order_by=curriculum_item_table.c.id,
            ),
        },
    )

    mapper_registry.map_imperatively(
        CurriculumItem,
        curriculum_item_table,
        polymorphic_on=curriculum_item_table.c.type,
        polymorphic_identity=CurriculumItem.DISCRIMINATOR,
        properties={
            "course_id": curriculum_item_table.c.course,
            "course": relationship(Course, back_populates="curriculum_items"),
        },
        exclude_properties={"content"},
    )
    mapper_registry.map_imperatively(
        Lecture,
        inherits=CurriculumItem,
        polymorphic_identity=Lecture.DISCRIMINATOR,
        properties={"content": curriculum_item_table.c.content},
    )
    mapper_registry.map_imperatively(
        Quiz,
        inherits=CurriculumItem,
        polymorphic_identity=Quiz.DISCRIMINATOR,
        properties={
            "questions": relationship(
                Question,
                back_populates="quiz",
                cascade="all, delete-orphan",
                lazy="selectin",
                order_by=question_table.c.id,
            ),
        },
    )

    mapper_registry.map_imperatively(
        Question,
        question_table,
        properties={"quiz": relationship(Quiz, back_populates="questions")},
    )
