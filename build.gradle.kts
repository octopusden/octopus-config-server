plugins {
    kotlin("jvm")
    id("org.springframework.boot")
    id("com.bmuschko.docker-spring-boot-application")
    id("io.gitlab.arturbosch.detekt")
    id("org.jlleitschuh.gradle.ktlint")
    id("org.octopusden.octopus-quality")
}

octopusQuality {
    // Regression guard: this repository publishes nothing, so the declared set is deliberately
    // empty. Re-adding a publication anywhere fails this task instead of quietly reappearing on
    // Maven Central at the next release.
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

// Read by `springBoot { buildInfo() }` into build-info.properties, not a publishing coordinate.
group = "org.octopusden.cloud.config-server"

repositories {
    mavenCentral()
}

// Nothing is published from this repository — not to Maven Central, not to GitHub Packages; the
// deliverable is the docker image built from `bootJar`. Hence no `maven-publish`, no `signing`
// and no Sonatype plugin. The guard above still runs on every `check` whether or not
// `maven-publish` is applied, and fails the build if a publication reappears.

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
