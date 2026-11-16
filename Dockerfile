ARG MAVEN_IMAGE=maven:3.9.9-eclipse-temurin-21
FROM ${MAVEN_IMAGE} AS build
WORKDIR /workspace
COPY . .
ARG MODULE
RUN mvn -B -ntp -pl "${MODULE}" -am package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app
ARG MODULE
ARG APP_PORT=8080
ENV APP_PORT=${APP_PORT}
# eclipse-temurin:21-jre already includes curl for the container health check.
COPY --from=build /workspace/${MODULE}/target/*.jar /app/app.jar
EXPOSE ${APP_PORT}
HEALTHCHECK --interval=20s --timeout=3s --start-period=40s --retries=5 CMD curl -fsS "http://localhost:${APP_PORT}/actuator/health" || exit 1
USER 10001:10001
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "/app/app.jar"]
