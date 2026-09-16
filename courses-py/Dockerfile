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

# Phase 1: the schema is owned by Liquibase; the Alembic step is a guarded no-op on the shared database.
# Set COURSES_DATABASE_URL, COURSES_JWT_SECRET_KEY, COURSES_BROKER=rabbitmq, COURSES_RABBITMQ_URL at runtime.
EXPOSE 8081
CMD ["uvicorn", "courses_py.api.app:app", "--host", "0.0.0.0", "--port", "8081"]
