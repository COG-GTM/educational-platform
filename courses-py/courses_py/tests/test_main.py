"""``python -m courses_py.main``: ``COURSES_HOST`` / ``COURSES_PORT`` / ``COURSES_RELOAD`` -> uvicorn wiring."""

from __future__ import annotations

import pytest
import uvicorn

from courses_py import main


class _RecordedRun:
    def __init__(self) -> None:
        self.calls: list[tuple[tuple[object, ...], dict[str, object]]] = []

    def __call__(self, *args: object, **kwargs: object) -> None:
        self.calls.append((args, kwargs))


@pytest.fixture
def run(monkeypatch: pytest.MonkeyPatch) -> _RecordedRun:
    for name in ("COURSES_HOST", "COURSES_PORT", "COURSES_RELOAD"):
        monkeypatch.delenv(name, raising=False)
    recorded = _RecordedRun()
    monkeypatch.setattr(uvicorn, "run", recorded)
    return recorded


def test_main_noVariables_servesAppOnAllInterfacesPort8081WithoutReload(run: _RecordedRun) -> None:
    # when
    main.main()

    # then
    assert run.calls == [(("courses_py.api.app:app",), {"host": "0.0.0.0", "port": 8081, "reload": False})]


def test_main_hostAndPortVariables_overrideDefaults(run: _RecordedRun, monkeypatch: pytest.MonkeyPatch) -> None:
    # given
    monkeypatch.setenv("COURSES_HOST", "127.0.0.1")
    monkeypatch.setenv("COURSES_PORT", "9000")

    # when
    main.main()

    # then
    [(_, kwargs)] = run.calls
    assert (kwargs["host"], kwargs["port"]) == ("127.0.0.1", 9000)
    assert isinstance(kwargs["port"], int)


@pytest.mark.parametrize(
    ("value", "expected"),
    [("true", True), ("True", False), ("1", False), ("false", False), ("", False)],
)
def test_main_reloadVariable_enabledOnlyForLowercaseTrue(
    run: _RecordedRun, monkeypatch: pytest.MonkeyPatch, value: str, expected: bool
) -> None:
    # given
    monkeypatch.setenv("COURSES_RELOAD", value)

    # when
    main.main()

    # then
    [(_, kwargs)] = run.calls
    assert kwargs["reload"] is expected


def test_main_nonNumericPort_valueErrorBeforeServerStarts(run: _RecordedRun, monkeypatch: pytest.MonkeyPatch) -> None:
    # given
    monkeypatch.setenv("COURSES_PORT", "eight-thousand")

    # when / then
    with pytest.raises(ValueError):
        main.main()
    assert run.calls == []
