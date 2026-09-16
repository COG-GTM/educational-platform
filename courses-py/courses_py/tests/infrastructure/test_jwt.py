"""Compatibility of ``JwtTokenProvider`` with tokens produced by the Java ``users`` module (jjwt, HS256)."""

from __future__ import annotations

import time

import jwt
import pytest

from courses_py.infrastructure.security.jwt import InvalidJwtTokenException, JwtTokenProvider, java_signing_key

# Produced by the real ``JwtTokenProvider`` code path (jjwt 0.9.1, ``signWith(HS256, Base64.encode("secret-key"))``):
# sub=teacher, auth=[{"authority": "ROLE_TEACHER"}], iat=1789572658, exp=1789576258.
JAVA_ISSUED_TOKEN = (
    "eyJhbGciOiJIUzI1NiJ9."
    "eyJzdWIiOiJ0ZWFjaGVyIiwiYXV0aCI6W3siYXV0aG9yaXR5IjoiUk9MRV9URUFDSEVSIn1dLCJpYXQiOjE3ODk1NzI2NTgsImV4cCI6MTc4OTU3NjI1OH0."
    "FDaEyiHOnShpRIZTSZytfPk3pcQre5vSLMBz4zrOMns"
)
JAVA_ISSUED_TOKEN_EXP = 1789576258


def _java_style_token(secret: str, exp_offset: int = 3600) -> str:
    """What ``Jwts.builder()...signWith(HS256, Base64.encode(secret))`` produces: jjwt decodes the Base64 again."""
    now = int(time.time())
    return jwt.encode(
        {"sub": "teacher", "auth": [{"authority": "ROLE_TEACHER"}], "iat": now, "exp": now + exp_offset},
        secret.encode(),
        algorithm="HS256",
    )


def test_java_signing_key_isRawSecret() -> None:
    assert java_signing_key("secret-key") == b"secret-key"


def test_javaSigningKey_verifiesTokenIssuedByJjwt() -> None:
    claims = jwt.decode(
        JAVA_ISSUED_TOKEN, java_signing_key("secret-key"), algorithms=["HS256"], options={"verify_exp": False}
    )

    assert claims["sub"] == "teacher" and claims["exp"] == JAVA_ISSUED_TOKEN_EXP
    with pytest.raises(jwt.InvalidSignatureError):
        jwt.decode(JAVA_ISSUED_TOKEN, b"c2VjcmV0LWtleQ==", algorithms=["HS256"], options={"verify_exp": False})


def test_validate_javaToken_principalWithAuthorities() -> None:
    principal = JwtTokenProvider("secret-key").validate_token(_java_style_token("secret-key"))

    assert principal.username == "teacher"
    assert principal.authorities == frozenset({"ROLE_TEACHER"})
    assert principal.has_role("TEACHER") and principal.has_role("ROLE_TEACHER") and not principal.has_role("ADMIN")


def test_validate_expiredToken_invalidJwtTokenException() -> None:
    with pytest.raises(InvalidJwtTokenException, match="Expired or invalid JWT token"):
        JwtTokenProvider("secret-key").validate_token(_java_style_token("secret-key", exp_offset=-10))


def test_validate_tokenWithoutExp_invalidJwtTokenException() -> None:
    token = jwt.encode({"sub": "teacher", "auth": []}, java_signing_key("secret-key"), algorithm="HS256")

    with pytest.raises(InvalidJwtTokenException):
        JwtTokenProvider("secret-key").validate_token(token)


def test_validate_wrongSecret_invalidJwtTokenException() -> None:
    with pytest.raises(InvalidJwtTokenException):
        JwtTokenProvider("secret-key").validate_token(_java_style_token("other"))


def test_validate_noneAlgorithm_rejected() -> None:
    token = jwt.encode({"sub": "teacher"}, key="", algorithm="none")  # noqa: S106

    with pytest.raises(InvalidJwtTokenException):
        JwtTokenProvider("secret-key").validate_token(token)


def test_validate_otherHmacAlgorithm_rejected() -> None:
    token = jwt.encode({"sub": "teacher"}, java_signing_key("secret-key"), algorithm="HS512")

    with pytest.raises(InvalidJwtTokenException):
        JwtTokenProvider("secret-key").validate_token(token)


def test_validate_missingSubject_invalidJwtTokenException() -> None:
    token = jwt.encode({"auth": [{"authority": "ROLE_TEACHER"}]}, java_signing_key("secret-key"), algorithm="HS256")

    with pytest.raises(InvalidJwtTokenException):
        JwtTokenProvider("secret-key").validate_token(token)


@pytest.mark.parametrize("sub", [42, None, ["teacher"], {"name": "teacher"}])
def test_validate_nonStringSubject_invalidJwtTokenException(sub: object) -> None:
    token = jwt.encode({"sub": sub}, java_signing_key("secret-key"), algorithm="HS256")

    with pytest.raises(InvalidJwtTokenException):
        JwtTokenProvider("secret-key").validate_token(token)


@pytest.mark.parametrize(
    "auth",
    [
        None,
        [],
        "ROLE_TEACHER",
        {"authority": "ROLE_TEACHER"},
        [{"role": "ROLE_TEACHER"}, {"authority": 1}, 7, None],
    ],
)
def test_validate_missingOrMalformedAuthClaim_principalWithoutAuthorities(auth: object) -> None:
    claims: dict[str, object] = {"sub": "teacher", "exp": int(time.time()) + 3600}
    if auth is not None:
        claims["auth"] = auth
    token = jwt.encode(claims, java_signing_key("secret-key"), algorithm="HS256")

    principal = JwtTokenProvider("secret-key").validate_token(token)

    assert principal.username == "teacher"
    assert principal.authorities == frozenset()
    assert not principal.has_role("TEACHER")


def test_validate_mixedAuthClaimEntries_onlyValidAuthoritiesKept() -> None:
    claims = {
        "sub": "teacher",
        "exp": int(time.time()) + 3600,
        "auth": ["ROLE_ADMIN", {"authority": "ROLE_TEACHER"}, {"authority": 1}, 7],
    }
    token = jwt.encode(claims, java_signing_key("secret-key"), algorithm="HS256")

    principal = JwtTokenProvider("secret-key").validate_token(token)

    assert principal.authorities == frozenset({"ROLE_ADMIN", "ROLE_TEACHER"})


def test_validate_tokenWithoutIssuedAt_accepted() -> None:
    claims = {"sub": "teacher", "exp": int(time.time()) + 3600}
    token = jwt.encode(claims, java_signing_key("secret-key"), algorithm="HS256")

    assert JwtTokenProvider("secret-key").validate_token(token).username == "teacher"


def test_validate_nonNumericExp_invalidJwtTokenException() -> None:
    token = jwt.encode({"sub": "teacher", "exp": "never"}, java_signing_key("secret-key"), algorithm="HS256")

    with pytest.raises(InvalidJwtTokenException):
        JwtTokenProvider("secret-key").validate_token(token)


def test_createToken_roundTrip() -> None:
    provider = JwtTokenProvider("secret-key")

    principal = provider.validate_token(provider.create_token("teacher", ["TEACHER", "ROLE_ADMIN"]))

    assert principal.authorities == frozenset({"ROLE_TEACHER", "ROLE_ADMIN"})


def test_createToken_javaClaimLayoutAndValidity() -> None:
    provider = JwtTokenProvider("secret-key", validity_ms=120_000)
    before = int(time.time())

    claims = jwt.decode(
        provider.create_token("teacher", ["TEACHER"]), java_signing_key("secret-key"), algorithms=["HS256"]
    )

    assert claims["sub"] == "teacher"
    assert claims["auth"] == [{"authority": "ROLE_TEACHER"}]
    assert before <= claims["iat"] <= before + 5
    assert claims["exp"] - claims["iat"] == 120


def test_createToken_noRoles_emptyAuthorities() -> None:
    provider = JwtTokenProvider("secret-key")

    principal = provider.validate_token(provider.create_token("teacher", []))

    assert principal.authorities == frozenset()
