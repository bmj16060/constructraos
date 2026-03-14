#!/usr/bin/env python3

import json
import os
import shutil
import subprocess
import tempfile
from http import HTTPStatus
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path


PORT = int(os.environ.get("PORT", "8091"))
CODEX_COMMAND = os.environ.get("CODEX_COMMAND", "codex").strip() or "codex"
CODEX_HOME = Path(os.environ.get("CODEX_HOME", "/codex-home")).resolve()
CONFIG_SOURCE = Path(os.environ.get("CODEX_RUNTIME_CONFIG_SOURCE", "/config")).resolve()
DEFAULT_WORKDIR = Path(os.environ.get("CODEX_RUNTIME_DEFAULT_WORKDIR", "/workspace")).resolve()


def ensure_codex_home() -> tuple[bool, str]:
    CODEX_HOME.mkdir(parents=True, exist_ok=True)

    copied = False
    for filename in ("auth.json", "config.toml"):
        source = CONFIG_SOURCE / filename
        target = CODEX_HOME / filename
        if source.exists():
            shutil.copy2(source, target)
            copied = True

    auth_path = CODEX_HOME / "auth.json"
    api_key = os.environ.get("OPENAI_API_KEY", "").strip()
    if api_key and not auth_path.exists():
        try:
            subprocess.run(
                [CODEX_COMMAND, "login", "--with-api-key"],
                check=True,
                input=api_key,
                text=True,
                capture_output=True,
                env=codex_environment(),
            )
        except subprocess.CalledProcessError as exc:
            stderr = (exc.stderr or "").strip()
            return False, stderr or "Codex login with API key failed."
        return True, "configured via OPENAI_API_KEY"

    if auth_path.exists():
        return True, "configured via explicit auth.json"

    if copied:
        return False, "config.toml copied but auth.json is missing"

    return False, "missing OPENAI_API_KEY or /config/auth.json"


def codex_environment() -> dict[str, str]:
    env = os.environ.copy()
    env["CODEX_HOME"] = str(CODEX_HOME)
    return env


CONFIGURED, CONFIG_MESSAGE = ensure_codex_home()


class CodexRuntimeHandler(BaseHTTPRequestHandler):
    server_version = "constructraos-codex-runtime/1.0"

    def do_GET(self):
        if self.path != "/healthz":
            self.respond_json(HTTPStatus.NOT_FOUND, {"error": "Not found."})
            return
        status_code = HTTPStatus.OK if CONFIGURED else HTTPStatus.SERVICE_UNAVAILABLE
        self.respond_json(
            status_code,
            {
                "status": "ok" if CONFIGURED else "unconfigured",
                "configured": CONFIGURED,
                "message": CONFIG_MESSAGE,
            },
        )

    def do_POST(self):
        if self.path != "/executions":
            self.respond_json(HTTPStatus.NOT_FOUND, {"error": "Not found."})
            return

        payload = self.read_json()
        if payload is None:
            return

        if not CONFIGURED:
            self.respond_json(
                HTTPStatus.INTERNAL_SERVER_ERROR,
                {"exit_code": -1, "lines": [], "error": f"Codex runtime is not configured: {CONFIG_MESSAGE}."},
            )
            return

        prompt = (payload.get("prompt") or "").strip()
        if not prompt:
            self.respond_json(HTTPStatus.BAD_REQUEST, {"exit_code": -1, "lines": [], "error": "Prompt is required."})
            return

        output_schema = payload.get("output_schema") or ""
        if not output_schema.strip():
            self.respond_json(
                HTTPStatus.BAD_REQUEST,
                {"exit_code": -1, "lines": [], "error": "output_schema is required."},
            )
            return

        timeout_seconds = int(payload.get("timeout_seconds") or 180)
        working_directory = resolve_working_directory(payload.get("working_directory"))
        if not working_directory.exists():
            self.respond_json(
                HTTPStatus.BAD_REQUEST,
                {"exit_code": -1, "lines": [], "error": f"Working directory does not exist: {working_directory}"},
            )
            return

        if not working_directory.is_dir():
            self.respond_json(
                HTTPStatus.BAD_REQUEST,
                {"exit_code": -1, "lines": [], "error": f"Working directory is not a directory: {working_directory}"},
            )
            return

        with tempfile.NamedTemporaryFile("w", suffix=".json", delete=False) as schema_file:
            schema_file.write(output_schema)
            schema_path = Path(schema_file.name)

        try:
            command = [
                CODEX_COMMAND,
                "exec",
                "--json",
                "--skip-git-repo-check",
                "--ephemeral",
                "--cd",
                str(working_directory),
                "--output-schema",
                str(schema_path),
                prompt,
            ]

            try:
                process = subprocess.run(
                    command,
                    check=False,
                    capture_output=True,
                    text=True,
                    timeout=timeout_seconds,
                    env=codex_environment(),
                )
                combined_output = (process.stdout or "") + (process.stderr or "")
                lines = [line for line in combined_output.splitlines() if line]
                self.respond_json(
                    HTTPStatus.OK,
                    {"exit_code": process.returncode, "lines": lines, "error": ""},
                )
            except subprocess.TimeoutExpired:
                self.respond_json(
                    HTTPStatus.OK,
                    {"exit_code": -1, "lines": [], "error": "Codex execution timed out."},
                )
            except OSError as exc:
                self.respond_json(
                    HTTPStatus.INTERNAL_SERVER_ERROR,
                    {"exit_code": -1, "lines": [], "error": f"Failed to start Codex: {exc}"},
                )
        finally:
            try:
                schema_path.unlink(missing_ok=True)
            except OSError:
                pass

    def read_json(self):
        try:
            content_length = int(self.headers.get("Content-Length", "0"))
        except ValueError:
            content_length = 0
        raw = self.rfile.read(content_length)
        try:
            return json.loads(raw or b"{}")
        except json.JSONDecodeError:
            self.respond_json(HTTPStatus.BAD_REQUEST, {"error": "Invalid JSON body."})
            return None

    def respond_json(self, status_code: HTTPStatus, body):
        payload = json.dumps(body).encode("utf-8")
        self.send_response(status_code)
        self.send_header("Content-Type", "application/json")
        self.send_header("Content-Length", str(len(payload)))
        self.end_headers()
        self.wfile.write(payload)

    def log_message(self, format, *args):
        return


def resolve_working_directory(value: str | None) -> Path:
    candidate = (value or "").strip()
    if not candidate:
        return DEFAULT_WORKDIR
    return Path(candidate).expanduser().resolve()


def main():
    server = ThreadingHTTPServer(("0.0.0.0", PORT), CodexRuntimeHandler)
    try:
        server.serve_forever()
    except KeyboardInterrupt:
        pass
    finally:
        server.server_close()


if __name__ == "__main__":
    main()
