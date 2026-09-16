"""``python -m courses_py.main`` — run the API with uvicorn."""

from __future__ import annotations

import os

import uvicorn
from alembic import command
from alembic.config import Config

from courses_py.config import Settings


def upgrade_database(config: Settings) -> None:
    """``alembic upgrade head`` for ``config.database_url`` (guarded: existing Liquibase tables are left untouched)."""
    alembic_config = Config()
    alembic_config.set_main_option("script_location", config.migrations_dir)
    alembic_config.set_main_option("sqlalchemy.url", config.database_url)
    command.upgrade(alembic_config, "head")


def main() -> None:
    config = Settings.from_env()
    if config.run_migrations:
        upgrade_database(config)
    uvicorn.run(
        "courses_py.api.app:app",
        host=os.environ.get("COURSES_HOST", "0.0.0.0"),
        port=int(os.environ.get("COURSES_PORT", "8081")),
        reload=os.environ.get("COURSES_RELOAD") == "true",
    )


if __name__ == "__main__":
    main()
