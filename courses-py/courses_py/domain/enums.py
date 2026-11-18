"""Enumerations of the Course aggregate (``PublishStatus``, ``ApprovalStatus``, ``LectureType`` in Java).

Values are persisted by name (``@Enumerated(EnumType.STRING)``), so the member names must stay identical
to the Java enum constants.
"""

from __future__ import annotations

from enum import Enum


class PublishStatus(str, Enum):
    DRAFT = "DRAFT"
    PUBLISHED = "PUBLISHED"
    ARCHIVED = "ARCHIVED"


class ApprovalStatus(str, Enum):
    NOT_SENT_FOR_APPROVAL = "NOT_SENT_FOR_APPROVAL"
    WAITING_FOR_APPROVAL = "WAITING_FOR_APPROVAL"
    DECLINED = "DECLINED"
    APPROVED = "APPROVED"


class LectureType(str, Enum):
    TEXT = "TEXT"
