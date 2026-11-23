# Agent notes

Learnings from addressing PR review comments. Keep entries short.

## Build & test
- Gradle subprojects are renamed: run module tests as `./gradlew :courses:courses-application:test :courses:courses-web:test` (pattern `:<module>:<module>-<submodule>:test`).
- Use the JDK 25 under `/usr/lib/jvm/jdk-25*` as `JAVA_HOME`.

## Conventions enforced in review
- API-first (`docs/architecture-decisions/0007-api-first.md`, CONTRIBUTING.md): every new/changed endpoint must be added to the module's OpenAPI spec (e.g. `courses/web/src/main/resources/courses.yaml`).
- OpenAPI `*.yaml` files use CRLF line endings with no trailing newline; preserve them to keep diffs minimal.
- Never build LIKE patterns as `"%" + value + "%"` without a null guard: a `null` value becomes the literal `%null%` and returns false-positive matches. Return an empty result (or reject) for `null` input.
- Use parameterized JPQL (entity properties, not physical column names) for custom repository queries.
