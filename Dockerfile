# syntax=docker/dockerfile:1.7

FROM eclipse-temurin:21-jdk-alpine AS repo-build

RUN apk add --no-cache bash nodejs npm

WORKDIR /workspace

COPY gradlew gradlew.bat settings.gradle build.gradle gradle.properties ./
COPY gradle ./gradle

COPY libraries/commons/build.gradle ./libraries/commons/build.gradle
COPY libraries/project-records/build.gradle ./libraries/project-records/build.gradle
COPY libraries/orchestration-clients/build.gradle ./libraries/orchestration-clients/build.gradle
COPY libraries/persistence/build.gradle ./libraries/persistence/build.gradle
COPY services/api-service/build.gradle ./services/api-service/build.gradle
COPY services/orchestration/build.gradle ./services/orchestration/build.gradle
COPY services/policy-service/build.gradle ./services/policy-service/build.gradle
COPY services/ui-service/build.gradle ./services/ui-service/build.gradle
COPY services/ui-service/frontend/package.json ./services/ui-service/frontend/package.json
COPY services/ui-service/frontend/package-lock.json ./services/ui-service/frontend/package-lock.json

RUN chmod +x gradlew

RUN --mount=type=cache,target=/root/.gradle \
    ./gradlew --no-daemon help

COPY libraries ./libraries
COPY projects ./projects
COPY services ./services
COPY shared ./shared

RUN --mount=type=cache,target=/root/.gradle \
    --mount=type=cache,target=/root/.npm \
    ./gradlew --no-daemon build \
    :services:api-service:installDist \
    :services:orchestration:installDist \
    :services:policy-service:installDist

FROM eclipse-temurin:21-jre-alpine AS api-service

WORKDIR /app

COPY --from=repo-build /workspace/services/api-service/build/install/api-service/ /app/

EXPOSE 8080

CMD ["/app/bin/api-service"]

FROM eclipse-temurin:21-jre-alpine AS orchestration

WORKDIR /app

COPY --from=repo-build /workspace/services/orchestration/build/install/orchestration/ /app/

EXPOSE 8081

CMD ["/app/bin/orchestration"]

FROM eclipse-temurin:21-jre-alpine AS policy-service

WORKDIR /app

COPY --from=repo-build /workspace/services/policy-service/build/install/policy-service/ /app/

EXPOSE 8082

CMD ["/app/bin/policy-service"]

FROM nginx:1.27-alpine AS ui-service

RUN apk add --no-cache gettext \
    && rm /etc/nginx/conf.d/default.conf \
    && mkdir -p /etc/constructraos-ui /usr/share/nginx/html-baked /var/run/constructraos-ui-config /var/run/constructraos-ui-overlay /tmp/client_temp /tmp/proxy_temp /tmp/fastcgi_temp /tmp/uwsgi_temp /tmp/scgi_temp \
    && chown -R nginx:nginx /usr/share/nginx/html /usr/share/nginx/html-baked /etc/nginx /etc/constructraos-ui /var/cache/nginx /var/run/constructraos-ui-config /var/run/constructraos-ui-overlay /tmp

COPY --from=repo-build /workspace/services/ui-service/build/frontend-static/ /usr/share/nginx/html-baked/
COPY services/ui-service/nginx/nginx.conf /etc/nginx/nginx.conf
COPY services/ui-service/nginx/default.conf /etc/nginx/conf.d/default.conf
COPY services/ui-service/nginx/local-proxy.conf.template /etc/constructraos-ui/local-proxy.conf.template
COPY services/ui-service/nginx/runtime-config.json.template /etc/constructraos-ui/runtime-config.json.template
COPY services/ui-service/nginx/entrypoint.sh /entrypoint.sh

RUN chmod +x /entrypoint.sh

USER nginx

EXPOSE 8090

ENTRYPOINT ["/entrypoint.sh"]
