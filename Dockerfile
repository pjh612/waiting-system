FROM bellsoft/liberica-openjre-alpine:21
WORKDIR /app

COPY opentelemetry-javaagent.jar /opentelemetry-javaagent.jar
RUN chmod 755 /opentelemetry-javaagent.jar

COPY waiting-service/build/libs/*.jar app.jar

ENV OTEL_SERVICE_NAME="waiting-service"

ENTRYPOINT ["java", "-jar", "app.jar"]