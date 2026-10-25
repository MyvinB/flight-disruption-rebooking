# syntax=docker/dockerfile:1

# --- Build: compile and package the Spring Boot jar (tests run in CI, not here) ---
FROM eclipse-temurin:17-jdk AS build
WORKDIR /app

# Dependencies first, so they are cached until pom.xml changes
COPY mvnw pom.xml ./
COPY .mvn .mvn
RUN chmod +x mvnw && ./mvnw -B -q dependency:go-offline

COPY src src
RUN ./mvnw -B -q package -DskipTests && cp target/disruption-*.jar app.jar

# --- Runtime: JRE only, non-root user ---
# Ubuntu-based images: the Alpine variants of Java 17 are not published for arm64 (Apple Silicon)
FROM eclipse-temurin:17-jre
WORKDIR /app

RUN groupadd --system app && useradd --system --gid app app
COPY --from=build /app/app.jar app.jar
USER app

EXPOSE 8080
# Respect the container's memory limit instead of the host's
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75"

HEALTHCHECK --interval=10s --timeout=3s --start-period=30s --retries=5 \
  CMD wget -qO- http://localhost:8080/actuator/health | grep -q '"status":"UP"' || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]
