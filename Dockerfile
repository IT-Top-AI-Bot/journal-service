# syntax=docker/dockerfile:1.7

############################
# Stage: build JAR
############################
FROM eclipse-temurin:25-jdk AS builder
WORKDIR /application

RUN apt-get update && apt-get install -y --no-install-recommends findutils && rm -rf /var/lib/apt/lists/*

COPY gradlew ./
COPY gradle gradle
COPY settings.gradle.kts build.gradle.kts ./
COPY src src

RUN --mount=type=cache,target=/root/.gradle \
    chmod +x gradlew && \
    ./gradlew --no-daemon clean bootJar -x test

RUN java -Djarmode=tools -jar build/libs/*.jar extract --layers --destination extracted

############################
# Stage: final runtime image
############################
FROM eclipse-temurin:25-jre-alpine
WORKDIR /application

RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

VOLUME ["/tmp"]

COPY --from=builder /application/extracted/dependencies/ ./
COPY --from=builder /application/extracted/spring-boot-loader/ ./
COPY --from=builder /application/extracted/snapshot-dependencies/ ./
COPY --from=builder /application/extracted/application/ ./

EXPOSE 8080

ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]