"""Custom column types and the imperative mapping (``orm.py``) round-tripping through SQLite."""

from __future__ import annotations

from uuid import UUID

import pytest
from sqlalchemy import Engine, text
from sqlalchemy.dialects import sqlite
from sqlalchemy.orm import Session, sessionmaker

from courses_py.application.course.create import CreateCourseCommand
from courses_py.domain.course import Course
from courses_py.domain.curriculum_item import CurriculumItem, Lecture, Quiz
from courses_py.domain.enums import ApprovalStatus, LectureType, PublishStatus
from courses_py.domain.value_objects import CourseRating, NumberOfStudents
from courses_py.infrastructure.persistence.orm import EnumByName, IntAsString, mapper_registry, start_mappers
from courses_py.infrastructure.persistence.repositories import SqlAlchemyCourseRepository, _to_item_dto
from courses_py.tests.conftest import insert_teacher

DIALECT = sqlite.dialect()


class TestIntAsString:
    sut = IntAsString()

    @pytest.mark.parametrize(("value", "expected"), [(None, None), (0, "0"), (12, "12"), (-3, "-3")])
    def test_bind_intToString(self, value: int | None, expected: str | None) -> None:
        assert self.sut.process_bind_param(value, DIALECT) == expected

    @pytest.mark.parametrize(("value", "expected"), [(None, None), ("", None), ("0", 0), ("12", 12), ("-3", -3)])
    def test_result_stringToInt(self, value: str | None, expected: int | None) -> None:
        assert self.sut.process_result_value(value, DIALECT) == expected

    def test_result_notANumber_valueError(self) -> None:
        with pytest.raises(ValueError):
            self.sut.process_result_value("abc", DIALECT)


class TestEnumByName:
    sut = EnumByName(ApprovalStatus)

    @pytest.mark.parametrize(
        ("value", "expected"),
        [
            (None, None),
            (ApprovalStatus.WAITING_FOR_APPROVAL, "WAITING_FOR_APPROVAL"),
            ("DECLINED", "DECLINED"),
        ],
    )
    def test_bind_memberOrValueToName(self, value: object, expected: str | None) -> None:
        assert self.sut.process_bind_param(value, DIALECT) == expected

    def test_bind_unknownValue_valueError(self) -> None:
        with pytest.raises(ValueError):
            self.sut.process_bind_param("NOPE", DIALECT)

    def test_bind_memberOfOtherEnum_valueError(self) -> None:
        with pytest.raises(ValueError):
            self.sut.process_bind_param(PublishStatus.DRAFT, DIALECT)

    @pytest.mark.parametrize(
        ("value", "expected"),
        [(None, None), ("APPROVED", ApprovalStatus.APPROVED), ("DECLINED", ApprovalStatus.DECLINED)],
    )
    def test_result_nameToMember(self, value: str | None, expected: ApprovalStatus | None) -> None:
        assert self.sut.process_result_value(value, DIALECT) is expected

    def test_result_unknownName_keyError(self) -> None:
        with pytest.raises(KeyError):
            self.sut.process_result_value("NOPE", DIALECT)

    def test_lectureType_textOnly(self) -> None:
        assert EnumByName(LectureType).process_result_value("TEXT", DIALECT) is LectureType.TEXT


def test_startMappers_idempotent(engine: Engine) -> None:
    mapped_before = {m.class_ for m in mapper_registry.mappers}

    start_mappers()
    start_mappers()

    assert {m.class_ for m in mapper_registry.mappers} == mapped_before
    assert Course in mapped_before


def test_course_roundTrip_liquibaseColumnLayout(session_factory: sessionmaker[Session]) -> None:
    command = CreateCourseCommand.model_validate(
        {
            "name": "name",
            "description": "description",
            "curriculum_items": [
                {"type": "Quiz", "title": "q", "description": "d", "serial_number": 2, "questions": [{"content": "?"}]},
                {"type": "Lecture", "title": "l", "description": "d", "serial_number": 1, "text": "hello"},
            ],
        }
    )
    with session_factory() as session:
        teacher = insert_teacher(session)
        assert teacher.id is not None
        course = Course(command, teacher.id)
        course.send_to_approve()
        course.update_rating(3.5)
        course.increase_number_of_students()
        SqlAlchemyCourseRepository(session).save(course)
        session.commit()
        uuid: UUID = course.uuid

    with session_factory() as session:
        row = session.execute(text("select publish_status, approval_status, rating, number_of_students from course"))
        assert row.one() == ("DRAFT", "WAITING_FOR_APPROVAL", 3.5, 1)
        items = session.execute(text("select title, serial_number, type, content from curriculum_item order by title"))
        assert [tuple(item) for item in items.all()] == [("l", "1", "Lecture", "hello"), ("q", "2", "Quiz", "")]
        assert session.execute(text("select content from question")).scalars().all() == ["?"]

    with session_factory() as session:
        loaded = SqlAlchemyCourseRepository(session).find_by_uuid(uuid)
        assert loaded is not None
        assert loaded.approval_status is ApprovalStatus.WAITING_FOR_APPROVAL
        assert loaded.publish_status is PublishStatus.DRAFT
        assert loaded.rating == CourseRating(3.5)
        assert loaded.number_of_students == NumberOfStudents(1)
        by_title = {item.title: item for item in loaded.curriculum_items}
        assert isinstance(by_title["l"], Lecture)
        assert by_title["l"].serial_number == 1 and by_title["l"].content == "hello"
        assert isinstance(by_title["q"], Quiz)
        assert by_title["q"].serial_number == 2 and [q.content for q in by_title["q"].questions] == ["?"]
        assert all(item.course is loaded for item in loaded.curriculum_items)


def test_toItemDto_baseCurriculumItem_raisesTypeError() -> None:
    # given: a mapped-but-unhandled curriculum item type must not be silently dropped from the projection
    item = CurriculumItem("title", "description", None, 1)

    # when / then
    with pytest.raises(TypeError, match="Unsupported curriculum item: CurriculumItem"):
        _to_item_dto(item)
