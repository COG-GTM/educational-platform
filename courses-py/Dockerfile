FROM python:3.12-slim AS base

ENV PYTHONDONTWRITEBYTECODE=1 \
    PYTHONUNBUFFERED=1 \
    PIP_NO_CACHE_DIR=1

WORKDIR /app

COPY pyproject.toml README.md ./
COPY courses_py ./courses_py
COPY alembic.ini ./
COPY migrations ./migrations

RUN pip install . psycopg[binary]

# Phase 1: the schema is owned by Liquibase. For a standalone database set COURSES_RUN_MIGRATIONS=true so the guarded
# Alembic upgrade (./migrations) runs before uvicorn starts.
# Set COURSES_DATABASE_URL, COURSES_JWT_SECRET_KEY, COURSES_BROKER=rabbitmq, COURSES_RABBITMQ_URL at runtime.
EXPOSE 8081
CMD ["python", "-m", "courses_py.main"]
