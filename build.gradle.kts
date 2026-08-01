plugins {
    kotlin("jvm")
    id("org.springframework.boot")
    id("com.bmuschko.docker-spring-boot-application")
    id("maven-publish")
    id("io.github.gradle-nexus.publish-plugin")
    signing
    id("io.gitlab.arturbosch.detekt")
    id("org.jlleitschuh.gradle.ktlint")
    id("org.octopusden.octopus-quality")
}

octopusQuality {
    // Regression guard: this repository must publish NOTHING to Maven Central, so the declared
    // set is deliberately empty. Adding a publication anywhere — including to the root project —
    // fails this task rather than silently reappearing on Central at the next release.
    publication {
        enforceCentralPublications.set(true)
        centralPublications.set(emptySet())
    }
    // Repo has no tests / no coverage tool yet — disable coverage verification.
    coverage {
        enabled.set(false)
    }
    // Enforce Kotlin static analysis (detekt + ktlint); current debt is absorbed by
    // detekt-baseline.xml / ktlint-baseline.xml so the gate stays green while enforcing.
    kotlin {
        failOnViolation.set(true)
    }
}

group = "org.octopusden.cloud.config-server"

java {
    withJavadocJar()
    withSourcesJar()
}

repositories {
    mavenCentral()
}

nexusPublishing {
    repositories {
        sonatype {
            nexusUrl.set(uri("https://ossrh-staging-api.central.sonatype.com/service/local/"))
            snapshotRepositoryUrl.set(uri("https://central.sonatype.com/repository/maven-snapshots/"))
            username.set(System.getenv("MAVEN_USERNAME"))
            password.set(System.getenv("MAVEN_PASSWORD"))
        }
    }
}

// No Maven publication is declared here on purpose: this module's deliverable is the docker
// image built from `bootJar`, and no known consumer resolves it as a Maven dependency (see
// release.yml). `maven-publish` and `signing` stay applied so `publish` / `publishToSonatype`
// keep existing as no-op lifecycle tasks, and the `octopusQuality { publication { } }` guard
// above (with an empty declared set) fails if a publication reappears here.

springBoot {
    buildInfo()
}

val dockerRegistry = System.getenv().getOrDefault("DOCKER_REGISTRY", project.properties["docker.registry"]) as? String
val octopusGithubDockerRegistry = System.getenv().getOrDefault(
    "OCTOPUS_GITHUB_DOCKER_REGISTRY",
    project.properties["octopus.github.docker.registry"],
) as? String

docker {
    springBootApplication {
        baseImage.set("$dockerRegistry/eclipse-temurin:21-jdk")
        ports.set(listOf(8888, 8888))
        images.set(setOf("$octopusGithubDockerRegistry/octopusden/${project.name}:${project.version}"))
    }
}

tasks.getByName("dockerBuildImage").doFirst {
    validateDockerRegistryParams()
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
        jvmTarget = "21"
    }
}

dependencies {
    implementation(platform("org.springframework.cloud:spring-cloud-dependencies:${project.property("spring-cloud.version")}"))
    implementation(platform("org.springframework.boot:spring-boot-dependencies:${project.property("spring-boot.version")}"))
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.springframework.cloud:spring-cloud-config-server")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("io.micrometer:micrometer-registry-prometheus:1.10.4")
}

fun validateDockerRegistryParams() {
    if (dockerRegistry.isNullOrBlank() || octopusGithubDockerRegistry.isNullOrBlank()) {
        throw IllegalArgumentException(
            "Start gradle build with" +
                (if (dockerRegistry.isNullOrBlank()) " -Pdocker.registry=..." else "") +
                (if (octopusGithubDockerRegistry.isNullOrBlank()) " -Poctopus.github.docker.registry=..." else "") +
                " or set env variable(s):" +
                (if (dockerRegistry.isNullOrBlank()) " DOCKER_REGISTRY" else "") +
                (if (octopusGithubDockerRegistry.isNullOrBlank()) " OCTOPUS_GITHUB_DOCKER_REGISTRY" else ""),
        )
    }
}
