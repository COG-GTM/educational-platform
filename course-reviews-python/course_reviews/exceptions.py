"""Exceptions mirroring ``com.educational.platform.common.exception`` and ``jakarta.validation``."""

from __future__ import annotations

from collections.abc import Sequence


class RelatedResourceIsNotResolvedException(Exception):
    """Raised when a related resource (course, reviewer) cannot be resolved by relation."""


class ResourceNotFoundException(Exception):
    """Raised when the requested aggregate does not exist."""


class ConstraintViolationException(Exception):
    """Raised when a command fails validation; carries the individual violation messages."""

    def __init__(self, violations: Sequence[str]):
        self.violations = list(violations)
        super().__init__("; ".join(self.violations))


class AccessDeniedException(Exception):
    """Raised when the authenticated user is not allowed to perform the operation."""
