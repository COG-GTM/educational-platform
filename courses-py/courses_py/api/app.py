"""FastAPI application factory."""

from __future__ import annotations

from fastapi import FastAPI
from sqlalchemy import Engine

from courses_py import __version__
from courses_py.api.dependencies import AppContext
from courses_py.api.errors import register_error_handlers
from courses_py.api.routers.courses import router as courses_router
from courses_py.config import Settings, settings
from courses_py.infrastructure.messaging.broker import InMemoryMessageBroker, MessageBroker
from courses_py.infrastructure.messaging.factory import build_broker
from courses_py.infrastructure.persistence.database import build_engine, build_session_factory
from courses_py.infrastructure.persistence.orm import start_mappers
from courses_py.infrastructure.security.jwt import JwtTokenProvider
from courses_py.integration_events.consumer import register_consumers


def create_app(
    config: Settings = settings,
    *,
    engine: Engine | None = None,
    broker: MessageBroker | None = None,
) -> FastAPI:
    start_mappers()
    engine = engine or build_engine(config.database_url)
    session_factory = build_session_factory(engine)
    broker = broker or build_broker(config)

    app = FastAPI(title="Courses", description="Courses API (Python strangler-fig port)", version=__version__)
    app.state.context = AppContext(
        session_factory=session_factory, broker=broker, jwt=JwtTokenProvider(config.jwt_secret_key)
    )
    app.include_router(courses_router)
    register_error_handlers(app)

    # With the in-memory broker the API process is also the consumer (events published in-process are
    # dispatched synchronously); with RabbitMQ, run the dedicated `courses-py-consumer` process instead.
    if isinstance(broker, InMemoryMessageBroker):
        register_consumers(broker, session_factory)

    return app


app = create_app()
