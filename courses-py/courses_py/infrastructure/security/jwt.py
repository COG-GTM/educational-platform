"""Validation of the JWTs issued by the Java ``users`` module (``JwtTokenProvider.java``).

Compatibility notes (taken from ``users/application/.../security/JwtTokenProvider.java``):

* algorithm ``HS256``; the signing key is ``Base64.encode(secret-key)`` where ``secret-key`` is the raw value of
  ``security.jwt.token.secret-key`` (default ``secret-key``). jjwt 0.9 treats that Base64 *string* as the key bytes.
* ``sub`` = username, ``iat``/``exp`` set by Java (``exp`` is enforced here as in Java).
* ``auth`` = list of ``SimpleGrantedAuthority`` objects, serialised as ``[{"authority": "ROLE_TEACHER"}, ...]``.
  Java re-loads authorities from the ``users`` table instead of trusting the claim; the Python service cannot reach that
  module's repository, so it trusts the signed ``auth`` claim (see README, "Authentication").
"""

from __future__ import annotations

import base64
from collections.abc import Iterable

import jwt

from courses_py.application.security import Principal

ALGORITHM = "HS256"
AUTHORITIES_CLAIM = "auth"


class InvalidJwtTokenException(Exception):
    """``JwtTokenFilter``: ``Expired or invalid JWT token`` (HTTP 400 in Java)."""

    def __init__(self) -> None:
        super().__init__("Expired or invalid JWT token")


def java_signing_key(secret_key: str) -> bytes:
    return base64.b64encode(secret_key.encode("utf-8"))


def _authorities(raw: object) -> Iterable[str]:
    if not isinstance(raw, list):
        return ()
    result: list[str] = []
    for entry in raw:
        if isinstance(entry, str):
            result.append(entry)
        elif isinstance(entry, dict) and isinstance(entry.get("authority"), str):
            result.append(entry["authority"])
    return result


class JwtTokenProvider:
    def __init__(self, secret_key: str, validity_ms: int = 3_600_000) -> None:
        self._key = java_signing_key(secret_key)
        self._validity_ms = validity_ms

    def validate_token(self, token: str) -> Principal:
        try:
            claims = jwt.decode(token, self._key, algorithms=[ALGORITHM], options={"require": ["sub"]})
        except jwt.PyJWTError as e:
            raise InvalidJwtTokenException() from e
        username = claims["sub"]
        if not isinstance(username, str):
            raise InvalidJwtTokenException()
        return Principal(username=username, authorities=frozenset(_authorities(claims.get(AUTHORITIES_CLAIM))))

    def create_token(self, username: str, roles: Iterable[str]) -> str:
        """Test helper mirroring ``JwtTokenProvider.createToken`` (same claim layout Java produces)."""
        import time

        now = int(time.time())
        payload = {
            "sub": username,
            AUTHORITIES_CLAIM: [{"authority": role if role.startswith("ROLE_") else f"ROLE_{role}"} for role in roles],
            "iat": now,
            "exp": now + self._validity_ms // 1000,
        }
        return jwt.encode(payload, self._key, algorithm=ALGORITHM)
