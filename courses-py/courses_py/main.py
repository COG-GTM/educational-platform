"""``python -m courses_py.main`` — run the API with uvicorn."""

from __future__ import annotations

import os

import uvicorn


def main() -> None:
    uvicorn.run(
        "courses_py.api.app:app",
        host=os.environ.get("COURSES_HOST", "0.0.0.0"),
        port=int(os.environ.get("COURSES_PORT", "8081")),
        reload=os.environ.get("COURSES_RELOAD") == "true",
    )


if __name__ == "__main__":
    main()
