"""courses schema (port of Liquibase changesets 1-4 in courses/application/src/main/resources/db/courses.yml)

Revision ID: 0001_courses_schema
Revises:
Create Date: 2026-09-16

GUARD: every table is created only if it does not already exist. In phase 1 of the strangler-fig migration the
schema is owned by the Java monolith's Liquibase changelog, so running ``alembic upgrade head`` against the shared
database is a no-op for the tables and merely records the revision in ``alembic_version``. Against an empty
(standalone) database it creates the exact same tables, columns and foreign keys as Liquibase does.
"""

from __future__ import annotations

from collections.abc import Sequence

import sqlalchemy as sa
from alembic import context, op

revision: str = "0001_courses_schema"
down_revision: str | None = None
branch_labels: str | Sequence[str] | None = None
depends_on: str | Sequence[str] | None = None

LIQUIBASE_MANAGED_TABLES = ("teacher", "course", "curriculum_item", "question")


def _existing_tables() -> set[str]:
    return set(sa.inspect(op.get_bind()).get_table_names())


def upgrade() -> None:
    existing = _existing_tables()

    # changeset 3
    if "teacher" not in existing:
        op.create_table(
            "teacher",
            sa.Column("id", sa.Integer, primary_key=True, autoincrement=True),
            sa.Column("username", sa.String(100), nullable=False),
        )

    # changeset 1
    if "course" not in existing:
        op.create_table(
            "course",
            sa.Column("id", sa.Integer, primary_key=True, autoincrement=True),
            sa.Column("uuid", sa.Uuid, nullable=False),
            sa.Column("name", sa.String(100), nullable=False),
            sa.Column("description", sa.String(100), nullable=False),
            sa.Column("publish_status", sa.String(100), nullable=False),
            sa.Column("approval_status", sa.String(100), nullable=False),
            sa.Column("rating", sa.Float, nullable=False),
            sa.Column("number_of_students", sa.Integer, nullable=False),
            sa.Column("teacher", sa.Integer, nullable=False),
            sa.ForeignKeyConstraint(["teacher"], ["teacher.id"], name="teacher_fkey"),
        )

    # changeset 2
    if "curriculum_item" not in existing:
        op.create_table(
            "curriculum_item",
            sa.Column("id", sa.Integer, primary_key=True, autoincrement=True),
            sa.Column("uuid", sa.Uuid, nullable=False),
            sa.Column("title", sa.String(100), nullable=False),
            sa.Column("description", sa.String(100), nullable=False),
            sa.Column("serial_number", sa.String(100), nullable=False),
            sa.Column("course", sa.Integer, nullable=False),
            sa.Column("type", sa.String(100), nullable=False),
            sa.Column("content", sa.String(100), nullable=False),
            sa.ForeignKeyConstraint(["course"], ["course.id"], name="course_course_fkey"),
        )

    # changeset 4
    if "question" not in existing:
        op.create_table(
            "question",
            sa.Column("id", sa.Integer, primary_key=True, autoincrement=True),
            sa.Column("content", sa.String(100), nullable=False),
            sa.Column("quiz_id", sa.Integer, nullable=False),
            sa.ForeignKeyConstraint(["quiz_id"], ["curriculum_item.id"], name="quiz_fkey"),
        )


def downgrade() -> None:
    # Never drop Liquibase-managed tables from the shared database; downgrade is only meaningful for a standalone DB
    # and is guarded behind an explicit opt-in.
    if context.get_x_argument(as_dictionary=True).get("standalone") != "true":
        raise RuntimeError(
            "Refusing to drop courses tables: they are managed by Liquibase in phase 1. "
            "Pass `-x standalone=true` to downgrade a standalone database."
        )
    for table in reversed(LIQUIBASE_MANAGED_TABLES):
        op.drop_table(table)
