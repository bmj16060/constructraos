FROM node:22-alpine

ARG CODEX_CLI_VERSION=0.104.0

RUN apk add --no-cache python3 \
    && npm install -g "@openai/codex@${CODEX_CLI_VERSION}" \
    && mkdir -p /app /workspace /codex-home /config

WORKDIR /app

COPY infra/codex-runtime/server.py /app/server.py

ENV PORT=8091
ENV CODEX_COMMAND=codex
ENV CODEX_HOME=/codex-home
ENV CODEX_RUNTIME_DEFAULT_WORKDIR=/workspace
ENV CODEX_RUNTIME_CONFIG_SOURCE=/config

EXPOSE 8091

CMD ["python3", "/app/server.py"]
