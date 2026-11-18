"""Course Review aggregate root (``CourseReview.java``)."""

from __future__ import annotations

from uuid import UUID, uuid4

from course_reviews.domain.value_objects import Comment, CourseRating


class CourseReview:
    id: int | None
    uuid: UUID
    reviewer: int
    course: int
    rating: CourseRating
    comment: Comment

    def __init__(self, *, uuid: UUID, course: int, reviewer: int, rating: CourseRating, comment: Comment) -> None:
        self.id = None
        self.uuid = uuid
        self.course = course
        self.reviewer = reviewer
        self.rating = rating
        self.comment = comment

    @classmethod
    def create(cls, *, course: int, reviewer: int, rating: float, comment: str | None) -> CourseReview:
        return cls(
            uuid=uuid4(),
            course=course,
            reviewer=reviewer,
            rating=CourseRating(rating),
            comment=Comment(comment),
        )

    def update(self, rating: float, comment: str | None) -> None:
        self.rating = CourseRating(rating)
        self.comment = Comment(comment)

    def to_identifier(self) -> UUID:
        return self.uuid
