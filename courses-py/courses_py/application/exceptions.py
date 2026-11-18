"""Application-level exceptions (ports of the platform ``common`` module and Spring Security exceptions)."""

from __future__ import annotations


class ResourceNotFoundException(RuntimeError):
    """``common.exception.ResourceNotFoundException`` -> HTTP 404."""


class RelatedResourceIsNotResolvedException(RuntimeError):
    """``common.exception.RelatedResourceIsNotResolvedException`` -> HTTP 400."""


class AccessDeniedException(RuntimeError):
    """Raised when a ``@PreAuthorize`` equivalent rule is not satisfied -> HTTP 403."""
