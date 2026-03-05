# syntax=docker/dockerfile:1.7

############################
# Stage: build JAR
############################
FROM eclipse-temurin:25-jdk AS builder
WORKDIR /application

RUN apt-get update && apt-get install -y --no-install-recommends findutils && rm -rf /var/lib/apt/lists/*

COPY gradlew ./
COPY gradle/wrapper/gradle-wrapper.jar gradle/wrapper/gradle-wrapper.jar
COPY gradle/wrapper/gradle-wrapper.properties gradle/wrapper/gradle-wrapper.properties
COPY settings.gradle.kts settings.gradle.kts
COPY build.gradle.kts build.gradle.kts

RUN chmod +x gradlew && \
    ./gradlew --no-daemon resolveDependencies

COPY src/ src/

RUN ./gradlew --no-daemon clean bootJar -x test


############################
# Stage: final runtime image
############################
FROM eclipse-temurin:25-jre
WORKDIR /application

VOLUME ["/tmp"]

COPY --from=builder /application/build/libs/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/application/app.jar"]
