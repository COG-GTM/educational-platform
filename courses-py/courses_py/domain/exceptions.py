"""Domain exceptions of the Course aggregate."""

from __future__ import annotations

from uuid import UUID


class CourseAlreadyApprovedException(RuntimeError):
    """Course cannot be sent for approval because it was already approved."""

    def __init__(self, uuid: UUID) -> None:
        self.uuid = uuid
        super().__init__(f"Course with uuid = {uuid} cannot be sent for approval, course was already approved")


class CourseCannotBePublishedException(RuntimeError):
    """Course cannot be published because it has not been approved by an admin."""

    def __init__(self, uuid: UUID) -> None:
        self.uuid = uuid
        super().__init__(f"Course with uuid = {uuid} cannot be published, course should be approved by admin at first")
