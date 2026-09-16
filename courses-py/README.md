# courses-py

Python port of the Java `courses` bounded context (`courses/`), built with FastAPI, SQLAlchemy 2.x, Alembic,
Pydantic v2 and pytest. It is being introduced with a **strangler-fig** approach: the Java module keeps running,
both implementations share the same database, and integration events cross the process boundary through a message
broker. Cutover happens only after parity is verified (see the checklist at the end).

```
courses_py/
  domain/                     Course aggregate + state machine, CurriculumItem/Lecture/Quiz/Question, Teacher,
                              CourseRating / NumberOfStudents value objects, domain exceptions   (Course.java & co.)
  application/                Pydantic commands/queries (Javax validation replacement), command & query handlers,
                              CourseTeacherChecker, ports (repository protocols, event publisher)
  infrastructure/persistence/ SQLAlchemy imperative mapping to the Liquibase tables, repositories
  infrastructure/security/    JWT validation compatible with the Java `users` JwtTokenProvider
  infrastructure/messaging/   MessageBroker interface, in-memory + RabbitMQ implementations, topics
  integration_events/         Event schemas, inbound handlers (consumer process), outbound publisher
  api/                        FastAPI app, routers (CourseController.java / courses.yaml), error mapping
  tests/                      pytest ports of CourseTest, handler tests, CourseApiTest, event/JWT/migration tests
migrations/                   Alembic (guarded, see "Phase 1: shared database")
```

## Running

```bash
python -m venv .venv && source .venv/bin/activate
pip install -e '.[test,dev]'

pytest                                # tests (SQLite, in-memory broker, no external services)
ruff check . && ruff format --check . && mypy courses_py migrations

python -m courses_py.main             # API on http://localhost:8081  (docs: /docs)
courses-py-consumer                   # broker consumer process (only needed with COURSES_BROKER=rabbitmq)
```

Configuration is environment based:

| Variable                 | Default                                 | Spring analogue                          |
|--------------------------|-----------------------------------------|------------------------------------------|
| `COURSES_DATABASE_URL`   | `sqlite:///./courses.db`                | `spring.datasource.url`                  |
| `COURSES_JWT_SECRET_KEY` | `secret-key`                            | `security.jwt.token.secret-key`          |
| `COURSES_BROKER`         | `memory`                                | – (`memory` or `rabbitmq`)               |
| `COURSES_RABBITMQ_URL`   | `amqp://guest:guest@localhost:5672/%2F` | `spring.rabbitmq.*`                      |
| `COURSES_HOST` / `COURSES_PORT` | `0.0.0.0` / `8081`               | `server.port` (the monolith uses 8080)   |

Docker: `docker build -t courses-py . && docker run -p 8081:8081 -e COURSES_DATABASE_URL=... courses-py`
(the image also installs `psycopg` for PostgreSQL URLs, e.g. `postgresql+psycopg://user:pw@host/db`).

### REST API

Same contract as `courses/web/src/main/resources/courses.yaml` / `CourseController.java`:

| Method & path                            | Role                  | Status | Notes                                                  |
|------------------------------------------|-----------------------|--------|--------------------------------------------------------|
| `POST /courses`                          | TEACHER               | 201    | `{"uuid": ...}`; teacher resolved from the JWT subject |
| `PUT /courses/{uuid}/publish-status`     | TEACHER + ownership   | 204    | 404 unknown course, 409 not approved                   |
| `PUT /courses/{uuid}/approval-status`    | TEACHER + ownership   | 204    | send to approve, publishes `SendCourseToApproveIntegrationEvent` |
| `GET /courses`                           | any authenticated     | 200    | `ListCourseQueryHandler` (`CourseLightDTO`)            |
| `GET /courses/{uuid}`                    | any authenticated     | 200    | `CourseByUUIDQueryHandler` (`CourseDTO`), 404 unknown  |

The read endpoints have no Java controller counterpart (the query handlers are only used internally there); they
require a valid JWT of any role, matching the monolith's `anyRequest().authenticated()` rule.

Errors use the Java `GlobalExceptionHandler` shape `{"errors": ["..."]}` with the same mapping
(400 validation / unresolved related resource / invalid JWT, 403 access denied, 404 not found, 409 conflict, 500).

## Phase 1: shared database (Liquibase owns the schema)

During the coexistence phase the Python service **reads and writes the existing `course`, `curriculum_item`,
`teacher` and `question` tables**, exactly as created by Liquibase from
`courses/application/src/main/resources/db/courses.yml`. `courses_py/infrastructure/persistence/orm.py` maps the
domain classes imperatively onto those tables (same names, columns, FK names; integer `id` stays the local PK and
`uuid` stays the cross-module identity, `curriculum_item.serial_number` stays a VARCHAR as in Liquibase).

`migrations/versions/0001_courses_schema.py` reproduces that schema in Alembic for **documentation and standalone
databases only**:

* `alembic upgrade head` creates each table only if it does not already exist, so it is a no-op (apart from the
  `alembic_version` bookkeeping table) against the shared database. Liquibase remains the single source of truth in
  phase 1 — do not add schema changes here that Liquibase does not also apply.
* `alembic downgrade` refuses to drop the tables unless run with `-x standalone=true`.

Point `COURSES_DATABASE_URL` at the monolith's datasource. Note the monolith's default profile uses an in-memory
H2 database (`jdbc:h2:mem:testdb`) which cannot be shared across processes; use PostgreSQL (or another networked
database) for the coexistence phase. After cutover, Alembic takes over schema ownership for these tables.

## Integration events over a message broker

Spring's in-process `ApplicationEventPublisher` / `@EventListener` cannot cross the JVM/Python boundary, so
events flow through a RabbitMQ topic exchange (`educational-platform.integration-events`). Routing keys and
payload keys are shared between the two sides and must stay in sync:

| Routing key                                        | Payload                                 | Producer → consumer            |
|----------------------------------------------------|-----------------------------------------|--------------------------------|
| `administration.course-approved-by-admin`          | `{"courseId": uuid}`                    | Java → Python `ApproveCourseCommandHandler` |
| `course-enrollments.student-enrolled-to-course`    | `{"courseId": uuid, "username": str}`   | Java → Python `IncreaseNumberOfStudentsCommandHandler` |
| `users.user-created`                               | `{"username": str, "email": str}`       | Java → Python `CreateTeacherCommandHandler` |
| `course-reviews.course-rating-recalculated`        | `{"courseId": uuid, "rating": float}`   | Java → Python `UpdateCourseRatingCommandHandler` |
| `courses.send-course-to-approve`                   | `{"courseId": uuid}`                    | Python `SendCourseToApproveCommandHandler` → Java `administration` |

* Python: `courses_py/infrastructure/messaging/topics.py`, `courses_py/integration_events/events.py`.
  `MessageBroker` (`broker.py`) has an `InMemoryMessageBroker` (default; API process dispatches synchronously, used
  by tests) and a `RabbitMQMessageBroker` (`rabbitmq.py`, one durable queue `courses-py.<routing key>` per event,
  manual acks). The consumer process is `courses-py-consumer`.
* Delivery semantics (same as the Java side, whose in-JVM events are fire-and-forget `@Async` listeners): at-least-once.
  Outbound events are buffered on the request's SQLAlchemy session and published only after the transaction commits
  (`AfterCommitIntegrationEventPublisher`), so a rolled-back request publishes nothing. Inbound messages that are
  malformed or fail validation are rejected without requeue; messages whose handler fails for another reason (e.g.
  database outage) are requeued once and rejected on the second failure. Queues have no dead-letter exchange and
  handlers are not idempotent (a crash between commit and ack redelivers the event); add a processed-event table /
  dead-letter queue before relying on the broker for production traffic — see the cutover checklist.
* Java: sub-project `courses/event-bridge` (`courses-event-bridge`, package `com.educational.platform.courses.bridge`),
  wired into `configuration`. `OutboundIntegrationEventBridge` listens to the four in-JVM events and forwards them to
  the exchange; `InboundIntegrationEventBridge` consumes `courses.send-course-to-approve` from queue
  `java-monolith.courses.send-course-to-approve` and re-publishes it in-JVM so the unchanged `administration`
  handler creates the course proposal. Topics live in `IntegrationEventTopics.java`.

Monolith properties (`configuration/src/main/resources/application.properties`):

| Property                               | Default | Meaning                                                                   |
|----------------------------------------|---------|---------------------------------------------------------------------------|
| `courses.event-bridge.enabled`         | `false` | Enable the bridge (`@EnableRabbit`, needs `spring.rabbitmq.*` connection) |
| `courses.in-process-handlers.enabled`  | `true`  | Keep the Java `courses` `*IntegrationEventHandler`s active                |

**Do not run both consumers at once**: with the bridge enabled and the Java in-process handlers still on, every
event would be applied twice to the shared tables (e.g. `number_of_students` double increments). Sequence:

1. Bridge disabled, Java handlers on — today's behaviour, Python can be deployed read-only for comparison.
2. Bridge enabled, Java handlers on, Python consumer **not** running — verify messages arrive on the exchange.
3. Set `courses.in-process-handlers.enabled=false`, start `courses-py-consumer` — Python owns event handling;
   Java `courses` REST endpoints still work for writes not driven by events.

## Authentication & authorization

`courses_py/infrastructure/security/jwt.py` validates the tokens issued by the Java `users` module
(`users/application/.../security/JwtTokenProvider.java`): HS256, HMAC key = the **raw bytes** of
`security.jwt.token.secret-key` (the Java provider Base64-encodes the secret, but jjwt 0.9's `String` key overloads
Base64-decode it again before use),
`sub` = username, `auth` = `[{"authority": "ROLE_TEACHER"}, ...]`, `exp`. Configure the same raw secret through
`COURSES_JWT_SECRET_KEY`. The FastAPI dependency `get_principal` reads `Authorization: Bearer <token>`; an invalid
token maps to 400 `Expired or invalid JWT token` like the Java `JwtTokenFilter`, a missing token to 403 `Access Denied`.

Differences from Java worth knowing: the Java filter reloads authorities from the users table on every request,
whereas `courses-py` trusts the signed `auth` claim (it does not own the users table), so a revoked role stays valid
until the token expires (1 h by default in Java). The `secret-key` default exists only for parity with the Java
default and local development; always set `COURSES_JWT_SECRET_KEY` in any shared environment. Role and ownership rules are
enforced in the application layer (`require_role`, `CourseTeacherChecker.check_access` backed by
`CourseRepository.is_teacher`), mirroring `@PreAuthorize("hasRole('TEACHER') and @courseTeacherChecker.hasAccess(...)")`.

## Cutover checklist (do not perform until parity is verified)

Parity gates:

- [ ] `courses-py` deployed against the shared database, `GET /courses` and `GET /courses/{uuid}` return the same
      data as the Java endpoints for existing rows.
- [ ] Contract tests for `POST /courses` and `PUT /courses/{uuid}/publish-status` pass against both implementations
      (status codes, error bodies, DB rows).
- [ ] Event flow verified end to end with the bridge enabled (steps 2–3 above), no double processing.
- [ ] Idempotent consumers (event id + processed-events table in the same transaction) and a dead-letter exchange on
      the `courses-py.*` / `java-monolith.*` queues, before the broker carries production traffic.
- [ ] Ingress/gateway routes `/courses/**` to `courses-py`; Java `courses-web` receives no traffic.

Cutover (Java side):

- [ ] Remove `courses:application`, `courses:web`, `courses:integration-events` **and** `courses:event-bridge` from
      `settings.gradle.kts`, and their `implementation(project(...))` entries from `configuration/build.gradle.kts`.
- [ ] Move `SendCourseToApproveIntegrationEvent` handling in `administration` (and any other module publishing the
      four bridged events) to talk to the broker directly, replacing the bridge; the remaining modules become
      broker publishers/consumers themselves.
- [ ] Delete `courses/` and the `courses.*` properties; drop `db/courses.yml` from the Liquibase master changelog
      (`liquibase changelog-sync`-style handling for existing databases).
- [ ] Remove the `Course`/`Teacher` JPA entities' consumers in other modules, if any remain.

Cutover (Python side):

- [ ] Alembic becomes schema owner: `alembic stamp head` on the shared database, drop the guarded `create table if
      absent` pattern for subsequent revisions.
- [ ] Make `COURSES_BROKER=rabbitmq` the only supported production mode; keep `memory` for tests.
- [ ] Consider re-checking authorities against the users service instead of trusting the `auth` claim.
