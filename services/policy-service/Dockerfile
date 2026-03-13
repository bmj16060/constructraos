FROM eclipse-temurin:21-jre-alpine

WORKDIR /app
COPY build/install/policy-service/ /app/

EXPOSE 8082
CMD ["/app/bin/policy-service"]
