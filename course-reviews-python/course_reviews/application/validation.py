"""``jakarta.validation.Validator`` analogue built on Pydantic."""

from __future__ import annotations

from pydantic import BaseModel, ValidationError

from course_reviews.exceptions import ConstraintViolationException


class Validator:
    def validate(self, model: BaseModel) -> list[str]:
        try:
            type(model).model_validate(dict(model))
        except ValidationError as e:
            return [f"{'.'.join(str(loc) for loc in err['loc'])}: {err['msg']}" for err in e.errors()]
        return []

    def validate_or_raise(self, model: BaseModel) -> None:
        violations = self.validate(model)
        if violations:
            raise ConstraintViolationException(violations)
