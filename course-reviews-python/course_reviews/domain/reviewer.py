"""Local projection of a user from the ``users`` context (``reviewer/Reviewer.java``)."""

from __future__ import annotations


class Reviewer:
    id: int | None
    username: str

    def __init__(self, username: str) -> None:
        self.id = None
        self.username = username

    @property
    def local_id(self) -> int:
        """Database identity; only available once the entity has been persisted."""
        if self.id is None:
            raise ValueError("Reviewer has not been persisted yet")
        return self.id
