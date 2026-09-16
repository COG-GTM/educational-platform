"""Port of ``jpa/CourseReviewRepositoryTest.java`` (in-memory SQLite as the H2 analogue)."""

from __future__ import annotations

from uuid import UUID, uuid4

import pytest
from sqlalchemy.orm import Session

from course_reviews.domain.course_review import CourseReview
from course_reviews.domain.reviewable_course import ReviewableCourse
from course_reviews.domain.reviewer import Reviewer
from course_reviews.infrastructure.repositories import (
    CourseReviewRepository,
    ReviewableCourseRepository,
    ReviewerRepository,
)
from course_reviews.tests.conftest import (
    COURSE_REVIEW_SQL,
    COURSE_REVIEW_UUID,
    COURSE_UUID,
    REVIEWER_USERNAME,
    execute_script,
)


@pytest.fixture
def sut(session: Session) -> CourseReviewRepository:
    execute_script(session, COURSE_REVIEW_SQL)
    return CourseReviewRepository(session)


def test_list_course_reviews_unpaged_course_reviews(sut: CourseReviewRepository) -> None:
    # given/when
    result = sut.list_course_reviews(COURSE_UUID)

    # then
    assert len(result) == 1
    dto = result[0]
    assert dto.uuid == COURSE_REVIEW_UUID
    assert dto.course == COURSE_UUID
    assert dto.username == REVIEWER_USERNAME
    assert dto.comment == "comment"
    assert dto.rating == 4.0


def test_list_course_reviews_other_course_empty(sut: CourseReviewRepository) -> None:
    assert sut.list_course_reviews(uuid4()) == []


def test_is_reviewer_valid_reviewer_true(sut: CourseReviewRepository) -> None:
    assert sut.is_reviewer(COURSE_REVIEW_UUID, REVIEWER_USERNAME) is True


def test_is_reviewer_invalid_reviewer_false(sut: CourseReviewRepository) -> None:
    assert sut.is_reviewer(COURSE_REVIEW_UUID, "another-reviewer") is False


def test_find_by_uuid_existing_review_loaded_with_value_objects(sut: CourseReviewRepository) -> None:
    review = sut.find_by_uuid(COURSE_REVIEW_UUID)

    assert review is not None
    assert review.to_identifier() == COURSE_REVIEW_UUID
    assert review.rating.rating == 4.0
    assert review.comment.comment == "comment"


def test_find_by_uuid_unknown_none(sut: CourseReviewRepository) -> None:
    assert sut.find_by_uuid(uuid4()) is None


def test_save_round_trip_and_average_rating(session: Session, sut: CourseReviewRepository) -> None:
    # given
    course = ReviewableCourseRepository(session).find_by_original_course_id(COURSE_UUID)
    reviewer = ReviewerRepository(session).find_by_username("another-reviewer")
    assert course is not None and reviewer is not None

    # when
    saved = sut.save(CourseReview.create(course=course.local_id, reviewer=reviewer.local_id, rating=2.0, comment=None))
    session.commit()
    session.expunge_all()

    # then
    loaded = sut.find_by_uuid(saved.uuid)
    assert loaded is not None and loaded.id == saved.id
    assert loaded.comment.comment is None
    assert sut.average_rating(course.local_id) == pytest.approx(3.0)
    assert len(sut.list_course_reviews(COURSE_UUID)) == 2


def test_average_rating_course_without_reviews_zero(session: Session, sut: CourseReviewRepository) -> None:
    course = ReviewableCourseRepository(session).save(ReviewableCourse(uuid4()))

    assert sut.average_rating(course.local_id) == 0.0


def test_average_rating_only_counts_reviews_of_given_course(session: Session, sut: CourseReviewRepository) -> None:
    # given
    other_course = ReviewableCourseRepository(session).save(ReviewableCourse(uuid4()))
    reviewer = ReviewerRepository(session).find_by_username(REVIEWER_USERNAME)
    assert reviewer is not None
    sut.save(CourseReview.create(course=other_course.local_id, reviewer=reviewer.local_id, rating=1.0, comment=None))
    sut.save(CourseReview.create(course=other_course.local_id, reviewer=reviewer.local_id, rating=0.0, comment=None))

    # when / then
    assert sut.average_rating(other_course.local_id) == pytest.approx(0.5)
    seeded_course = ReviewableCourseRepository(session).find_by_original_course_id(COURSE_UUID)
    assert seeded_course is not None
    assert sut.average_rating(seeded_course.local_id) == pytest.approx(4.0)


def test_is_reviewer_unknown_review_false(sut: CourseReviewRepository) -> None:
    assert sut.is_reviewer(uuid4(), REVIEWER_USERNAME) is False


def test_update_persists_new_value_objects(session: Session, sut: CourseReviewRepository) -> None:
    # given
    review = sut.find_by_uuid(COURSE_REVIEW_UUID)
    assert review is not None

    # when
    review.update(1.5, None)
    sut.save(review)
    session.commit()
    session.expunge_all()

    # then
    reloaded = sut.find_by_uuid(COURSE_REVIEW_UUID)
    assert reloaded is not None
    assert reloaded.rating.rating == 1.5
    assert reloaded.comment.comment is None
    dto = sut.list_course_reviews(COURSE_UUID)[0]
    assert (dto.rating, dto.comment) == (1.5, None)


def test_reviewable_course_and_reviewer_repositories(session: Session) -> None:
    courses = ReviewableCourseRepository(session)
    reviewers = ReviewerRepository(session)
    original = UUID("123e4567-e89b-12d3-a456-426655440099")

    course = courses.save(ReviewableCourse(original))
    reviewer = reviewers.save(Reviewer("someone"))

    assert course.id is not None and reviewer.id is not None
    assert courses.find_by_original_course_id(original) is course
    assert courses.find_by_id(course.id) is course
    assert courses.find_by_original_course_id(uuid4()) is None
    assert reviewers.find_by_username("someone") is reviewer
    assert reviewers.find_by_username("nobody") is None
