# syntax=docker/dockerfile:1.7

############################
# Stage: build native binary
############################
FROM ghcr.io/graalvm/native-image-community:25 AS builder
WORKDIR /application

RUN microdnf install -y findutils zip unzip git ca-certificates && microdnf clean all

COPY gradlew settings.gradle* build.gradle* gradle/ ./

RUN --mount=type=cache,target=/root/.gradle \
    chmod +x gradlew && \
    ./gradlew --no-daemon -x test help

COPY src/ src/

RUN --mount=type=cache,target=/root/.gradle \
    ./gradlew --no-daemon clean nativeCompile -x test

RUN mkdir -p /out && \
    bin="$(find build/native/nativeCompile -maxdepth 1 -type f -executable | head -n 1)" && \
    test -n "$bin" && \
    cp "$bin" /out/app && \
    chmod +x /out/app


############################
# Stage: final runtime image
############################
FROM gcr.io/distroless/base-debian12:nonroot
WORKDIR /application

VOLUME ["/tmp"]

COPY --from=builder /out/app /application/app

USER nonroot:nonroot
EXPOSE 8080

# Native binary
ENTRYPOINT ["/application/app"]
