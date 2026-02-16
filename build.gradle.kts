plugins {
    java
    id("org.sonarqube") version "7.2.2.6593"
    id("org.springframework.boot") version "4.0.2"
    id("org.graalvm.buildtools.native") version "0.11.4"
    id("io.spring.dependency-management") version "1.1.7"
}

group = "com.aquadev"
version = "0.0.1-SNAPSHOT"
description = "it-top-ai"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

sonar {
    properties {
        property("sonar.projectKey", "aquadev-pet-projects_it-top-ai-backend")
        property("sonar.organization", "aquadev-pet-projects")
    }
}

configurations {
    compileOnly {
        extendsFrom(configurations.annotationProcessor.get())
    }
}

repositories {
    mavenCentral()
}

extra["springDocVersion"] = "3.0.1"
extra["awsSdkVersion"] = "2.41.27"
extra["mapStructVersion"] = "1.6.3"
extra["keycloakAdminClientVersion"] = "26.0.8"
val springCloudVersion by extra("2025.1.1")

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-webmvc")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-liquibase")
    implementation("org.keycloak:keycloak-admin-client:${property("keycloakAdminClientVersion")}")
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:${property("springDocVersion")}")
    implementation("org.mapstruct:mapstruct:${property("mapStructVersion")}")
    implementation("org.springframework.cloud:spring-cloud-starter-openfeign")
    implementation(platform("software.amazon.awssdk:bom:${property("awsSdkVersion")}"))
    implementation("software.amazon.awssdk:s3")
    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")
    annotationProcessor("org.mapstruct:mapstruct-processor:${property("mapStructVersion")}")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    developmentOnly("org.springframework.boot:spring-boot-devtools")
    runtimeOnly("org.postgresql:postgresql")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

dependencyManagement {
    imports {
        mavenBom("org.springframework.cloud:spring-cloud-dependencies:$springCloudVersion")
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}
