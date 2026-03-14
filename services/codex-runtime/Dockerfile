FROM eclipse-temurin:21-jdk-alpine AS build

ARG CODEX_CLI_VERSION=0.104.0

RUN apk add --no-cache bash nodejs npm

WORKDIR /workspace

COPY gradlew gradlew.bat settings.gradle build.gradle gradle.properties ./
COPY gradle ./gradle

RUN chmod +x gradlew

COPY . .

RUN --mount=type=cache,target=/root/.gradle \
    ./gradlew --no-daemon :services:codex-runtime:installDist

FROM eclipse-temurin:21-jre-alpine

ARG CODEX_CLI_VERSION=0.104.0

RUN apk add --no-cache nodejs npm \
    && npm install -g "@openai/codex@${CODEX_CLI_VERSION}" \
    && mkdir -p /app /workspace /codex-home /config

WORKDIR /app

COPY --from=build /workspace/services/codex-runtime/build/install/codex-runtime/ /app/

EXPOSE 8091

CMD ["/app/bin/codex-runtime"]
