# Clausify API image for Render (free tier: 512 MB RAM, 0.1 CPU).
# Build from the backend folder: docker build -t clausify-api backend

# --- Build stage: compile the jar -------------------------------------------
FROM eclipse-temurin:21-jdk-noble AS build
WORKDIR /workspace

# Dependencies change far less often than code, so download them in their own cached layer.
COPY mvnw pom.xml ./
COPY .mvn .mvn
RUN ./mvnw -B -q dependency:go-offline

# Tests are skipped here because CI runs them on every pull request.
COPY src src
RUN ./mvnw -B -q -DskipTests package \
    && cp target/*.jar application.jar \
    && java -Djarmode=tools -jar application.jar extract --layers --destination extracted

# --- Runtime stage: JRE only, one Docker layer per Spring Boot layer --------
FROM eclipse-temurin:21-jre-noble
WORKDIR /application

RUN groupadd --system spring && useradd --system --gid spring --no-create-home spring

# Ordered from least to most frequently changed, so a code change only re-ships the last layer.
COPY --from=build /workspace/extracted/dependencies/ ./
COPY --from=build /workspace/extracted/spring-boot-loader/ ./
COPY --from=build /workspace/extracted/snapshot-dependencies/ ./
COPY --from=build /workspace/extracted/application/ ./

USER spring

# Sized for 512 MB: heap at 70% of the container limit leaves room for metaspace and threads,
# Serial GC has the smallest footprint on one CPU, and exiting on OOM lets Render restart cleanly.
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=70 -XX:+UseSerialGC -XX:+ExitOnOutOfMemoryError -Xss512k"

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "application.jar"]
