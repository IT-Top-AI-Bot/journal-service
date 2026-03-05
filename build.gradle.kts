plugins {
    java
    id("org.sonarqube") version "7.2.2.6593"
    id("org.springframework.boot") version "4.0.3"
id("io.spring.dependency-management") version "1.1.7"
}

group = "com.aquadev"
version = "0.0.1-SNAPSHOT"
description = "journal-service"

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

val mapStructVersion by extra("1.6.3")
val springDocVersion by extra("3.0.1")
val springCloudAwsVersion by extra("4.0.0")
val springCloudVersion by extra("2025.1.1")

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-kafka")
    implementation("org.springframework.boot:spring-boot-starter-webmvc")
    implementation("org.springframework.boot:spring-boot-starter-aspectj")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-liquibase")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
    implementation("io.github.resilience4j:resilience4j-spring-boot3")
    implementation("io.awspring.cloud:spring-cloud-aws-starter-s3")
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:$springDocVersion")
    implementation("org.mapstruct:mapstruct:$mapStructVersion")
    implementation("org.springframework.cloud:spring-cloud-starter-netflix-eureka-client")
    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")
    annotationProcessor("org.mapstruct:mapstruct-processor:${property("mapStructVersion")}")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("com.h2database:h2")
    testImplementation("org.springframework.kafka:spring-kafka-test")
    runtimeOnly("org.postgresql:postgresql")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}
dependencyManagement {
    imports {
        mavenBom("org.springframework.cloud:spring-cloud-dependencies:$springCloudVersion")
        mavenBom("io.awspring.cloud:spring-cloud-aws-dependencies:$springCloudAwsVersion")
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}

tasks.register("resolveDependencies") {
    doLast {
        project.rootProject.allprojects.forEach { subProject ->
            subProject.buildscript.configurations.forEach { if (it.isCanBeResolved) it.resolve() }
            subProject.configurations.forEach { if (it.isCanBeResolved) it.resolve() }
        }
    }
}
