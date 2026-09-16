"""Method-security primitives replacing Spring's ``SecurityContextHolder`` / ``@PreAuthorize``.

``Principal`` is what the JWT filter puts into the security context; ``CurrentUser`` is the port through
which handlers obtain it (the web layer supplies a request-scoped implementation).
"""

from __future__ import annotations

from dataclasses import dataclass, field
from typing import Protocol

from courses_py.application.exceptions import AccessDeniedException

ROLE_PREFIX = "ROLE_"
ROLE_TEACHER = "TEACHER"
ROLE_STUDENT = "STUDENT"
ROLE_ADMIN = "ADMIN"


@dataclass(frozen=True)
class Principal:
    username: str
    authorities: frozenset[str] = field(default_factory=frozenset)

    def has_role(self, role: str) -> bool:
        """``hasRole('TEACHER')``: accepts both ``TEACHER`` and ``ROLE_TEACHER`` spellings."""
        authority = role if role.startswith(ROLE_PREFIX) else ROLE_PREFIX + role
        return authority in self.authorities


class CurrentUser(Protocol):
    def principal(self) -> Principal: ...


@dataclass(frozen=True)
class StaticCurrentUser:
    _principal: Principal

    def principal(self) -> Principal:
        return self._principal


def require_role(principal: Principal, role: str) -> None:
    if not principal.has_role(role):
        raise AccessDeniedException("Access Denied")
