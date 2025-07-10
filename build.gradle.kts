plugins {
    kotlin("jvm")
    id("org.springframework.boot")
    id("com.bmuschko.docker-spring-boot-application")
    id("maven-publish")
    id("io.github.gradle-nexus.publish-plugin")
    signing
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

publishing {
    repositories {
        maven {

        }
    }
    publications {
        create<MavenPublication>("bootJar") {
            from(components["java"])
            artifact(tasks.getByName("bootJar"))
            pom {
                name.set(project.name)
                description.set("Octopus module: ${project.name}")
                url.set("https://github.com/octopusden/octopus-config-server.git")
                licenses {
                    license {
                        name.set("The Apache License, Version 2.0")
                        url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                    }
                }
                scm {
                    url.set("https://github.com/octopusden/octopus-config-server.git")
                    connection.set("scm:git://github.com/octopusden/octopus-config-server.git")
                }
                developers {
                    developer {
                        id.set("octopus")
                        name.set("octopus")
                    }
                }
            }
        }
    }
}


signing {
    val signingKey: String? by project
    val signingPassword: String? by project
    useInMemoryPgpKeys(signingKey, signingPassword)
    sign(publishing.publications["bootJar"])
}

springBoot {
    buildInfo()
}

val dockerRegistry = System.getenv().getOrDefault("DOCKER_REGISTRY", project.properties["docker.registry"]) as? String
val octopusGithubDockerRegistry = System.getenv().getOrDefault("OCTOPUS_GITHUB_DOCKER_REGISTRY", project.properties["octopus.github.docker.registry"]) as? String

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
                    (if (octopusGithubDockerRegistry.isNullOrBlank()) " OCTOPUS_GITHUB_DOCKER_REGISTRY" else "")
        )
    }
}
