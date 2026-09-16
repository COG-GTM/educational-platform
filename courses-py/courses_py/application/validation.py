"""Shared Pydantic constraints replacing Javax/Jakarta validation annotations."""

from __future__ import annotations

from typing import Annotated

from pydantic import AfterValidator


def _not_blank(value: str) -> str:
    if not value.strip():
        raise ValueError("must not be blank")
    return value


NotBlank = Annotated[str, AfterValidator(_not_blank)]
"""``@NotBlank``: non-null string with at least one non-whitespace character."""
