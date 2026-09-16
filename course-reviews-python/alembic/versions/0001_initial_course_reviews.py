"""initial course reviews schema (port of db/course-reviews.yml Liquibase changelog)

Revision ID: 0001_initial
Revises:
Create Date: 2026-09-16

"""

from collections.abc import Sequence

import sqlalchemy as sa

from alembic import op

revision: str = "0001_initial"
down_revision: str | None = None
branch_labels: str | Sequence[str] | None = None
depends_on: str | Sequence[str] | None = None


def upgrade() -> None:
    op.create_table(
        "reviewable_course",
        sa.Column("id", sa.Integer(), primary_key=True, autoincrement=True),
        sa.Column("original_course_id", sa.Uuid(), nullable=False),
    )
    op.create_table(
        "reviewer",
        sa.Column("id", sa.Integer(), primary_key=True, autoincrement=True),
        sa.Column("username", sa.String(length=100), nullable=False),
    )
    op.create_table(
        "course_review",
        sa.Column("id", sa.Integer(), primary_key=True, autoincrement=True),
        sa.Column("uuid", sa.Uuid(), nullable=False),
        sa.Column("reviewer", sa.Integer(), nullable=False),
        sa.Column("course", sa.Integer(), nullable=False),
        sa.Column("rating", sa.Float(), nullable=False),
        sa.Column("comment", sa.String(length=100), nullable=True),
        sa.ForeignKeyConstraint(["reviewer"], ["reviewer.id"], name="reviewer_fkey"),
        sa.ForeignKeyConstraint(["course"], ["reviewable_course.id"], name="reviewable_course_fkey"),
    )


def downgrade() -> None:
    op.drop_table("course_review")
    op.drop_table("reviewer")
    op.drop_table("reviewable_course")
