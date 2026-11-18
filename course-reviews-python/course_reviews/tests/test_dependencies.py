"""Tests for the simplified authentication dependency (``web/dependencies.py::get_current_username``)."""

from __future__ import annotations

import pytest
from fastapi import HTTPException

from course_reviews.web.dependencies import get_current_username


def test_bearer_token_used_as_username() -> None:
    assert get_current_username(authorization="Bearer alice") == "alice"


def test_bearer_scheme_case_insensitive_and_token_trimmed() -> None:
    assert get_current_username(authorization="bearer   alice  ") == "alice"
    assert get_current_username(authorization="BEARER alice") == "alice"


def test_x_username_header_used_when_authorization_absent() -> None:
    assert get_current_username(x_username="bob") == "bob"


def test_authorization_header_takes_precedence_over_x_username() -> None:
    assert get_current_username(authorization="Bearer alice", x_username="bob") == "alice"


@pytest.mark.parametrize("authorization", ["Basic alice", "Bearer", "Bearer   ", "alice"])
def test_unusable_authorization_falls_back_to_x_username(authorization: str) -> None:
    assert get_current_username(authorization=authorization, x_username="bob") == "bob"


@pytest.mark.parametrize(
    ("authorization", "x_username"),
    [(None, None), ("", ""), ("Basic alice", None), ("Bearer ", None), ("Bearer", "")],
)
def test_missing_or_unusable_credentials_unauthorized(authorization: str | None, x_username: str | None) -> None:
    with pytest.raises(HTTPException) as exc_info:
        get_current_username(authorization=authorization, x_username=x_username)

    assert exc_info.value.status_code == 401
    assert exc_info.value.detail == "Not authenticated"
