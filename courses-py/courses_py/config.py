"""Runtime settings, read from environment variables (Spring ``application.properties`` analogue)."""

from __future__ import annotations

import os
from dataclasses import dataclass


@dataclass(frozen=True)
class Settings:
    database_url: str
    """SQLAlchemy URL of the database shared with the monolith (``spring.datasource.url``)."""

    jwt_secret_key: str
    """Raw signing secret, same value as ``security.jwt.token.secret-key`` in the Java ``users`` module."""

    broker: str
    """``memory`` (default, in-process) or ``rabbitmq``."""

    rabbitmq_url: str
    """AMQP URL used when ``broker == "rabbitmq"`` (``spring.rabbitmq.*`` analogue)."""

    @classmethod
    def from_env(cls) -> Settings:
        return cls(
            database_url=os.environ.get("COURSES_DATABASE_URL", "sqlite:///./courses.db"),
            jwt_secret_key=os.environ.get("COURSES_JWT_SECRET_KEY", "secret-key"),
            broker=os.environ.get("COURSES_BROKER", "memory"),
            rabbitmq_url=os.environ.get("COURSES_RABBITMQ_URL", "amqp://guest:guest@localhost:5672/%2F"),
        )


settings = Settings.from_env()
