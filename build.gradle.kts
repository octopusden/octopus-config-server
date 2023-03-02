plugins {
    kotlin("jvm")
    id("org.octopusden.release-management")
    id("org.springframework.boot")
    id("io.spring.dependency-management") version "1.1.0"
    id("com.bmuschko.docker-spring-boot-application") version "7.1.0"
    id("maven-publish")
}

group = "org.octopusden.cloud.config-server"
version = if (project.hasProperty("buildVersion")) {
    project.properties["buildVersion"] as String
} else {
    "1.0-SNAPSHOT"
}

publishing {
    repositories {
        maven {

        }
    }
    publications {
        create<MavenPublication>("bootJar") {
            artifact(tasks.getByName("bootJar"))
        }
    }
}

springBoot {
    buildInfo()
}

docker {
    springBootApplication {
        baseImage.set("${rootProject.properties["docker.registry"]}/openjdk:11")
        ports.set(listOf(8765, 8765))
        images.set(setOf("${rootProject.properties["publishing.docker.registry"]}/${project.name}:${project.version}"))
    }
}


dependencies {
    implementation(platform("org.springframework.cloud:spring-cloud-dependencies:${project.property("spring-cloud.version")}"))
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.springframework.cloud:spring-cloud-config-server")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("io.micrometer:micrometer-registry-prometheus:1.10.4")
}
