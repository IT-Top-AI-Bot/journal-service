# syntax=docker/dockerfile:1.7

############################
# Stage: extract layers from pre-built JAR
############################
FROM eclipse-temurin:25-jdk AS extractor
WORKDIR /application

COPY build/libs/*.jar application.jar

RUN java -Djarmode=tools -jar application.jar extract --layers --destination extracted

############################
# Stage: final runtime image
############################
FROM eclipse-temurin:25-jre-alpine
WORKDIR /application

RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

VOLUME ["/tmp"]

COPY --from=extractor /application/extracted/lib/ lib/
COPY --from=extractor /application/extracted/snapshot-lib/ snapshot-lib/
COPY --from=extractor /application/extracted/app.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
