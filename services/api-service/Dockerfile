FROM eclipse-temurin:21-jre-alpine

WORKDIR /app
COPY build/install/api-service/ /app/

EXPOSE 8080
CMD ["/app/bin/api-service"]
