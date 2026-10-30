# syntax=docker/dockerfile:1

# =====================================================================
# Stage 1 - build: full JDK + Maven, compiles the executable JAR
# =====================================================================
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace

# Copy only the POM first and download dependencies. This layer is cached
# and reused on every build until pom.xml changes, so code-only changes
# don't re-download the whole internet.
COPY pom.xml .
RUN mvn -B -ntp dependency:go-offline

# Now the source code (changes often -> later layer)
COPY src ./src
# Tests already ran in the Jenkins "Test" stage, so skip them here
RUN mvn -B -ntp package -DskipTests

# =====================================================================
# Stage 2 - runtime: slim JRE only, no Maven, no JDK, no source code
# =====================================================================
FROM eclipse-temurin:21-jre-alpine

# Unprivileged system user to run the app (never run as root)
RUN addgroup -S spring && adduser -S spring -G spring

WORKDIR /app

# Only the JAR crosses over from the build stage. It stays owned by root
# (read-only for the app user), so the running process can't modify it.
COPY --from=build /workspace/target/calculator.jar app.jar

# Jenkins passes --build-arg BUILD_NUMBER=<n>; shown at /actuator/info
ARG BUILD_NUMBER=local
ENV APP_BUILD_NUMBER=${BUILD_NUMBER} \
    JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75.0"

USER spring

EXPOSE 8080

# busybox wget ships with Alpine; non-2xx (e.g. 503 when DOWN) => unhealthy
HEALTHCHECK --interval=15s --timeout=5s --start-period=40s --retries=3 \
    CMD wget -qO /dev/null http://127.0.0.1:8080/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
