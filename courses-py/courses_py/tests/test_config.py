"""``Settings.from_env``: environment variable names and defaults."""

from __future__ import annotations

import pytest

from courses_py.config import Settings

ENV_VARS = (
    "COURSES_DATABASE_URL",
    "COURSES_JWT_SECRET_KEY",
    "COURSES_BROKER",
    "COURSES_RABBITMQ_URL",
    "COURSES_RUN_MIGRATIONS",
    "COURSES_MIGRATIONS_DIR",
)


@pytest.fixture(autouse=True)
def clean_env(monkeypatch: pytest.MonkeyPatch) -> None:
    for name in ENV_VARS:
        monkeypatch.delenv(name, raising=False)


def test_fromEnv_noVariables_javaCompatibleDefaults() -> None:
    assert Settings.from_env() == Settings(
        database_url="sqlite:///./courses.db",
        jwt_secret_key="secret-key",
        broker="memory",
        rabbitmq_url="amqp://guest:guest@localhost:5672/%2F",
    )


def test_fromEnv_allVariables_overridden(monkeypatch: pytest.MonkeyPatch) -> None:
    monkeypatch.setenv("COURSES_DATABASE_URL", "postgresql+psycopg://u:p@db/platform")
    monkeypatch.setenv("COURSES_JWT_SECRET_KEY", "shared-with-java")
    monkeypatch.setenv("COURSES_BROKER", "rabbitmq")
    monkeypatch.setenv("COURSES_RABBITMQ_URL", "amqp://u:p@rabbit:5672/vhost")
    monkeypatch.setenv("COURSES_RUN_MIGRATIONS", "TRUE")
    monkeypatch.setenv("COURSES_MIGRATIONS_DIR", "/opt/courses-py/migrations")

    assert Settings.from_env() == Settings(
        database_url="postgresql+psycopg://u:p@db/platform",
        jwt_secret_key="shared-with-java",
        broker="rabbitmq",
        rabbitmq_url="amqp://u:p@rabbit:5672/vhost",
        run_migrations=True,
        migrations_dir="/opt/courses-py/migrations",
    )


@pytest.mark.parametrize("value", ["false", "1", "yes", ""])
def test_fromEnv_runMigrationsNotTrue_disabled(monkeypatch: pytest.MonkeyPatch, value: str) -> None:
    monkeypatch.setenv("COURSES_RUN_MIGRATIONS", value)

    assert Settings.from_env().run_migrations is False


def test_fromEnv_emptyVariable_keptAsEmptyString(monkeypatch: pytest.MonkeyPatch) -> None:
    monkeypatch.setenv("COURSES_BROKER", "")

    assert Settings.from_env().broker == ""


def test_settings_isFrozen() -> None:
    settings = Settings.from_env()

    with pytest.raises(AttributeError):
        settings.broker = "rabbitmq"  # type: ignore[misc]
