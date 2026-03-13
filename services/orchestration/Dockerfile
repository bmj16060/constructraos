FROM eclipse-temurin:21-jre-alpine

WORKDIR /app
COPY build/install/orchestration/ /app/

EXPOSE 8081
CMD ["/app/bin/orchestration"]
