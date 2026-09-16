"""Compatibility of ``JwtTokenProvider`` with tokens produced by the Java ``users`` module (jjwt, HS256)."""

from __future__ import annotations

import base64
import time

import jwt
import pytest

from courses_py.infrastructure.security.jwt import InvalidJwtTokenException, JwtTokenProvider, java_signing_key


def _java_style_token(secret: str, exp_offset: int = 3600) -> str:
    """What ``Jwts.builder()...signWith(HS256, Base64.encode(secret))`` produces."""
    now = int(time.time())
    return jwt.encode(
        {"sub": "teacher", "auth": [{"authority": "ROLE_TEACHER"}], "iat": now, "exp": now + exp_offset},
        base64.b64encode(secret.encode()),
        algorithm="HS256",
    )


def test_java_signing_key_isBase64OfSecret() -> None:
    assert java_signing_key("secret-key") == b"c2VjcmV0LWtleQ=="


def test_validate_javaToken_principalWithAuthorities() -> None:
    principal = JwtTokenProvider("secret-key").validate_token(_java_style_token("secret-key"))

    assert principal.username == "teacher"
    assert principal.authorities == frozenset({"ROLE_TEACHER"})
    assert principal.has_role("TEACHER") and principal.has_role("ROLE_TEACHER") and not principal.has_role("ADMIN")


def test_validate_expiredToken_invalidJwtTokenException() -> None:
    with pytest.raises(InvalidJwtTokenException, match="Expired or invalid JWT token"):
        JwtTokenProvider("secret-key").validate_token(_java_style_token("secret-key", exp_offset=-10))


def test_validate_wrongSecret_invalidJwtTokenException() -> None:
    with pytest.raises(InvalidJwtTokenException):
        JwtTokenProvider("secret-key").validate_token(_java_style_token("other"))


def test_validate_noneAlgorithm_rejected() -> None:
    token = jwt.encode({"sub": "teacher"}, key="", algorithm="none")  # noqa: S106

    with pytest.raises(InvalidJwtTokenException):
        JwtTokenProvider("secret-key").validate_token(token)


def test_createToken_roundTrip() -> None:
    provider = JwtTokenProvider("secret-key")

    principal = provider.validate_token(provider.create_token("teacher", ["TEACHER", "ROLE_ADMIN"]))

    assert principal.authorities == frozenset({"ROLE_TEACHER", "ROLE_ADMIN"})
